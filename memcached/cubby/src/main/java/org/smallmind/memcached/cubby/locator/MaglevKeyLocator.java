/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.memcached.cubby.locator;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import io.whitfin.siphash.SipHash;
import io.whitfin.siphash.SipHashContext;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.HostControl;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

/**
 * A {@link KeyLocator} that implements the Maglev consistent-hashing algorithm to distribute
 * cache keys across memcached hosts with minimal key remapping when the host set changes.
 *
 * <p>Maglev works by pre-computing a per-host permutation table that defines the preference
 * order each host has over the lookup table slots. During routing table generation the
 * algorithm fills a fixed-size lookup table by round-robining through each host's permutation
 * until every slot is assigned, producing a table in which each host owns an approximately
 * equal number of entries. Because the permutations are derived solely from host names and the
 * prime-sized table, the assignment is stable: adding or removing one host changes only those
 * slots that were assigned to that host.</p>
 *
 * <p>The lookup table size is the first prime number greater than
 * {@code serverPool.size() * virtualHostCount}. Larger values of {@code virtualHostCount}
 * produce a larger table and a more balanced distribution at the cost of more memory and a
 * longer {@link #installRouting(ServerPool)} call.</p>
 *
 * <p>Per-request key hashing is performed with SipHash-1-3 for speed; the permutation tables
 * themselves use SHA-256 and SHA3-256 for their offset and skip values respectively.</p>
 *
 * <p>All access to the routing map is protected by a {@link ReentrantReadWriteLock} so that
 * concurrent {@link #find(ServerPool, String)} calls are safe alongside periodic
 * {@link #updateRouting(ServerPool)} calls.</p>
 */
public class MaglevKeyLocator implements KeyLocator {

  private final SipHashContext SIPHASH_CONTEXT = SipHash.context("0123456789ABCDEF".getBytes(StandardCharsets.UTF_8));
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final HashMap<String, int[]> permutationMap = new HashMap<>();
  private final int virtualHostCount;
  private HashMap<Integer, String> routingMap;
  private LinkedList<MemcachedHost> currentHostList;
  private int permutationSize;
  private long longerPermutationSize;

  /**
   * Creates a locator with a default virtual host count of 100 entries per real host.
   */
  public MaglevKeyLocator () {

    this(100);
  }

  /**
   * Creates a locator with the specified number of virtual entries per real host.
   *
   * <p>Higher values improve the evenness of key distribution but increase memory usage and
   * the time required by {@link #installRouting(ServerPool)}.</p>
   *
   * @param virtualHostCount the number of virtual slots to allocate per real host when sizing
   *                         the Maglev lookup table; must be a positive integer
   */
  public MaglevKeyLocator (int virtualHostCount) {

    this.virtualHostCount = virtualHostCount;
  }

  /**
   * Executes the Maglev fill algorithm to produce a complete lookup table mapping slot indices
   * to host names.
   *
   * <p>Only active hosts participate. The method sorts the active host names to ensure that the
   * table assignment is deterministic regardless of the order in which hosts were added to the
   * pool. The cached {@code currentHostList} is updated as a side-effect so that
   * {@link #updateRouting(ServerPool)} can detect future changes.</p>
   *
   * @param serverPool the pool from which currently active hosts are drawn
   * @return a map from slot index ({@code 0..permutationSize-1}) to the name of the host
   * assigned to that slot; empty if no host is active
   */
  private HashMap<Integer, String> generateRoutingMap (ServerPool serverPool) {

    HashMap<Integer, String> routingMap = new HashMap<>();
    LinkedList<String> activeNameList = new LinkedList<>();

    currentHostList = new LinkedList<>();
    for (HostControl hostControl : serverPool.values()) {
      if (hostControl.isActive()) {
        currentHostList.add(hostControl.getMemcachedHost());
        activeNameList.add(hostControl.getMemcachedHost().getName());
      }
    }

    if (!activeNameList.isEmpty()) {

      String[] activeNames;
      int[] next = new int[activeNameList.size()];
      int[] entry = new int[permutationSize];
      int n = 0;

      Collections.sort(activeNameList);
      activeNames = activeNameList.toArray(new String[0]);
      Arrays.fill(entry, -1);

      while (n != permutationSize) {
        for (int i = 0; i < next.length; i++) {

          int c = permutationMap.get(activeNames[i])[next[i]];

          while (entry[c] >= 0) {
            next[i] = next[i] + 1;
            c = permutationMap.get(activeNames[i])[next[i]];
          }

          entry[c] = i;
          next[i] = next[i] + 1;
          n = n + 1;

          if (n == permutationSize) {
            break;
          }
        }
      }

      for (int i = 0; i < entry.length; i++) {
        routingMap.put(i, activeNames[entry[i]]);
      }
    }

    return routingMap;
  }

