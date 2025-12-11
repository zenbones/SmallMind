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
package org.smallmind.bayeux.oumuamua.server.impl.longpolling;

import jakarta.servlet.ServletConfig;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;

/**
 * Long-polling transport implementation for servlet environments.
 *
 * @param <V> value representation
 */
public class LongPollingTransport<V extends Value<V>> extends AbstractAttributed implements Transport<V> {

  private final ServletProtocol<V> servletProtocol;

  /**
   * Creates a long-polling transport bound to its protocol.
   *
   * @param servletProtocol owning protocol
   */
  public LongPollingTransport (ServletProtocol<V> servletProtocol) {

    this.servletProtocol = servletProtocol;
  }

  /**
   * @return owning protocol
   */
  @Override
  public Protocol<V> getProtocol () {

    return servletProtocol;
  }

  /**
   * @return transport name
   */
  @Override
  public String getName () {

    return Transports.LONG_POLLING.getName();
  }

  /**
   * Long-poll transport is remote (not local).
   *
   * @return {@code false}
   */
  @Override
  public boolean isLocal () {

    return Transports.LONG_POLLING.isLocal();
  }

  /**
   * Initializes the transport in the servlet context.
   *
   * @param server        owning server
   * @param servletConfig servlet configuration
   */
  @Override
  public void init (Server<?> server, ServletConfig servletConfig) {

  }
}
