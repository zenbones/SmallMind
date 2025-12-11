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
package org.smallmind.bayeux.oumuamua.server.api;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Contract for a Bayeux server capable of managing channels, sessions, transports, and backbone communication.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface Server<V extends Value<V>> extends Attributed {

  String ATTRIBUTE = "org.smallmind.bayeux.oumuamua.server";

  /**
   * Marker interface for server listeners.
   */
  interface Listener<V extends Value<V>> {

  }

  /**
   * Listener for session lifecycle events.
   */
  interface SessionListener<V extends Value<V>> extends Listener<V> {

    /**
     * Invoked when a session connects.
     *
     * @param session the connected session
     */
    void onConnected (Session<V> session);

    /**
     * Invoked when a session disconnects.
     *
     * @param session the disconnected session
     */
    void onDisconnected (Session<V> session);
  }

  /**
   * Listener for channel creation and removal.
   */
  interface ChannelListener<V extends Value<V>> extends Listener<V> {

    /**
     * Invoked when a channel is created.
     *
     * @param channel the new channel
     */
    void onCreated (Channel<V> channel);

    /**
     * Invoked when a channel is removed.
     *
     * @param channel the removed channel
     */
    void onRemoved (Channel<V> channel);
  }

  /**
   * Listener for subscription activity.
   */
  interface SubscriptionListener<V extends Value<V>> extends Listener<V> {

    /**
     * Invoked when a session subscribes to a channel.
     *
     * @param channel the channel subscribed to
     * @param session the subscribing session
     */
    void onSubscribed (Channel<V> channel, Session<V> session);

    /**
     * Invoked when a session unsubscribes from a channel.
     *
     * @param channel the channel unsubscribed from
     * @param session the unsubscribing session
     */
    void onUnsubscribed (Channel<V> channel, Session<V> session);
  }

  /**
   * Listener for server-level packet processing.
   */
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called when a request packet is received.
     *
     * @param sender the sending session
     * @param packet the request packet
     * @return the packet to continue processing, possibly transformed
     */
    Packet<V> onRequest (Session<V> sender, Packet<V> packet);

    /**
     * Called when a response packet is about to be delivered.
     *
     * @param sender the sending session
     * @param packet the response packet
     * @return the packet to continue processing, possibly transformed
     */
    Packet<V> onResponse (Session<V> sender, Packet<V> packet);

    /**
     * Called when a delivery packet is being routed to subscribers.
     *
     * @param sender the sending session
     * @param packet the delivery packet
     * @return the packet to continue processing, possibly transformed
     */
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  /**
   * Registers a Bayeux service at its bound routes.
   *
   * @param service service to add
   */
  void addService (BayeuxService<V> service);

  /**
   * Removes a service bound to the specified route.
   *
   * @param route the route whose service should be removed
   */
  void removeService (Route route);

  /**
   * Retrieves a service by route.
   *
   * @param route target route
   * @return the service, or {@code null} if none exists
   */
  BayeuxService<V> getService (Route route);

  /**
   * Adds a server listener.
   *
   * @param listener listener to register
   */
  void addListener (Listener<V> listener);

  /**
   * Removes a server listener.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);

  /**
   * Starts the server using servlet configuration.
   *
   * @param servletConfig servlet configuration
   * @throws ServletException if initialization fails
   */
  void start (ServletConfig servletConfig)
    throws ServletException;

  /**
   * Stops the server and releases resources.
   */
  void stop ();

  /**
   * @return the Bayeux protocol version supported
   */
  String getBayeuxVersion ();

  /**
   * @return the minimum Bayeux protocol version accepted
   */
  String getMinimumBayeuxVersion ();

  /**
   * @return interval in milliseconds to keep a session connection alive
   */
  long getSessionConnectionIntervalMilliseconds ();

  /**
   * @return {@code true} if sessions may connect implicitly without explicit handshake
   */
  boolean allowsImplicitConnection ();

  /**
   * @return names of supported protocols
   */
  String[] getProtocolNames ();

  /**
   * Resolves a protocol by name.
   *
   * @param name protocol name
   * @return protocol instance
   */
  Protocol<V> getProtocol (String name);

  /**
   * @return the backbone used for clustered messaging
   */
  Backbone<V> getBackbone ();

  /**
   * @return the security policy applied to incoming operations
   */
  SecurityPolicy<V> getSecurityPolicy ();

  /**
   * Provides the codec used to encode/decode messages.
   *
   * @return the codec
   */
  // Serves as an injection point for implementations that wish to add client configurable additions to the json codec pipeline
  Codec<V> getCodec ();

  /**
   * Determines whether the channel identified by the route will reflect published messages to the sender.
   *
   * @param route route to inspect
   * @return {@code true} if reflecting
   */
  boolean isReflecting (Route route);

  /**
   * Determines whether the channel identified by the route streams data.
   *
   * @param route route to inspect
   * @return {@code true} if streaming
   */
  boolean isStreaming (Route route);

  /**
   * Retrieves a session by id.
   *
   * @param sessionId the session identifier
   * @return the session, or {@code null} if absent
   */
  Session<V> getSession (String sessionId);

  /**
   * Adds a channel initializer applied when channels are created.
   *
   * @param initializer initializer to add
   */
  void addInitializer (ChannelInitializer<V> initializer);

  /**
   * Removes a previously added initializer.
   *
   * @param initializer initializer to remove
   */
  void removeInitializer (ChannelInitializer<V> initializer);

  /**
   * Finds an existing channel by path.
   *
   * @param path channel path
   * @return the channel if present
   * @throws InvalidPathException if the path is invalid
   */
  Channel<V> findChannel (String path)
    throws InvalidPathException;

  /**
   * Locates or creates a channel for the given path, applying any initializers.
   *
   * @param path channel path
   * @param initializers optional initializers applied when creating the channel
   * @return the existing or new channel
   * @throws InvalidPathException if the path is invalid
   */
  // Initializers will be applied before the channel is returned if this call creates the channel
  Channel<V> requireChannel (String path, ChannelInitializer... initializers)
    throws InvalidPathException;

  /**
   * Removes a channel from the server.
   *
   * @param channel the channel to remove
   * @throws ChannelStateException if the channel cannot be removed
   */
  // It's an error to attempt to remove a persistent channel
  // All sessions must be unsubscribed (and listeners notified) upon channel removal
  void removeChannel (Channel<V> channel)
    throws ChannelStateException;

  /**
   * Intercepts an incoming request packet.
   *
   * @param sender the originating session
   * @param packet the request packet
   * @return the processed packet
   */
  Packet<V> onRequest (Session<V> sender, Packet<V> packet);

  /**
   * Intercepts a response packet.
   *
   * @param sender the originating session
   * @param packet the response packet
   * @return the processed packet
   */
  Packet<V> onResponse (Session<V> sender, Packet<V> packet);

  /**
   * Delivers a packet to the appropriate transport(s).
   *
   * @param sender the originating session
   * @param packet the packet to deliver
   * @param clustered {@code true} when forwarding across backbone
   */
  void deliver (Session<V> sender, Packet<V> packet, boolean clustered);

  /**
   * Forwards a packet to listeners on a channel without invoking transports.
   *
   * @param channel target channel
   * @param packet packet to forward
   */
  void forward (Channel<V> channel, Packet<V> packet);
}