  /**
   * Performs one-time initialisation of the routing data structures for the given server pool.
   *
   * <p>This method is called once by the client after all hosts have been registered in the
   * pool. Implementations may pre-compute expensive structures (e.g. hash permutation tables)
   * that need only be built once per pool configuration.</p>
   *
   * <p>Computes the prime-sized lookup table dimension, then derives a per-host permutation
   * array for every host in the pool (including inactive ones, so that they are available
   * immediately if they become active later without requiring a full re-install). The offset
   * for each host's permutation is computed from a SHA-256 hash of the host name; the skip is
   * computed from a SHA3-256 hash, ensuring the two values are independent. After all
   * permutations are built, {@link #updateRouting(ServerPool)} is called to populate the
   * initial routing map from the currently active hosts.</p>
   *
   * @param serverPool the fully populated pool; its size determines the lookup table dimension
   * @throws CubbyOperationException if SHA-256 or SHA3-256 is not available in the current
   *                                 JVM ({@link NoSuchAlgorithmException} is wrapped)
   */
  @Override
  public void installRouting (ServerPool serverPool)
    throws CubbyOperationException {

    permutationSize = PrimeGenerator.nextPrime(serverPool.size() * virtualHostCount);
    longerPermutationSize = permutationSize;

    for (String name : serverPool.keySet()) {
      try {

        int[] permutations;
        int offset = new BigInteger(EncryptionUtility.hash(HashAlgorithm.SHA_256, name.getBytes(StandardCharsets.UTF_8))).mod(BigInteger.valueOf(longerPermutationSize)).intValue();
        int skip = new BigInteger(EncryptionUtility.hash(HashAlgorithm.SHA3_256, name.getBytes(StandardCharsets.UTF_8))).mod(BigInteger.valueOf(longerPermutationSize - 1)).intValue() + 1;

        permutationMap.put(name, permutations = new int[permutationSize]);
        for (int index = 0; index < permutationSize; index++) {
          permutations[index] = (offset + (index * skip)) % permutationSize;
        }
      } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        throw new CubbyOperationException(noSuchAlgorithmException);
      }
    }

    updateRouting(serverPool);
  }

  /**
   * Refreshes the routing structures to reflect the current availability of hosts in the pool.
   *
   * <p>This method is called whenever a host transitions between active and inactive states.
   * Implementations should check whether the active host set has actually changed before
   * rebuilding their routing tables to avoid unnecessary work.</p>
   *
   * <p>Acquires the write lock and rebuilds the routing map only when the active host set has
   * changed since the last build. Change detection compares the pool's current active hosts
   * against the cached {@code currentHostList}.</p>
   *
   * @param serverPool the pool whose current active host set should be reflected in the
   *                   Maglev lookup table
   */
  @Override
  public void updateRouting (ServerPool serverPool) {

    lock.writeLock().lock();
    try {
      if ((currentHostList == null) || (!serverPool.representsHosts(currentHostList))) {
        routingMap = generateRoutingMap(serverPool);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Resolves the {@link MemcachedHost} that should handle the request for the given key.
   *
   * <p>This method is called on every cache operation and must be efficient. Implementations
   * should use a read lock or equivalent mechanism to allow concurrent lookups.</p>
   *
   * <p>Acquires the read lock and hashes the key using SipHash-1-3 to obtain a slot index
   * into the Maglev lookup table, then resolves the winning host name via the server pool.
   * SipHash is chosen for its speed and distribution quality; cryptographic strength is not
   * required for routing decisions.</p>
   *
   * @param serverPool the pool used to resolve the host name stored in the routing map to a
   *                   {@link MemcachedHost} instance
   * @param key        the cache key to route
   * @return the active {@link MemcachedHost} that should service this key
   * @throws IOException if the routing map is empty or {@code null}, indicating that no
   *                     active host is available ({@link NoAvailableHostException})
   */
  @Override
  public MemcachedHost find (ServerPool serverPool, String key)
    throws IOException {

    lock.readLock().lock();
    try {
      if ((routingMap == null) || routingMap.isEmpty()) {
        throw new NoAvailableHostException();
      } else {

        // Any hash will do, it just needs to be fast and well distributed, and does not need to be cryptographically safe
        return serverPool.get(routingMap.get((int)(Math.abs(SIPHASH_CONTEXT.hash(key.getBytes(StandardCharsets.UTF_8), 1, 3)) % longerPermutationSize))).getMemcachedHost();
      }
    } finally {
      lock.readLock().unlock();
    }
  }
}
