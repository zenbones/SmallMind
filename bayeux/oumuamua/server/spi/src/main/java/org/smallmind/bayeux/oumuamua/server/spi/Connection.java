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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.io.IOException;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Reconnect;

/**
 * Logical connection through which a client exchanges Bayeux messages with the server;
 * encapsulates session lifecycle, meta-channel routing, and packet delivery for one transport endpoint.
 *
 * @param <V> concrete {@link Value} type carried in Bayeux messages
 */
public interface Connection<V extends Value<V>> {

  /**
   * Processes an array of inbound messages through the full Bayeux meta lifecycle, delivering
   * each resulting packet to {@code responseConsumer}; errors are converted to error-response packets.
   *
   * @param server           server that owns this connection
   * @param responseConsumer callback that receives each generated response packet paired with its session
   * @param messages         messages received from the client in one batch
   */
  default void process (Server<V> server, ResponseConsumer<V> responseConsumer, Message<V>[] messages) {

    for (Message<V> message : messages) {

      Session<V> session = null;
      Packet<V> packet;
      String path = message.getChannel();
      String sessionId = message.getSessionId();

      try {

        Meta meta = Meta.from(path);
        Route route = switch (meta) {
          case PUBLISH, SERVICE -> new DefaultRoute(path);
          default -> meta.getRoute();
        };

        if (sessionId == null) {
          if (Meta.HANDSHAKE.equals(meta)) {
            session = createSession(server);

            if (SessionState.DISCONNECTED.equals(session.getState())) {
              throw new MetaProcessingException("Session has been disconnected");
            } else {
              packet = cycle(meta, route, server, session, message);
            }
          } else {
            throw new MetaProcessingException("Missing client id");
          }
        } else if ((session = server.getSession(sessionId)) == null) {
          throw new MetaProcessingException("Invalid client id");
        } else if (!validateSession(session)) {
          throw new MetaProcessingException("Invalid session type");
        } else if (SessionState.DISCONNECTED.equals(session.getState())) {
          throw new MetaProcessingException("Session has been disconnected");
        } else {
          // The cometd clients ignore the specification when using the reload extension, and just steals the session without a new handshake.
          if (Meta.CONNECT.equals(meta) || Meta.SUBSCRIBE.equals(meta)) {
            hijackSession(session);
          }

          updateSession(session);
          packet = cycle(meta, route, server, session, message);
        }
      } catch (MetaProcessingException exception) {
        packet = new Packet<>(PacketType.RESPONSE, sessionId, null, Meta.constructErrorResponse(server, path, message.getId(), sessionId, exception.getMessage(), Reconnect.HANDSHAKE));
      } catch (IOException | InterruptedException | InvalidPathException exception) {
        packet = new Packet<>(PacketType.RESPONSE, sessionId, null, Meta.constructErrorResponse(server, path, message.getId(), sessionId, exception.getMessage(), null));
      }

      if (packet != null) {
        responseConsumer.accept(session, packet);
      }
    }
  }

  /**
   * Delegates to {@link #respond} and triggers disconnect handling when the session transitions
   * to disconnected as a result of processing.
   *
   * @param meta    meta operation identified from the message channel
   * @param route   resolved {@link Route} for the message
   * @param server  server hosting this connection
   * @param session session associated with the message
   * @param request inbound message being processed
   * @return the response packet, or {@code null} if no response should be sent
   * @throws IOException          if packet encoding fails
   * @throws InterruptedException if the thread is interrupted while awaiting a response
   * @throws InvalidPathException if the resolved route contains an invalid path
   */
  private Packet<V> cycle (Meta meta, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws IOException, InterruptedException, InvalidPathException {

    Packet<V> packet;

    packet = respond(meta, route, server, session, request);

    if (SessionState.DISCONNECTED.equals(session.getState())) {
      onDisconnect(server, session);
    }

    return packet;
  }

  /**
   * Submits the request packet to {@link Server#onRequest}, runs each processed message through
   * the meta handler and {@link Server#onResponse}, and merges all response packets into one.
   *
   * @param meta    meta operation identified from the message channel
   * @param route   resolved {@link Route} for the message
   * @param server  server hosting this connection
   * @param session session associated with the message
   * @param request inbound message to process
   * @return merged response packet, or {@code null} if {@code onRequest} suppressed the message
   * @throws InterruptedException if the thread is interrupted while awaiting a response
   * @throws InvalidPathException if the resolved route contains an invalid path
   */
  private Packet<V> respond (Meta meta, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws InterruptedException, InvalidPathException {

    Packet<V> processedRequestPacket;

    if ((processedRequestPacket = server.onRequest(session, new Packet<>(PacketType.REQUEST, session.getId(), route, request))) == null) {

      return null;
    } else {

      Packet<V> mergedResponsePacket = null;

      for (Message<V> processedRequestMessage : processedRequestPacket.getMessages()) {

        Packet<V> processedResponsePacket;

        if ((processedResponsePacket = server.onResponse(session, meta.process(getTransport().getProtocol(), route, server, session, processedRequestMessage))) != null) {
          if (mergedResponsePacket == null) {
            mergedResponsePacket = processedResponsePacket;
          } else {
            mergedResponsePacket = PacketUtility.merge(mergedResponsePacket, processedResponsePacket, null, false);
          }
        }
      }

      return mergedResponsePacket;
    }
  }

  /**
   * Returns the unique identifier assigned to this connection.
   *
   * @return connection identifier
   */
  String getId ();

  /**
   * Returns the transport underlying this connection.
   *
   * @return transport instance used to send and receive data
   */
  Transport<V> getTransport ();

  /**
   * Creates a new server-side session to represent the client on this connection.
   *
   * @param server server that will own the new session
   * @return the newly created session
   */
  Session<V> createSession (Server<V> server);

  /**
   * Determines whether the given session is of the type expected for this connection.
   *
   * @param session session to examine
   * @return {@code true} if the session is compatible with this connection type
   */
  boolean validateSession (Session<V> session);

  /**
   * Refreshes any connection-specific state on the session before request processing begins.
   *
   * @param session session to update
   */
  void updateSession (Session<V> session);

  /**
   * Associates an existing session with this connection, displacing any prior association;
   * used to support clients that reuse sessions across reconnects without re-handshaking.
   *
   * @param session session to claim for this connection
   */
  void hijackSession (Session<V> session);

  /**
   * Called after a session transitions to the disconnected state so the connection can
   * perform any server-side cleanup or notification.
   *
   * @param server  server that owns the session
   * @param session session that has just disconnected
   */
  void onDisconnect (Server<V> server, Session<V> session);

  /**
   * Releases any resources held by this connection when it is permanently discarded.
   */
  void onCleanup ();

  /**
   * Pushes an outgoing packet to the client over this connection.
   *
   * @param packet packet to deliver
   */
  void deliver (Packet<V> packet);
}
