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
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaServer<V extends Value<V>> extends AbstractAttributed implements Server<V> {

  private final ExecutorService executorService;
  private final ConcurrentHashMap<String, OumuamuaSession<V>> sessionMap = new ConcurrentHashMap<>();
  private final HashMap<String, Protocol<V>> protocolMap = new HashMap<>();
  private final ConcurrentHashMap<Route, BayeuxService> serviceMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<ChannelInitializer<V>> initializerList = new ConcurrentLinkedQueue<>();
  private final OumuamuaConfiguration<V> configuration;
  private final ChannelTree<V> channelTree;
  private final String[] protocolNames;
  private final long sessionConnectionIntervalMilliseconds;

  private IdleChannelSifter<V> idleChannelSifter;
  private IdleSessionInspector<V> idleSessionInspector;

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
        for (BayeuxService service : configuration.getServices()) {
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

  public void start (ServletConfig servletConfig)
    throws ServletException {

    Backbone<V> backbone;

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

    new Thread(idleChannelSifter = new IdleChannelSifter<>(configuration.getIdleChannelCycleMinutes(), channelTree, this::onRemoved)).start();
    new Thread(idleSessionInspector = new IdleSessionInspector<>(this, configuration.getIdleSessionCycleMinutes())).start();
  }

  public void stop () {

    Backbone<V> backbone;

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
  }

  public ExecutorService getExecutorService () {

    return executorService;
  }

  private void onConnected (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onConnected(session);
      }
    }
  }

  private void onDisconnected (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onDisconnected(session);
      }
    }
  }

  private void onSubscribed (Channel<V> channel, Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener<V>)listener).onSubscribed(channel, session);
      }
    }
  }

  private void onUnsubscribed (Channel<V> channel, Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener<V>)listener).onUnsubscribed(channel, session);
      }
    }
  }

  private void onCreated (Channel<V> channel) {

    for (Listener<V> listener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(listener.getClass())) {
        ((ChannelListener<V>)listener).onCreated(channel);
      }
    }
  }

  private void onRemoved (Channel<V> channel) {

    for (Listener<V> listener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(listener.getClass())) {
        ((ChannelListener<V>)listener).onRemoved(channel);
      }
    }
  }

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

  @Override
  public void addService (BayeuxService service) {

    Route[] boundRoutes;

    if ((boundRoutes = service.getBoundRoutes()) != null) {
      for (Route boundRoute : boundRoutes) {
        serviceMap.put(boundRoute, service);
      }
    }
  }

  @Override
  public void removeService (Route route) {

    serviceMap.remove(route);
  }

  @Override
  public BayeuxService getService (Route route) {

    return serviceMap.get(route);
  }

  @Override
  public void addListener (Listener<V> listener) {

    listenerList.add(listener);
  }

  @Override
  public void removeListener (Listener<V> listener) {

    listenerList.remove(listener);
  }

  @Override
  public String getBayeuxVersion () {

    return "1.0";
  }

  @Override
  public String getMinimumBayeuxVersion () {

    return "1.0";
  }

  @Override
  public String[] getProtocolNames () {

    return protocolNames;
  }

  @Override
  public Protocol<V> getProtocol (String name) {

    return protocolMap.get(name);
  }

  @Override
  public Backbone<V> getBackbone () {

    return configuration.getBackbone();
  }

  @Override
  public SecurityPolicy<V> getSecurityPolicy () {

    return configuration.getSecurityPolicy();
  }

  @Override
  public Codec<V> getCodec () {

    return configuration.getCodec();
  }

  @Override
  public long getSessionConnectionIntervalMilliseconds () {

    return sessionConnectionIntervalMilliseconds;
  }

  @Override
  public boolean isReflective (Route route) {

    return configuration.isReflective(route);
  }

  @Override
  public boolean isStreaming (Route route) {

    return configuration.isStreaming(route);
  }

  public OumuamuaSession<V> createSession (Connection<V> connection) {

    return new OumuamuaSession<>(this::onConnected, this::onDisconnected, connection, configuration.getMaxLongPollQueueSize(), configuration.getSessionMaxIdleTimeoutSeconds() * 1000L);
  }

  public OumuamuaSession<V> getSession (String sessionId) {

    return sessionMap.get(sessionId);
  }

  public void addSession (OumuamuaSession<V> session) {

    sessionMap.put(session.getId(), session);
  }

  public void removeSession (OumuamuaSession<V> session) {

    OumuamuaSession<V> removedSession;

    if ((removedSession = sessionMap.remove(session.getId())) != null) {
      departChannels(removedSession);
    }
  }

  public void departChannels (OumuamuaSession<V> session) {

    channelTree.walk(new RemovedSessionOperation<>(session));
  }

  public Iterator<OumuamuaSession<V>> iterateSessions () {

    return sessionMap.values().iterator();
  }

  @Override
  public void addInitializer (ChannelInitializer<V> initializer) {

    initializerList.add(initializer);
  }

  @Override
  public void removeInitializer (ChannelInitializer<V> initializer) {

    initializerList.remove(initializer);
  }

  @Override
  public Channel<V> findChannel (String path)
    throws InvalidPathException {

    return channelTree.find(0, new DefaultRoute(path));
  }

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

  @Override
  public void removeChannel (Channel<V> channel)
    throws ChannelStateException {

    channelTree.removeChannelIfPresent(0, channel.getRoute(), this::onRemoved);
  }

  @Override
  public Packet<V> onRequest (Session<V> sender, Packet<V> packet) {

    // No need to freeze the packet as changes generated here should be by all further processing, including the response to the sender
    return onProcessing(sender, packet);
  }

  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    // No need to freeze the packet as any changes generated here are specifically for, and seen only by, the sender
    return onProcessing(sender, packet);
  }

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
