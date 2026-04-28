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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.OumuamuaException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Central Bayeux server that owns the channel tree, session registry, protocol map, and backbone,
 * and routes messages between all of those components.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class OumuamuaServer<V extends Value<V>> extends AbstractAttributed implements Server<V> {

  private final ExecutorService executorService;
  private final ConcurrentHashMap<String, OumuamuaSession<V>> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Protocol<V>> protocolMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Route, BayeuxService<V>> serviceMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<ChannelInitializer<V>> initializerList = new ConcurrentLinkedQueue<>();
  private final OumuamuaConfiguration<V> configuration;
  private final ChannelTree<V> channelTree;
  private final String[] protocolNames;
  private final boolean allowsImplicitConnection;
  private final long sessionConnectionIntervalMilliseconds;

  private IdleChannelSifter<V> idleChannelSifter;
  private IdleSessionInspector<V> idleSessionInspector;

  /**
   * Builds the server from the supplied configuration, wiring protocols, services, and listeners.
   *
   * @param configuration fully populated server configuration; must not be {@code null} and must
   *                      contain at least one protocol and a non-{@code null} codec
   * @throws OumuamuaException if {@code configuration} is {@code null}, its codec is missing, or
   *                           no protocols have been defined
   */
  public OumuamuaServer (OumuamuaConfiguration<V> configuration)
    throws OumuamuaException {

    if (configuration == null) {
      throw new OumuamuaException("Missing configuration");
    } else if (configuration.getCodec() == null) {
      throw new OumuamuaException("Missing codec");
    } else {

      ExecutorService configuredExecutorService;

      this.configuration = configuration;

      executorService = ((configuredExecutorService = configuration.getExecutorService()) != null) ? configuredExecutorService : Executors.newVirtualThreadPerTaskExecutor();

      allowsImplicitConnection = configuration.isAllowsImplicitConnection();
      sessionConnectionIntervalMilliseconds = configuration.getSessionConnectIntervalSeconds() * 1000L;
      channelTree = new ChannelTree<>(new ChannelRoot<>(this));

      if (configuration.getProtocols() == null) {
        throw new OumuamuaException("No protocols have been defined");
      } else {
        for (Protocol<V> protocol : configuration.getProtocols()) {
          protocolMap.put(protocol.getName(), protocol);
        }

        protocolNames = protocolMap.keySet().toArray(new String[0]);
      }

      if (configuration.getServices() != null) {
        for (BayeuxService<V> service : configuration.getServices()) {
          addService(service);
        }
      }

      if (configuration.getListeners() != null) {
        for (Listener<V> listener : configuration.getListeners()) {
          addListener(listener);
        }
      }
    }
  }

  /**
   * Starts the server by bringing up the backbone, initializing each protocol, and launching the
   * idle-channel and idle-session maintenance threads.
   *
   * @param servletConfig servlet configuration forwarded to each protocol's {@code init} method
   * @throws ServletException if the backbone fails to start or a protocol throws during initialization
   */
  public void start (ServletConfig servletConfig)
    throws ServletException {

    Backbone<V> backbone;

    LoggerManager.getLogger(OumuamuaServer.class).info("Oumuamua Server starting...");
    LoggerManager.getLogger(OumuamuaServer.class).info("\n" + JsonCodec.writeAsPrettyPrintedString(OumuamuaConfigurationOutView.instance(configuration)));

    if ((backbone = getBackbone()) != null) {
      try {
        backbone.startUp(this);
      } catch (Exception exception) {
        throw new ServletException(exception);
      }
    }

    for (Protocol<V> protocol : configuration.getProtocols()) {
      protocol.init(this, servletConfig);
      protocolMap.put(protocol.getName(), protocol);
    }

    new Thread(idleChannelSifter = new IdleChannelSifter<>(configuration.getIdleChannelCycleMinutes(), configuration.getIdleCleanupLogLevel(), channelTree, this::onRemoved)).start();
    new Thread(idleSessionInspector = new IdleSessionInspector<>(this, configuration.getIdleSessionCycleMinutes(), configuration.getIdleCleanupLogLevel())).start();

    LoggerManager.getLogger(OumuamuaServer.class).info("Oumuamua Server started...");
  }

  /**
   * Shuts down the backbone, stops both maintenance threads, and terminates the executor service.
   */
  public void stop () {

    Backbone<V> backbone;

    LoggerManager.getLogger(OumuamuaServer.class).info("Oumuamua Server stopping...");

    if ((backbone = getBackbone()) != null) {
      try {
        backbone.shutDown();
      } catch (Exception exception) {
        LoggerManager.getLogger(OumuamuaServer.class).error(exception);
      }
    }

    try {
      idleSessionInspector.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    }

    try {
      idleChannelSifter.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    }

    executorService.shutdown();
    LoggerManager.getLogger(OumuamuaServer.class).info("Oumuamua Server stopped...");
  }

  /**
   * Returns the executor service used to dispatch asynchronous server tasks.
   *
   * @return the configured executor, or the default virtual-thread-per-task executor
   */
  public ExecutorService getExecutorService () {

    return executorService;
  }

  /**
   * Dispatches the session-connected event to all registered {@link SessionListener}s.
   *
   * @param session the session that has just completed its connection handshake
   */
  private void onConnected (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onConnected(session);
      }
    }
  }

  /**
   * Dispatches the session-disconnected event to all registered {@link SessionListener}s.
   *
   * @param session the session that has just disconnected
   */
  private void onDisconnected (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onDisconnected(session);
      }
    }
  }

  /**
   * Dispatches the subscribed event to all registered {@link SubscriptionListener}s.
   *
   * @param channel the channel that was subscribed to
   * @param session the session that performed the subscription
   */
  private void onSubscribed (Channel<V> channel, Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener<V>)listener).onSubscribed(channel, session);
      }
    }
  }

  /**
   * Dispatches the unsubscribed event to all registered {@link SubscriptionListener}s.
   *
   * @param channel the channel that was unsubscribed from
   * @param session the session that performed the unsubscription
   */
  private void onUnsubscribed (Channel<V> channel, Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener<V>)listener).onUnsubscribed(channel, session);
      }
    }
  }

  /**
   * Dispatches the channel-created event to all registered {@link ChannelListener}s.
   *
   * @param channel the newly created channel
   */
  private void onCreated (Channel<V> channel) {

    for (Listener<V> listener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(listener.getClass())) {
        ((ChannelListener<V>)listener).onCreated(channel);
      }
    }
  }

  /**
   * Dispatches the channel-removed event to all registered {@link ChannelListener}s.
   *
   * @param channel the channel that has been removed from the tree
   */
  private void onRemoved (Channel<V> channel) {

    for (Listener<V> listener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(listener.getClass())) {
        ((ChannelListener<V>)listener).onRemoved(channel);
      }
    }
  }

  /**
   * Runs the packet through every registered {@link PacketListener}, giving each one the
   * opportunity to transform or veto it.
   *
   * @param sender the originating session, or {@code null} for backbone-sourced packets
   * @param packet the packet to process; the appropriate listener method is chosen based on its
   *               {@link PacketType}
   * @return the (possibly transformed) packet, or {@code null} if a listener vetoed delivery
   */
  private Packet<V> onProcessing (Session<V> sender, Packet<V> packet) {

    for (Listener<V> listener : listenerList) {
      if (PacketListener.class.isAssignableFrom(listener.getClass())) {
        if (PacketType.REQUEST.equals(packet.getPacketType())) {
          if ((packet = ((PacketListener<V>)listener).onRequest(sender, packet)) == null) {
            break;
          }
        } else if (PacketType.RESPONSE.equals(packet.getPacketType())) {
          if ((packet = ((PacketListener<V>)listener).onResponse(sender, packet)) == null) {
            break;
          }
        } else {
          if ((packet = ((PacketListener<V>)listener).onDelivery(sender, packet)) == null) {
            break;
          }
        }
      }
    }

    return packet;
  }

  /**
   * Registers a {@link BayeuxService} under every route it declares.
   *
   * @param service the service to register; routes with no declared bound routes are silently ignored
   */
  @Override
  public void addService (BayeuxService<V> service) {

    Route[] boundRoutes;

    if ((boundRoutes = service.getBoundRoutes()) != null) {
      for (Route boundRoute : boundRoutes) {
        serviceMap.put(boundRoute, service);
      }
    }
  }

  /**
   * Deregisters the service bound to the given route.
   *
   * @param route the route whose service binding should be removed
   */
  @Override
  public void removeService (Route route) {

    serviceMap.remove(route);
  }

  /**
   * Looks up the service bound to the given route.
   *
   * @param route the route to resolve
   * @return the bound {@link BayeuxService}, or {@code null} if no service is registered for the route
   */
  @Override
  public BayeuxService<V> getService (Route route) {

    return serviceMap.get(route);
  }

  /**
   * Appends a listener to the server's listener chain.
   *
   * @param listener the {@link Listener} to register; may implement any combination of
   *                 {@link SessionListener}, {@link SubscriptionListener}, {@link ChannelListener},
   *                 or {@link PacketListener}
   */
  @Override
  public void addListener (Listener<V> listener) {

    listenerList.add(listener);
  }

  /**
   * Removes a previously registered listener from the server's listener chain.
   *
   * @param listener the listener to remove; no-op if it was never registered
   */
  @Override
  public void removeListener (Listener<V> listener) {

    listenerList.remove(listener);
  }

  /**
   * Returns the Bayeux protocol version advertised during handshake.
   *
   * @return the version string {@code "1.0"}
   */
  @Override
  public String getBayeuxVersion () {

    return "1.0";
  }

  /**
   * Returns the minimum Bayeux protocol version accepted from clients.
   *
   * @return the minimum version string {@code "1.0"}
   */
  @Override
  public String getMinimumBayeuxVersion () {

    return "1.0";
  }

  /**
   * Returns the names of all protocols registered with the server.
   *
   * @return array of protocol name strings; order is not guaranteed
   */
  @Override
  public String[] getProtocolNames () {

    return protocolNames;
  }

  /**
   * Retrieves a registered protocol by its canonical name.
   *
   * @param name the protocol name as returned by {@link Protocol#getName()}
   * @return the matching {@link Protocol}, or {@code null} if no protocol has that name
   */
  @Override
  public Protocol<V> getProtocol (String name) {

    return protocolMap.get(name);
  }

  /**
   * Returns the backbone used for cluster-wide message distribution.
   *
   * @return the configured {@link Backbone}, or {@code null} if no backbone was configured
   */
  @Override
  public Backbone<V> getBackbone () {

    return configuration.getBackbone();
  }

  /**
   * Returns the security policy that governs handshake and subscription authorization.
   *
   * @return the configured {@link SecurityPolicy}, or {@code null} if no policy was set
   */
  @Override
  public SecurityPolicy<V> getSecurityPolicy () {

    return configuration.getSecurityPolicy();
  }

  /**
   * Returns the codec used to serialize and deserialize Bayeux messages.
   *
   * @return the configured {@link Codec}; never {@code null}
   */
  @Override
  public Codec<V> getCodec () {

    return configuration.getCodec();
  }

  /**
   * Indicates whether the server allows clients to publish without an explicit connect message.
   *
   * @return {@code true} if implicit connection is permitted
   */
  @Override
  public boolean allowsImplicitConnection () {

    return allowsImplicitConnection;
  }

  /**
   * Returns the maximum permitted gap between client connect messages before a session is
   * considered stale.
   *
   * @return the interval in milliseconds
   */
  @Override
  public long getSessionConnectionIntervalMilliseconds () {

    return sessionConnectionIntervalMilliseconds;
  }

  /**
   * Indicates whether published messages on the given route are echoed back to the publishing session.
   *
   * @param route the channel route to check
   * @return {@code true} if the publisher should receive its own messages
   */
  @Override
  public boolean isReflecting (Route route) {

    return configuration.isReflecting(route);
  }

  /**
   * Indicates whether messages on the given route bypass the long-poll queue and are pushed
   * directly over the active connection.
   *
   * @param route the channel route to check
   * @return {@code true} if streaming delivery is active for the route
   */
  @Override
  public boolean isStreaming (Route route) {

    return configuration.isStreaming(route);
  }

  /**
   * Returns the log level at which raw inbound and outbound messages are recorded.
   *
   * @return the configured message log level
   */
  public Level getMessageLogLevel () {

    return configuration.getMessageLogLevel();
  }

  /**
   * Instantiates a new {@link OumuamuaSession} bound to the supplied connection without registering
   * it in the session map; call {@link #addSession} to complete registration.
   *
   * @param connection the transport connection that owns the new session
   * @return a freshly created, unregistered session
   */
  public OumuamuaSession<V> createSession (Connection<V> connection) {

    return new OumuamuaSession<>(this::onConnected, this::onDisconnected, connection, configuration.getMaxLongPollQueueSize(), configuration.getSessionMaxIdleTimeoutSeconds() * 1000L, configuration.getOverflowLogLevel());
  }

  /**
   * Looks up an active session by its unique identifier.
   *
   * @param sessionId the session id to find
   * @return the matching session, or {@code null} if no session with that id is registered
   */
  public OumuamuaSession<V> getSession (String sessionId) {

    return sessionMap.get(sessionId);
  }

  /**
   * Adds the session to the active session registry, keyed by its id.
   *
   * @param session the session to register; replaces any existing entry with the same id
   */
  public void addSession (OumuamuaSession<V> session) {

    sessionMap.put(session.getId(), session);
  }

  /**
   * Removes the session from the registry and unsubscribes it from all channels.
   *
   * @param session the session to deregister; no-op if it was not found in the registry
   */
  public void removeSession (OumuamuaSession<V> session) {

    OumuamuaSession<V> removedSession;

    if ((removedSession = sessionMap.remove(session.getId())) != null) {
      departChannels(removedSession);
    }
  }

  /**
   * Walks the entire channel tree and unsubscribes the session from every channel it belongs to.
   *
   * @param session the session being evicted from all channels
   */
  public void departChannels (OumuamuaSession<V> session) {

    channelTree.walk(new RemovedSessionOperation<>(session));
  }

  /**
   * Returns an iterator over the current snapshot of active sessions.
   *
   * @return live iterator from the session registry; supports {@code remove()}
   */
  public Iterator<OumuamuaSession<V>> iterateSessions () {

    return sessionMap.values().iterator();
  }

  /**
   * Registers a {@link ChannelInitializer} that is applied to every channel at creation time.
   *
   * @param initializer the initializer to register
   */
  @Override
  public void addInitializer (ChannelInitializer<V> initializer) {

    initializerList.add(initializer);
  }

  /**
   * Removes a previously registered channel initializer.
   *
   * @param initializer the initializer to remove; no-op if it was never registered
   */
  @Override
  public void removeInitializer (ChannelInitializer<V> initializer) {

    initializerList.remove(initializer);
  }

  /**
   * Returns the channel at the given path if it already exists in the tree.
   *
   * @param path the Bayeux channel path (e.g. {@code "/foo/bar"})
   * @return the existing channel, or {@code null} if no channel is registered at that path
   * @throws InvalidPathException if {@code path} cannot be parsed as a valid route
   */
  @Override
  public Channel<V> findChannel (String path)
    throws InvalidPathException {

    return channelTree.find(0, new DefaultRoute(path));
  }

  /**
   * Returns the channel at the given path, creating it (and any missing intermediate branches)
   * if it does not yet exist and running all registered initializers plus any supplied ones.
   *
   * @param path         the Bayeux channel path to create or retrieve
   * @param initializers zero or more per-call initializers appended after the server-level ones;
   *                     may be {@code null} or empty
   * @return the existing or newly created channel; never {@code null}
   * @throws InvalidPathException if {@code path} cannot be parsed as a valid route
   */
  @Override
  public Channel<V> requireChannel (String path, ChannelInitializer... initializers)
    throws InvalidPathException {

    Queue<ChannelInitializer<V>> combinedInitializers;

    if ((initializers == null) || (initializers.length == 0)) {
      combinedInitializers = initializerList;
    } else {
      combinedInitializers = new LinkedBlockingDeque<>(initializerList);
      combinedInitializers.addAll(Arrays.asList((ChannelInitializer<V>[])initializers));
    }

    return channelTree.createIfAbsent(configuration.getChannelTimeToLiveMinutes() * 60 * 1000, 0, new DefaultRoute(path), this::onCreated, this::onSubscribed, this::onUnsubscribed, combinedInitializers);
  }

  /**
   * Terminates and removes the channel from the tree, firing the removed listener callback.
   *
   * @param channel the channel to remove
   * @throws ChannelStateException if the channel is marked persistent and therefore cannot be removed
   */
  @Override
  public void removeChannel (Channel<V> channel)
    throws ChannelStateException {

    channelTree.removeChannelIfPresent(0, channel.getRoute(), this::onRemoved);
  }

  /**
   * Passes an inbound request packet through all server-level {@link PacketListener}s.
   * Changes made here are visible to all subsequent processing, including the reply to the sender.
   *
   * @param sender the session that sent the request
   * @param packet the request packet; modifications are shared with all downstream processing
   * @return the (possibly transformed) packet, or {@code null} if processing should halt
   */
  @Override
  public Packet<V> onRequest (Session<V> sender, Packet<V> packet) {

    // No need to freeze the packet as changes generated here should be by all further processing, including the response to the sender
    return onProcessing(sender, packet);
  }

  /**
   * Passes an outbound response packet through all server-level {@link PacketListener}s.
   * Changes made here are visible only to the reply sent back to the originating sender.
   *
   * @param sender the session that will receive the response
   * @param packet the response packet; modifications are scoped to the sender
   * @return the (possibly transformed) packet, or {@code null} if processing should halt
   */
  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    // No need to freeze the packet as any changes generated here are specifically for, and seen only by, the sender
    return onProcessing(sender, packet);
  }

  /**
   * Delivers a packet to all matching channel subscribers and, when requested, publishes it to the
   * backbone for cluster-wide distribution.
   *
   * @param sender    the session publishing the packet, or {@code null} for server-initiated delivery
   * @param packet    the packet to deliver; must carry a non-{@code null} route
   * @param clustered {@code true} to also publish through the backbone; pass {@code false} for
   *                  packets already received from the backbone to avoid re-broadcast loops
   */
  @Override
  public void deliver (Session<V> sender, Packet<V> packet, boolean clustered) {

    if (packet.getRoute() != null) {
      // Packet is not frozen as all channels should see these changes
      if ((packet = onProcessing(sender, packet)) != null) {

        channelTree.deliver(sender, 0, packet, new HashSet<>());

        // Do *not* redistribute packets from the backbone
        if (clustered) {

          Backbone<V> backbone;

          if ((backbone = getBackbone()) != null) {
            backbone.publish(packet);
          }
        }
      }
    }
  }

  /**
   * Delivers a packet directly to the given channel's subscribers and publishes it to the backbone.
   * Intended for server-initiated publishes that originate on a specific channel rather than
   * flowing through the full tree traversal.
   *
   * @param channel the channel whose subscribers should receive the packet
   * @param packet  the packet to deliver; must carry a non-{@code null} route
   */
  @Override
  public void forward (Channel<V> channel, Packet<V> packet) {

    if (packet.getRoute() != null) {
      // Packet is not frozen as all channels should see these changes
      if ((packet = onProcessing(null, packet)) != null) {

        Backbone<V> backbone;

        channel.deliver(null, packet, new HashSet<>());

        if ((backbone = getBackbone()) != null) {
          backbone.publish(packet);
        }
      }
    }
  }
}
