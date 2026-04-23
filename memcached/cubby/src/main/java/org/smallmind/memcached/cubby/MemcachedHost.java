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
package org.smallmind.memcached.cubby;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Describes a single memcached node within the cluster, capturing its logical name and network
 * coordinates.
 *
 * <p>The {@code name} field is used by the {@link org.smallmind.memcached.cubby.locator.KeyLocator}
 * as a stable identifier for consistent-hashing ring placement. The hostname and port are
 * combined into an {@link InetSocketAddress} at construction time and may be refreshed by
 * {@link #regenerate(InetSocketAddress)} after a successful reconnect probe, accommodating
 * downstream load-balancer address changes.</p>
 */
public class MemcachedHost {

  private final String name;
  private final String hostName;
  private final int port;
  private SocketAddress address;

  /**
   * Constructs a host descriptor and immediately resolves the initial socket address.
   *
   * @param name     a unique logical name used as the key in routing tables
   * @param hostName the DNS hostname or IP address of the memcached server
   * @param port     the TCP port on which the memcached server listens
   */
  public MemcachedHost (String name, String hostName, int port) {

    this.name = name;
    this.hostName = hostName;
    this.port = port;

    address = new InetSocketAddress(hostName, port);
  }

  /**
   * Creates a fresh {@link InetSocketAddress} from the stored hostname and port, triggering a
   * new DNS resolution. Used by the {@link ServerDefibrillator} when probing whether a
   * previously offline host has become reachable again.
   *
   * @return a newly constructed socket address for this host
   */
  protected InetSocketAddress constructAddress () {

    return new InetSocketAddress(hostName, port);
  }

  /**
   * Replaces the stored socket address with one obtained from a successful probe. This allows
   * the host to track an updated address when the underlying load balancer has changed the
   * endpoint behind the hostname.
   *
   * @param address the new socket address to record
   * @return this instance for convenient use in call chains
   */
  protected MemcachedHost regenerate (InetSocketAddress address) {

    this.address = address;

    return this;
  }

  /**
   * Returns the logical name assigned to this host, used as a stable identifier in routing tables.
   *
   * @return the host name, never {@code null}
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the current socket address of this host. The address is set at construction time
   * and may be updated by {@link #regenerate(InetSocketAddress)} after a reconnect probe.
   *
   * @return the current {@link SocketAddress}, never {@code null}
   */
  public SocketAddress getAddress () {

    return address;
  }
}
