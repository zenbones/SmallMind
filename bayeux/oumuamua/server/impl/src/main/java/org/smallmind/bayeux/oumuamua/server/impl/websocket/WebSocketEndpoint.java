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
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Route;
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

/**
 * JSR-356 {@link Endpoint} that bridges an incoming WebSocket connection into an Oumuamua
 * session, dispatching inbound text frames through the server and flushing outbound packets
 * back over the same socket.
 *
 * @param <V> the concrete {@link Value} type used by the server's JSON codec
 */
public class WebSocketEndpoint<V extends Value<V>> extends Endpoint implements MessageHandler.Whole<String>, OumuamuaConnection<V> {

  private javax.websocket.Session websocketSession;
  private OumuamuaServer<V> server;
  private WebSocketTransport<V> websocketTransport;
  private boolean discardReported = false;

  /**
   * Completes endpoint setup when the WebSocket handshake succeeds: captures the server and
   * transport from user properties, applies idle-timeout and buffer-size settings, and
   * registers this instance as the whole-message handler.
   *
   * @param websocketSession the newly established WebSocket session
   * @param config           endpoint configuration carrying server and transport attributes
   */
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

  /**
   * Returns the connection identifier sourced from the underlying WebSocket session.
   *
   * @return the WebSocket session identifier
   */
  @Override
  public String getId () {

    return websocketSession.getId();
  }

  /**
   * Returns the {@link WebSocketTransport} that accepted this connection.
   *
   * @return the owning WebSocket transport
   */
  @Override
  public Transport<V> getTransport () {

    return websocketTransport;
  }

  /**
   * Encodes and sends a packet over the WebSocket session, notifying the protocol on success.
   *
   * <p>Sends are serialized on this endpoint because JSR-356 forbids concurrent writes to one
   * session.  That makes the time spent inside the write the connection's head-of-line cost, so
   * the send is bounded by {@link WebSocketTransport#getAsyncSendTimeoutMilliseconds()}: a peer
   * that stops reading delays this connection by at most that long instead of stalling every
   * subsequent delivery indefinitely.  Configuring a non-positive timeout opts back into an
   * unbounded blocking write and is not recommended.</p>
   *
   * <p>Failure is reported rather than swallowed.  A closed session or a failed write is logged
   * at warning severity — not at the message log level, which traces wire content and normally
   * sits below the logging threshold — and yields {@code false}.  Discarding on a closed session
   * is logged only the first time per connection, since a socket that died without a disconnect
   * holds its session until the idle sweep reaps it and would otherwise repeat the same news for
   * every broadcast in between.  Failed writes are always logged.  The protocol is notified only
   * for packets that actually went out.</p>
   *
   * @param packet the {@link Packet} to encode and transmit
   * @return {@code true} when the packet was written, {@code false} when it was discarded or the
   * write failed
   */
  @Override
  public synchronized boolean deliver (Packet<V> packet) {

    // Checking up front avoids encoding for a socket that is already gone, but it is an optimization, not a guarantee - the peer may close while the write is in flight, which the catch below handles
    if (!websocketSession.isOpen()) {
      // A socket that died without a disconnect keeps its session until the idle sweep reaps it, so every broadcast in that window would otherwise repeat this - the first one carries the news
      if (!discardReported) {
        discardReported = true;
        LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), "Connection(%s) is closed, discarding an undelivered packet on channel(%s), and any that follow", getId(), describe(packet));
      }

      return false;
    }

    try {

      String encodedPacket = PacketUtility.encode(packet);
      long asyncSendTimeoutMilliseconds = websocketTransport.getAsyncSendTimeoutMilliseconds();

      LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), () -> "=>" + encodedPacket);

      if (asyncSendTimeoutMilliseconds > 0) {
        websocketSession.getAsyncRemote().sendText(encodedPacket).get(asyncSendTimeoutMilliseconds, TimeUnit.MILLISECONDS);
      } else {
        websocketSession.getBasicRemote().sendText(encodedPacket);
      }

      ((WebsocketProtocol<V>)websocketTransport.getProtocol()).onDelivery(packet);

      return true;
    } catch (TimeoutException timeoutException) {
      // The write may yet complete, so the packet is neither confirmed nor safe to re-send
      LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), "Connection(%s) timed out writing a packet on channel(%s), delivery is unconfirmed", getId(), describe(packet));

      return false;
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), interruptedException, "Connection(%s) was interrupted writing a packet on channel(%s)", getId(), describe(packet));

      return false;
    } catch (IOException | ExecutionException exception) {
      LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), exception, "Connection(%s) failed to deliver a packet on channel(%s)", getId(), describe(packet));

      return false;
    }
  }

  /**
   * Describes the packet in a form fit for a log line, without dragging its payload along.
   *
   * @param packet the packet being described
   * @return the packet's channel path, or its type when the packet carries no route
   */
  private String describe (Packet<V> packet) {

    Route route;

    return ((route = packet.getRoute()) == null) ? String.valueOf(packet.getPacketType()) : route.getPath();
  }

  /**
   * Receives a raw text frame from the WebSocket client, decodes it into Bayeux messages on
   * the server's executor, and dispatches each message through the session or responds
   * directly when no session exists. Triggers cleanup if the session transitions to
   * {@link SessionState#DISCONNECTED} after dispatch, noting when the client closed before its
   * final response could be written — which is ordinary for {@code /meta/disconnect}, since a
   * client that has read {@code reconnect: none} is entitled to hang up immediately.
   *
   * @param content the raw JSON text frame from the client
   */
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

            // The cometd clients ignore the specification when using the reload extension, and they just steal the session without a new handshake.
            boolean dispatched = session.dispatch(packet);

            if (SessionState.DISCONNECTED.equals(session.getState())) {
              if (!dispatched) {
                LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), "Session(%s) closed before its final response could be written", session.getId());
              }

              onCleanup();
            }
          }
        }, messages);
      } catch (IOException ioException) {
        LoggerManager.getLogger(WebSocketEndpoint.class).log(server.getMessageLogLevel(), ioException);
      }
    });
  }

  /**
   * Logs a WebSocket-level error at the error severity level.
   *
   * @param wsSession the session on which the error occurred
   * @param failure   the error raised by the WebSocket container
   */
  @Override
  public synchronized void onError (Session wsSession, Throwable failure) {

    LoggerManager.getLogger(WebSocketEndpoint.class).error(failure);
  }

  /**
   * Closes the underlying WebSocket session if it is still open, logging any
   * {@link IOException} that occurs during closure.
   */
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
