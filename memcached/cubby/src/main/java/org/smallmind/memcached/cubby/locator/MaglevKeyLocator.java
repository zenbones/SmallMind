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
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.cubby.NoAvailableHostException;
import org.smallmind.memcached.cubby.ServerPool;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public class MaglevKeyLocator implements KeyLocator {

  private final ServerPool serverPool;
  private final HashMap<String, int[]> permutationMap = new HashMap<>();
  private final int permutationSize;
  private final long longerPermutationSize;
  private HashMap<Integer, String> routingMap = new HashMap<>();

  public MaglevKeyLocator (ServerPool serverPool)
    throws NoSuchAlgorithmException {

    this(serverPool, 100);
  }

  public MaglevKeyLocator (ServerPool serverPool, int virtualHostCount)
    throws NoSuchAlgorithmException {

    this.serverPool = serverPool;

    permutationSize = PrimeGenerator.nextPrime(serverPool.size() * virtualHostCount);
    longerPermutationSize = permutationSize;

    for (String name : serverPool.keySet()) {

      int[] permutations;
      int offset = new BigInteger(EncryptionUtility.hash(HashAlgorithm.SHA_256, name.getBytes())).mod(BigInteger.valueOf(longerPermutationSize)).intValue();
      int skip = new BigInteger(EncryptionUtility.hash(HashAlgorithm.SHA3_256, name.getBytes())).mod(BigInteger.valueOf(longerPermutationSize - 1)).intValue() + 1;

      permutationMap.put(name, permutations = new int[permutationSize]);
      for (int index = 0; index < permutationSize; index++) {
        permutations[index] = (offset + (index * skip)) % permutationSize;
      }
    }

    routingMap = generateRoutingMap(serverPool);
  }

  private HashMap<Integer, String> generateRoutingMap (ServerPool serverPool) {

    HashMap<Integer, String> routingMap = new HashMap<>();
    LinkedList<String> activeNameList = new LinkedList<>();

    for (MemcachedHost memcachedHost : serverPool.values()) {
      if (memcachedHost.isActive()) {
        activeNameList.add(memcachedHost.getName());
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
  public MemcachedHost find (String key)
    throws IOException, NoSuchAlgorithmException {

    if (routingMap.isEmpty()) {
      throw new NoAvailableHostException();
    } else {

      return serverPool.get(routingMap.get(new BigInteger(EncryptionUtility.hash(HashAlgorithm.MD5, key.getBytes())).mod(BigInteger.valueOf(longerPermutationSize)).intValue()));
    }
  }
}
