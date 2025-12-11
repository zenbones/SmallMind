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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * Core server implementation that coordinates protocols, transports, sessions, channels, and services.
 *
 * @param <V> value representation
 */
public class OumuamuaServer<V extends Value<V>> extends AbstractAttributed implements Server<V> {

  private final ExecutorService executorService;
  private final ConcurrentHashMap<String, OumuamuaSession<V>> sessionMap = new ConcurrentHashMap<>();
  private final HashMap<String, Protocol<V>> protocolMap = new HashMap<>();
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
   * Constructs a server using the supplied configuration, registering protocols, services, and listeners.
   *
   * @param configuration server configuration
   * @throws OumuamuaException if required components are missing
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

      executorService = ((configuredExecutorService = configuration.getExecutorService()) != null) ? configuredExecutorService : new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

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
   * Starts the server, initializing the backbone and maintenance tasks.
   *
   * @param servletConfig servlet configuration provided by the container
   * @throws ServletException if startup fails
   */
  public void start (ServletConfig servletConfig)
    throws ServletException {

    Backbone<V> backbone;

    LoggerManager.getLogger(OumuamuaServer.class).info("Oumuamua Server starting...");

    try {
      LoggerManager.getLogger(OumuamuaServer.class).info("\n" + JsonCodec.writeAsPrettyPrintedString(OumuamuaConfigurationOutView.instance(configuration)));
    } catch (JsonProcessingException jsonProcessingException) {
      throw new ServletException(jsonProcessingException);
    }

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
   * Stops background processing and releases executor resources.
   */
  public void stop () {

    Backbone<V> backbone;

    LoggerManager.getLogger(OumuamuaServer.class).info("Oumuamua Server stopping...");

    if ((backbone = getBackbone()) != null) {
      try {
        backbone.shutDown();
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
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
   * @return executor used for server tasks
   */
  public ExecutorService getExecutorService () {

    return executorService;
  }

  /**
   * Listener invoked when a session completes its connection phase.
   *
   * @param session connected session
   */
  private void onConnected (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onConnected(session);
      }
    }
  }

  /**
   * Listener invoked when a session disconnects.
   *
   * @param session disconnected session
   */
  private void onDisconnected (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onDisconnected(session);
      }
    }
  }

