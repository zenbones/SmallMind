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
package org.smallmind.bayeux.oumuamua.server.impl.longpolling;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncContext;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaServer;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaSession;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.scribe.pen.LoggerManager;

public class LongPollingConnection<V extends Value<V>> implements Connection<V> {

  private final LongPollingTransport<V> longPollingTransport;
  private final OumuamuaServer<V> server;
  private final AtomicReference<OumuamuaSession<V>> sessionRef = new AtomicReference<>();
  private final AtomicLong lastContact;
  private final long maxIdleTimeoutMilliseconds;

  public LongPollingConnection (LongPollingTransport<V> longPollingTransport, OumuamuaServer<V> server, long maxIdleTimeoutMilliseconds) {

    this.longPollingTransport = longPollingTransport;
    this.server = server;
    this.maxIdleTimeoutMilliseconds = maxIdleTimeoutMilliseconds;

    lastContact = new AtomicLong(System.currentTimeMillis());
  }

  @Override
  public Transport<V> getTransport () {

    return longPollingTransport;
  }

  public void setSession (OumuamuaSession<V> session) {

    sessionRef.set(session);
  }

  @Override
  public void maintenance () {

    if ((System.currentTimeMillis() - lastContact.get()) > maxIdleTimeoutMilliseconds) {
      close();
    }
  }

  @Override
  public void deliver (Packet<V> packet) {

    throw new UnsupportedOperationException();
  }

  private void emit (AsyncContext asyncContext, Packet<V> packet)
    throws IOException {

    String encodedPacket = PacketUtility.encode(packet).toString();

    LoggerManager.getLogger(LongPollingConnection.class).debug(() -> "=>" + encodedPacket);

    asyncContext.getResponse().getOutputStream().print(encodedPacket);
    asyncContext.getResponse().flushBuffer();
    asyncContext.complete();
  }

  public void onMessages (AsyncContext asyncContext, Message<V>[] messages, byte[] contentBuffer) {

    OumuamuaSession<V> session;

    LoggerManager.getLogger(LongPollingConnection.class).debug(() -> "<=" + new String(contentBuffer));

    if ((session = sessionRef.get()) != null) {
      lastContact.set(System.currentTimeMillis());

      for (Message<V> message : messages) {
        try {
          emit(asyncContext, respond(getTransport().getProtocol(), server, session, message));
        } catch (IOException ioException) {
          LoggerManager.getLogger(LongPollingConnection.class).error(ioException);
        }
      }

      if (SessionState.DISCONNECTED.equals(session.getState())) {
        close();
      }
    }
  }

  private synchronized void close () {

    OumuamuaSession<V> session;

    if ((session = sessionRef.get()) != null) {
      if (!SessionState.DISCONNECTED.equals(session.getState())) {
        session.completeDisconnect();
      }

      server.removeSession(session);
      sessionRef.set(null);
    }
  }
}
