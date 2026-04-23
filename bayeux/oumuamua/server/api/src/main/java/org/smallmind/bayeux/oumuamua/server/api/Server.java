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
package org.smallmind.bayeux.oumuamua.server.api;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Central Bayeux server contract responsible for channel and session management, protocol routing,
 * security enforcement, backbone integration, and the full request/response/delivery pipeline.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface Server<V extends Value<V>> extends Attributed {

  String ATTRIBUTE = "org.smallmind.bayeux.oumuamua.server";

  /**
   * Base marker type for server-level event listeners.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface Listener<V extends Value<V>> {

  }

  /**
   * Notified when client sessions connect to or disconnect from the server.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface SessionListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called after a session successfully completes its connection handshake.
     *
     * @param session newly connected session
     */
    void onConnected (Session<V> session);

    /**
     * Called after a session has been fully disconnected and removed.
     *
     * @param session session that disconnected
     */
    void onDisconnected (Session<V> session);
  }

  /**
   * Notified when channels are created or removed from the server.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface ChannelListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called immediately after a new channel is created and registered.
     *
     * @param channel the newly created channel
     */
    void onCreated (Channel<V> channel);

    /**
     * Called immediately after a channel is deregistered and removed.
     *
     * @param channel the removed channel
     */
    void onRemoved (Channel<V> channel);
  }

  /**
   * Notified when sessions subscribe to or unsubscribe from channels.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface SubscriptionListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called after a session successfully subscribes to a channel.
     *
     * @param channel channel that was subscribed to
     * @param session session that subscribed
     */
    void onSubscribed (Channel<V> channel, Session<V> session);

    /**
     * Called after a session unsubscribes from a channel.
     *
     * @param channel channel that was unsubscribed from
     * @param session session that unsubscribed
     */
    void onUnsubscribed (Channel<V> channel, Session<V> session);
  }

  /**
   * Intercepts packets at each stage of server-level processing, with the ability to transform
   * or replace the packet before it continues through the pipeline.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called when an inbound request packet arrives from a client; may return a replacement packet.
     *
     * @param sender session that sent the request
     * @param packet incoming request packet
     * @return packet to continue processing, possibly transformed
     */
    Packet<V> onRequest (Session<V> sender, Packet<V> packet);

    /**
     * Called when an outbound response packet is about to be returned to a client; may return
     * a replacement packet.
     *
     * @param sender session that will receive the response
     * @param packet outgoing response packet
     * @return packet to continue processing, possibly transformed
     */
    Packet<V> onResponse (Session<V> sender, Packet<V> packet);

    /**
     * Called when a delivery packet is being routed to channel subscribers; may return
     * a replacement packet.
     *
     * @param sender session that originally published the message
     * @param packet delivery packet being distributed
     * @return packet to continue delivering, possibly transformed
     */
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  /**
   * Registers a service and binds it to each of its declared routes.
   *
   * @param service service to register
   */
  void addService (BayeuxService<V> service);

  /**
   * Removes the service bound to the given route.
   *
   * @param route route whose service should be deregistered
   */
  void removeService (Route route);

  /**
   * Looks up the service registered for the given route.
   *
   * @param route route to look up
   * @return the bound service, or {@code null} if none is registered for that route
   */
  BayeuxService<V> getService (Route route);

  /**
   * Registers a server-level event listener.
   *
   * @param listener listener to add
   */
  void addListener (Listener<V> listener);

  /**
   * Deregisters a server-level event listener.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);

  /**
   * Starts the server, initializing all registered protocols and transports.
   *
   * @param servletConfig servlet configuration supplied to transports during initialization
   * @throws ServletException if any protocol or transport fails to initialize
   */
  void start (ServletConfig servletConfig)
    throws ServletException;

  /**
   * Shuts down the server and releases all held resources.
   */
  void stop ();

  /**
   * Returns the Bayeux protocol version this server implements.
   *
   * @return Bayeux version string (e.g. {@code "1.0"})
   */
  String getBayeuxVersion ();

  /**
   * Returns the oldest Bayeux protocol version this server will accept from clients.
   *
   * @return minimum acceptable Bayeux version string
   */
  String getMinimumBayeuxVersion ();

  /**
   * Returns how long the server will maintain a session connection without a reconnect before
   * considering the client gone.
   *
   * @return connection keep-alive interval in milliseconds
   */
  long getSessionConnectionIntervalMilliseconds ();

  /**
   * Returns whether clients may connect and publish without first completing a handshake.
   *
   * @return {@code true} if implicit connection is permitted
   */
  boolean allowsImplicitConnection ();

  /**
   * Returns the names of all protocols registered with this server.
   *
   * @return array of protocol names
   */
  String[] getProtocolNames ();

  /**
   * Looks up a protocol by name.
   *
   * @param name protocol name to resolve
   * @return matching protocol instance
   */
  Protocol<V> getProtocol (String name);

  /**
   * Returns the backbone used to distribute packets to other nodes in a cluster.
   *
   * @return configured backbone, or {@code null} if clustering is disabled
   */
  Backbone<V> getBackbone ();

  /**
   * Returns the security policy consulted for all access-control decisions.
   *
   * @return active security policy
   */
  SecurityPolicy<V> getSecurityPolicy ();

  /**
   * Returns the codec used to serialize and deserialize Bayeux messages; serves as an injection
   * point for implementations that want to wrap or extend the default JSON pipeline.
   *
   * @return active message codec
   */
  // Serves as an injection point for implementations that wish to add client configurable additions to the json codec pipeline
  Codec<V> getCodec ();

  /**
   * Returns whether the channel at the given route reflects published messages back to the sender.
   *
   * @param route route identifying the channel to inspect
   * @return {@code true} if the channel is configured to reflect
   */
  boolean isReflecting (Route route);

  /**
   * Returns whether the channel at the given route delivers messages in streaming mode.
   *
   * @param route route identifying the channel to inspect
   * @return {@code true} if the channel is configured for streaming delivery
   */
  boolean isStreaming (Route route);

  /**
   * Looks up a session by its unique identifier.
   *
   * @param sessionId session identifier to resolve
   * @return matching session, or {@code null} if no such session exists
   */
  Session<V> getSession (String sessionId);

  /**
   * Registers a channel initializer that is applied whenever a new channel is created.
   *
   * @param initializer initializer to add to the initialization chain
   */
  void addInitializer (ChannelInitializer<V> initializer);

  /**
   * Deregisters a channel initializer.
   *
   * @param initializer initializer to remove
   */
  void removeInitializer (ChannelInitializer<V> initializer);

  /**
   * Returns the existing channel at the given path, or {@code null} if it does not exist.
   *
   * @param path channel path to look up
   * @return existing channel, or {@code null}
   * @throws InvalidPathException if the path string is syntactically invalid
   */
  Channel<V> findChannel (String path)
    throws InvalidPathException;

  /**
   * Returns the channel at the given path, creating it if necessary and applying all registered
   * initializers before returning a newly created channel.
   *
   * @param path         channel path to locate or create
   * @param initializers additional initializers applied only if the channel is created by this call
   * @return existing or newly created channel
   * @throws InvalidPathException if the path string is syntactically invalid
   */
  // Initializers will be applied before the channel is returned if this call creates the channel
  Channel<V> requireChannel (String path, ChannelInitializer... initializers)
    throws InvalidPathException;

  /**
   * Removes a channel from the server, unsubscribing all sessions and notifying listeners.
   * Attempting to remove a persistent channel is an error.
   *
   * @param channel channel to remove
   * @throws ChannelStateException if the channel is persistent or cannot otherwise be removed
   */
  // It's an error to attempt to remove a persistent channel
  // All sessions must be unsubscribed (and listeners notified) upon channel removal
  void removeChannel (Channel<V> channel)
    throws ChannelStateException;

  /**
   * Passes a request packet through the server's {@link PacketListener} chain and returns
   * the final packet.
   *
   * @param sender session that sent the request
   * @param packet inbound request packet
   * @return packet after all listeners have processed it
   */
  Packet<V> onRequest (Session<V> sender, Packet<V> packet);

  /**
   * Passes a response packet through the server's {@link PacketListener} chain and returns
   * the final packet.
   *
   * @param sender session receiving the response
   * @param packet outbound response packet
   * @return packet after all listeners have processed it
   */
  Packet<V> onResponse (Session<V> sender, Packet<V> packet);

  /**
   * Routes a packet to its target channel subscribers, optionally publishing to the backbone
   * for cluster-wide distribution.
   *
   * @param sender    session originating the packet
   * @param packet    packet to deliver
   * @param clustered {@code true} to also forward the packet to the backbone for other nodes
   */
  void deliver (Session<V> sender, Packet<V> packet, boolean clustered);

  /**
   * Pushes a packet directly to the packet listeners of a channel without involving transports,
   * used for server-side local fan-out.
   *
   * @param channel channel whose packet listeners should receive the packet
   * @param packet  packet to forward
   */
  void forward (Channel<V> channel, Packet<V> packet);
}
