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

/**
 * Represents a logical connection capable of processing inbound Bayeux messages and producing packets.
 *
 * @param <V> concrete value type used in messages
 */
public interface Connection<V extends Value<V>> {

  /**
   * Processes a batch of incoming messages, routing them through the meta lifecycle and sending results to the response consumer.
   *
   * @param server           server handling the messages
   * @param responseConsumer callback for generated packets
   * @param messages         incoming messages
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

          packet = cycle(meta, route, server, session, message);
        }
      } catch (IOException | InterruptedException | InvalidPathException | MetaProcessingException exception) {
        packet = new Packet<>(PacketType.RESPONSE, sessionId, null, Meta.constructErrorResponse(server, path, message.getId(), sessionId, exception.getMessage(), null));
      }

      if (packet != null) {
        responseConsumer.accept(session, packet);
      }
    }
  }

  /**
   * Executes the full meta cycle for a message.
   *
   * @param meta    meta operation represented by the message
   * @param route   resolved route
   * @param server  hosting server
   * @param session session associated with the message
   * @param request the incoming message
   * @return resulting packet or {@code null} if nothing should be sent
   * @throws IOException          if encoding fails
   * @throws InterruptedException if waiting for responses is interrupted
   * @throws InvalidPathException if the route is invalid
   */
  private Packet<V> cycle (Meta meta, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws IOException, InterruptedException, InvalidPathException {

    Packet<V> packet;

    updateSession(session);
    packet = respond(meta, route, server, session, request);

    if (SessionState.DISCONNECTED.equals(session.getState())) {
      onDisconnect(server, session);
    }

    return packet;
  }

  /**
   * Routes the request through protocol handlers and aggregates the response.
   *
   * @param meta    meta operation represented by the message
   * @param route   resolved route
   * @param server  hosting server
   * @param session session associated with the message
   * @param request processed request message
   * @return response packet or {@code null} when none should be returned
   * @throws InterruptedException if waiting for responses is interrupted
   * @throws InvalidPathException if the route is invalid
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
   * @return unique identifier for this connection
   */
  String getId ();

  /**
   * @return the transport backing this connection
   */
  Transport<V> getTransport ();

  /**
   * Creates a new session associated with the connection.
   *
   * @param server hosting server
   * @return newly created session
   */
  Session<V> createSession (Server<V> server);

  /**
   * Validates that the supplied session is compatible with this connection.
   *
   * @param session session to validate
   * @return {@code true} when acceptable
   */
  boolean validateSession (Session<V> session);

  /**
   * Updates session state prior to processing a request.
   *
   * @param session session to update
   */
  void updateSession (Session<V> session);

  /**
   * Claims a session that may have been previously associated with another connection.
   *
   * @param session session to hijack
   */
  void hijackSession (Session<V> session);

  /**
   * Callback invoked when a session transitions to disconnected.
   *
   * @param server  hosting server
   * @param session disconnected session
   */
  void onDisconnect (Server<V> server, Session<V> session);

  /**
   * Performs any cleanup when the connection is being discarded.
   */
  void onCleanup ();

  /**
   * Delivers an outgoing packet to the connection.
   *
   * @param packet packet to send
   */
  void deliver (Packet<V> packet);
}
