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
package org.smallmind.bayeux.oumuamua.server.impl.websocket;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaConnection;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaServer;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356.WebSocketTransport;
import org.smallmind.scribe.pen.LoggerManager;

public class WebSocketEndpoint<V extends Value<V>> extends Endpoint implements MessageHandler.Whole<String>, OumuamuaConnection<V> {

  private javax.websocket.Session websocketSession;
  private OumuamuaServer<V> server;
  private WebSocketTransport<V> websocketTransport;

  @Override
  public void onOpen (javax.websocket.Session websocketSession, EndpointConfig config) {

    this.websocketSession = websocketSession;

    server = (OumuamuaServer<V>)config.getUserProperties().get(Server.ATTRIBUTE);
    websocketTransport = (WebSocketTransport<V>)config.getUserProperties().get(WebSocketTransport.ATTRIBUTE);

    if (websocketTransport.getMaxIdleTimeoutMilliseconds() >= 0) {
      websocketSession.setMaxIdleTimeout(websocketTransport.getMaxIdleTimeoutMilliseconds());
    }
    if (websocketTransport.getMaximumTextMessageBufferSize() > 0) {
      websocketSession.getContainer().setDefaultMaxTextMessageBufferSize(websocketTransport.getMaximumTextMessageBufferSize());
    }

    websocketSession.addMessageHandler(this);
  }

  @Override
  public Transport<V> getTransport () {

    return websocketTransport;
  }

  public synchronized void deliver (Packet<V> packet) {

    if (websocketSession.isOpen()) {
      try {

        String encodedPacket = PacketUtility.encode(packet);

        LoggerManager.getLogger(WebSocketEndpoint.class).debug(() -> "=>" + encodedPacket);

        if (websocketTransport.getAsyncSendTimeoutMilliseconds() > 0) {
          websocketSession.getAsyncRemote().sendText(encodedPacket).get(websocketTransport.getAsyncSendTimeoutMilliseconds(), TimeUnit.MILLISECONDS);
        } else {
          websocketSession.getBasicRemote().sendText(encodedPacket);
        }
      } catch (IOException | InterruptedException | TimeoutException | ExecutionException exception) {
        LoggerManager.getLogger(WebSocketEndpoint.class).error(exception);
      }
    }
  }

  @Override
  public void onMessage (String content) {

    server.getExecutorService().submit(() -> {

      LoggerManager.getLogger(WebSocketEndpoint.class).debug(() -> "<=" + content);

      try {
        process(server, this::deliver, server.getCodec().from(content));
      } catch (IOException ioException) {
        LoggerManager.getLogger(WebSocketEndpoint.class).error(ioException);
      }
    });
  }

  @Override
  public synchronized void onError (Session wsSession, Throwable failure) {

    LoggerManager.getLogger(WebSocketEndpoint.class).error(failure);
  }
}