  /**
   * Notifies listeners of a subscription event.
   *
   * @param channel subscribed channel
   * @param session subscribing session
   */
  private void onSubscribed (Channel<V> channel, Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener<V>)listener).onSubscribed(channel, session);
      }
    }
  }

  /**
   * Notifies listeners of an unsubscribe event.
   *
   * @param channel unsubscribed channel
   * @param session unsubscribing session
   */
  private void onUnsubscribed (Channel<V> channel, Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener<V>)listener).onUnsubscribed(channel, session);
      }
    }
  }

  /**
   * Notifies listeners when a channel is created.
   *
   * @param channel channel that was created
   */
  private void onCreated (Channel<V> channel) {

    for (Listener<V> listener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(listener.getClass())) {
        ((ChannelListener<V>)listener).onCreated(channel);
      }
    }
  }

  /**
   * Notifies listeners when a channel is removed.
   *
   * @param channel channel that was removed
   */
  private void onRemoved (Channel<V> channel) {

    for (Listener<V> listener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(listener.getClass())) {
        ((ChannelListener<V>)listener).onRemoved(channel);
      }
    }
  }

  /**
   * Applies server-level packet listeners to an inbound packet.
   *
   * @param sender originating session
   * @param packet packet to process
   * @return potentially modified packet, or {@code null} to halt processing
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
   * Registers a Bayeux service.
   *
   * @param service service to add
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
   * Removes the service bound to the given route.
   *
   * @param route service route
   */
  @Override
  public void removeService (Route route) {

    serviceMap.remove(route);
  }

  /**
   * Resolves a service for the supplied route.
   *
   * @param route service route
   * @return matching service or {@code null}
   */
  @Override
  public BayeuxService<V> getService (Route route) {

    return serviceMap.get(route);
  }

  /**
   * Adds a server listener.
   *
   * @param listener listener to register
   */
  @Override
  public void addListener (Listener<V> listener) {

    listenerList.add(listener);
  }

  /**
   * Removes a server listener.
   *
   * @param listener listener to remove
   */
  @Override
  public void removeListener (Listener<V> listener) {

    listenerList.remove(listener);
  }

  /**
   * @return Bayeux protocol version exposed by the server
   */
  @Override
  public String getBayeuxVersion () {

    return "1.0";
  }

  /**
   * @return minimum Bayeux version supported by the server
   */
  @Override
  public String getMinimumBayeuxVersion () {

    return "1.0";
  }

  /**
   * @return array of configured protocol names
   */
  @Override
  public String[] getProtocolNames () {

    return protocolNames;
  }

  /**
   * Looks up a protocol by name.
   *
   * @param name protocol name
   * @return protocol or {@code null} if none registered
   */
  @Override
  public Protocol<V> getProtocol (String name) {

    return protocolMap.get(name);
  }

  /**
   * @return configured backbone implementation
   */
  @Override
  public Backbone<V> getBackbone () {

    return configuration.getBackbone();
  }

  /**
   * @return active security policy, or {@code null} if none
   */
  @Override
  public SecurityPolicy<V> getSecurityPolicy () {

    return configuration.getSecurityPolicy();
  }

  /**
   * @return codec used for message encoding/decoding
   */
  @Override
  public Codec<V> getCodec () {

    return configuration.getCodec();
  }

  /**
   * @return whether implicit connection is permitted
   */
  @Override
  public boolean allowsImplicitConnection () {

    return allowsImplicitConnection;
  }

  /**
   * @return session connect interval in milliseconds
   */
  @Override
  public long getSessionConnectionIntervalMilliseconds () {

    return sessionConnectionIntervalMilliseconds;
  }

  /**
   * Indicates whether the route should reflect messages back to the sender.
   *
   * @param route channel route
   * @return {@code true} if reflection is enabled
   */
  @Override
  public boolean isReflecting (Route route) {

    return configuration.isReflecting(route);
  }

  /**
   * Indicates whether the route should stream data.
   *
   * @param route channel route
   * @return {@code true} if streaming is enabled
   */
  @Override
  public boolean isStreaming (Route route) {

    return configuration.isStreaming(route);
  }

  /**
   * @return log level used for message handling
   */
  public Level getMessageLogLevel () {

    return configuration.getMessageLogLevel();
  }

  /**
   * Creates a new session for the provided connection.
   *
   * @param connection transport connection
   * @return new session
   */
  public OumuamuaSession<V> createSession (Connection<V> connection) {

    return new OumuamuaSession<>(this::onConnected, this::onDisconnected, connection, configuration.getMaxLongPollQueueSize(), configuration.getSessionMaxIdleTimeoutSeconds() * 1000L, configuration.getOverflowLogLevel());
  }

  /**
   * Retrieves a session by id.
   *
   * @param sessionId session identifier
   * @return session or {@code null}
   */
  public OumuamuaSession<V> getSession (String sessionId) {

    return sessionMap.get(sessionId);
  }

  /**
   * Registers a session with the server.
   *
   * @param session session to register
   */
  public void addSession (OumuamuaSession<V> session) {

    sessionMap.put(session.getId(), session);
  }

  /**
   * Removes a session from the server, departing its channels.
   *
   * @param session session to remove
   */
  public void removeSession (OumuamuaSession<V> session) {

    OumuamuaSession<V> removedSession;

    if ((removedSession = sessionMap.remove(session.getId())) != null) {
      departChannels(removedSession);
    }
  }

  /**
   * Unsubscribes the session from all channels.
   *
   * @param session session departing
   */
  public void departChannels (OumuamuaSession<V> session) {

    channelTree.walk(new RemovedSessionOperation<>(session));
  }

  /**
   * @return iterator over active sessions
   */
  public Iterator<OumuamuaSession<V>> iterateSessions () {

    return sessionMap.values().iterator();
  }

  /**
   * Adds a channel initializer run when channels are created.
   *
   * @param initializer initializer to add
   */
  @Override
  public void addInitializer (ChannelInitializer<V> initializer) {

    initializerList.add(initializer);
  }

  /**
   * Removes a channel initializer.
   *
   * @param initializer initializer to remove
   */
  @Override
  public void removeInitializer (ChannelInitializer<V> initializer) {

    initializerList.remove(initializer);
  }

  /**
   * Finds an existing channel by path.
   *
   * @param path channel path
   * @return channel or {@code null}
   * @throws InvalidPathException if the path is malformed
   */
  @Override
  public Channel<V> findChannel (String path)
    throws InvalidPathException {

    return channelTree.find(0, new DefaultRoute(path));
  }

  /**
   * Retrieves a channel, creating it (and applying initializers) if absent.
   *
   * @param path channel path
   * @param initializers optional initializers to apply
   * @return channel instance
   * @throws InvalidPathException if the path is invalid
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
   * Removes a channel if present, honoring persistence rules.
   *
   * @param channel channel to remove
   * @throws ChannelStateException if removal is not allowed
   */
  @Override
  public void removeChannel (Channel<V> channel)
    throws ChannelStateException {

    channelTree.removeChannelIfPresent(0, channel.getRoute(), this::onRemoved);
  }

  /**
   * Applies listeners to a request packet.
   *
   * @param sender originating session
   * @param packet request packet
   * @return processed packet or {@code null} to stop processing
   */
  @Override
  public Packet<V> onRequest (Session<V> sender, Packet<V> packet) {

    // No need to freeze the packet as changes generated here should be by all further processing, including the response to the sender
    return onProcessing(sender, packet);
  }

  /**
   * Applies listeners to a response packet.
   *
   * @param sender originating session
   * @param packet response packet
   * @return processed packet or {@code null} to stop processing
   */
  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    // No need to freeze the packet as any changes generated here are specifically for, and seen only by, the sender
    return onProcessing(sender, packet);
  }

  /**
   * Delivers a packet to subscribers and optionally across the cluster.
   *
   * @param sender originating session
   * @param packet packet to deliver
   * @param clustered {@code true} to forward through the backbone
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
   * Forwards a packet via the backbone.
   *
   * @param channel originating channel
   * @param packet packet to forward
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
