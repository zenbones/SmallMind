/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;

public class ServletProtocol<V extends Value<V>> extends AbstractProtocol<V> {

  private final LongPollingTransport<V> longPollingTransport;
  private final long longPollTimeoutMilliseconds;

  public ServletProtocol (long longPollTimeoutMilliseconds) {

    this(longPollTimeoutMilliseconds, null);
  }

  public ServletProtocol (long longPollTimeoutMilliseconds, ProtocolListener<V>[] listeners) {

    this.longPollTimeoutMilliseconds = longPollTimeoutMilliseconds;

    longPollingTransport = new LongPollingTransport<>(this);

    if (listeners != null) {
      for (ProtocolListener<V> listener : listeners) {
        addListener(listener);
      }
    }
  }

  @Override
  public String getName () {

    return Protocols.SERVLET.getName();
  }

  @Override
  public boolean isLongPolling () {

    return true;
  }

  @Override
  public long getLongPollTimeoutMilliseconds () {

    return longPollTimeoutMilliseconds;
  }

  @Override
  public String[] getTransportNames () {

    return new String[] {Transports.LONG_POLLING.getName()};
  }

  @Override
  public Transport<V> getTransport (String name) {

    return Transports.LONG_POLLING.getName().equals(name) ? longPollingTransport : null;
  }
}
