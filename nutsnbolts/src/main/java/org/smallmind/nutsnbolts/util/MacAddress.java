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
package org.smallmind.nutsnbolts.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Utility for obtaining the MAC address of the network interface associated with the local host address.
 */
public class MacAddress {

  /**
   * Resolves the MAC address for the non-loopback local host interface.
   *
   * @return byte array of the interface hardware address
   * @throws SocketException      if network interfaces cannot be queried
   * @throws UnknownHostException if the local host address cannot be resolved
   * @throws MacAddressException  if only loopback is available or no matching interface is found
   */
  public static byte[] getBytes ()
    throws SocketException, UnknownHostException, MacAddressException {

    InetAddress localHostAddress;

    if ((localHostAddress = InetAddress.getLocalHost()).equals(InetAddress.getLoopbackAddress())) {
      throw new MacAddressException("Local host address must != loopback address(%s)", localHostAddress.getHostAddress());
    }

    for (NetworkInterface network : new EnumerationIterator<>(NetworkInterface.getNetworkInterfaces())) {

      byte[] mac;

      if (((mac = network.getHardwareAddress()) != null) && (mac.length > 0)) {
        for (InetAddress address : new EnumerationIterator<>(network.getInetAddresses())) {
          if (address.equals(localHostAddress)) {

            return mac;
          }
        }
      }
    }

    throw new MacAddressException("Found no mac address for the local host address(%s)", localHostAddress.getHostAddress());
  }
}
