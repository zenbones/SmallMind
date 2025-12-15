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
package org.smallmind.nutsnbolts.net;

import java.net.InetAddress;
import java.util.Comparator;

/**
 * Compares {@link InetAddress} instances lexicographically by their raw octets, normalizing
 * differing address lengths (IPv4 vs IPv6) by left-padding with zeros.
 */
public class InetAddressComparator implements Comparator<InetAddress> {

  /**
   * Compares two addresses by iterating through their octets (most significant first), padding
   * the shorter address with leading zeros so IPv4 and IPv6 can be compared consistently.
   *
   * @param a first address
   * @param b second address
   * @return negative if {@code a} is less than {@code b}, positive if greater, zero if equal
   */
  @Override
  public int compare (InetAddress a, InetAddress b) {

    byte[] aOctets = a.getAddress();
    byte[] bOctets = b.getAddress();
    int len = Math.max(aOctets.length, bOctets.length);

    for (int i = 0; i < len; i++) {
      byte aOctet = (i >= len - aOctets.length) ? aOctets[i - (len - aOctets.length)] : 0;
      byte bOctet = (i >= len - bOctets.length) ? bOctets[i - (len - bOctets.length)] : 0;

      if (aOctet != bOctet) return (0xff & aOctet) - (0xff & bOctet);
    }

    return 0;
  }
}
