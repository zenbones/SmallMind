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
package org.smallmind.bayeux.oumuamua.server.impl.websocket;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaConnection;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaServer;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356.WebSocketTransport;
import org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356.WebsocketProtocol;
import org.smallmind.scribe.pen.LoggerManager;

public class WebSocketEndpoint<V extends Value<V>> extends Endpoint implements MessageHandler.Whole<String>, OumuamuaConnection<V> {

  private jakarta.websocket.Session websocketSession;
  private OumuamuaServer<V> server;
  private WebSocketTransport<V> websocketTransport;

  @Override
  public void onOpen (jakarta.websocket.Session websocketSession, EndpointConfig config) {

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
  public String getId () {

    return websocketSession.getId();
  }

  @Override
  public Transport<V> getTransport () {

    return websocketTransport;
  }

  public synchronized void deliver (Packet<V> packet) {

    if (websocketSession.isOpen()) {
      try {

        String encodedPacket = PacketUtility.encode(packet);

        LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), () -> "=>" + encodedPacket);

        if (websocketTransport.getAsyncSendTimeoutMilliseconds() > 0) {
          websocketSession.getAsyncRemote().sendText(encodedPacket).get(websocketTransport.getAsyncSendTimeoutMilliseconds(), TimeUnit.MILLISECONDS);
        } else {
          websocketSession.getBasicRemote().sendText(encodedPacket);
        }

        ((WebsocketProtocol<V>)websocketTransport.getProtocol()).onDelivery(packet);
      } catch (IOException | InterruptedException | TimeoutException | ExecutionException exception) {
        LoggerManager.getLogger(WebSocketEndpoint.class).error(exception);
      }
    }
  }

  @Override
  public void onMessage (String content) {

    server.getExecutorService().submit(() -> {

      LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), () -> "<=" + content);

      try {

        Message<V>[] messages = server.getCodec().from(content);

        ((WebsocketProtocol<V>)websocketTransport.getProtocol()).onReceipt(messages);

        process(server, (session, packet) -> {
          if (session == null) {
            deliver(packet);
          } else {
            // The cometd clients ignore the specification when using the reload extension, and just steals the session without a new handshake.
            session.dispatch(packet);

            if (SessionState.DISCONNECTED.equals(session.getState())) {
              onCleanup();
            }
          }
        }, messages);
      } catch (IOException ioException) {
        LoggerManager.getLogger(WebSocketEndpoint.class).error(ioException);
      }
    });
  }

  @Override
  public synchronized void onError (Session wsSession, Throwable failure) {

    LoggerManager.getLogger(WebSocketEndpoint.class).error(failure);
  }

  @Override
  public synchronized void onCleanup () {

    if (websocketSession.isOpen()) {
      try {
        websocketSession.close();
      } catch (IOException ioException) {
        LoggerManager.getLogger(WebSocketEndpoint.class).error(ioException);
      }
    }
  }
}
