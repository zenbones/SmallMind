/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import io.whitfin.siphash.SipHasher;
import io.whitfin.siphash.SipHasherContainer;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.HostControl;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public class MaglevKeyLocator implements KeyLocator {

  private static final SipHasherContainer SIPHASH = SipHasher.container("0123456789ABCDEF".getBytes());
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final HashMap<String, int[]> permutationMap = new HashMap<>();
  private final int virtualHostCount;
  private HashMap<Integer, String> routingMap;
  private LinkedList<MemcachedHost> currentHostList;
  private int permutationSize;
  private long longerPermutationSize;

  public MaglevKeyLocator () {

    this(100);
  }

  public MaglevKeyLocator (int virtualHostCount) {

    this.virtualHostCount = virtualHostCount;
  }

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

  @Override
  public void installRouting (ServerPool serverPool)
    throws CubbyOperationException {

    permutationSize = PrimeGenerator.nextPrime(serverPool.size() * virtualHostCount);
    longerPermutationSize = permutationSize;

    for (String name : serverPool.keySet()) {
      try {

        int[] permutations;
        int offset = new BigInteger(EncryptionUtility.hash(HashAlgorithm.SHA_256, name.getBytes())).mod(BigInteger.valueOf(longerPermutationSize)).intValue();
        int skip = new BigInteger(EncryptionUtility.hash(HashAlgorithm.SHA3_256, name.getBytes())).mod(BigInteger.valueOf(longerPermutationSize - 1)).intValue() + 1;

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

  @Override
  public MemcachedHost find (ServerPool serverPool, String key)
    throws IOException {

    lock.readLock().lock();
    try {
      if ((routingMap == null) || routingMap.isEmpty()) {
        throw new NoAvailableHostException();
      } else {

        // Any hash will do, it just needs to be fast and well distributed, and does not need to be cryptographically safe
        return serverPool.get(routingMap.get((int)(Math.abs(SIPHASH.hash(key.getBytes())) % longerPermutationSize))).getMemcachedHost();
      }
    } finally {
      lock.readLock().unlock();
    }
  }
}
