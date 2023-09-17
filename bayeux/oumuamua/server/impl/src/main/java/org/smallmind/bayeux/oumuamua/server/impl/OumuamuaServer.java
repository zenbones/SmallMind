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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.common.api.json.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.OumuamuaException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaServer<V extends Value<V>> extends AbstractAttributed implements Server<V> {

  private final ConcurrentHashMap<String, OumuamuaSession<V>> sessionMap = new ConcurrentHashMap<>();
  private final HashMap<String, Protocol<V>> protocolMap = new HashMap<>();
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

      this.configuration = configuration;

      sessionConnectionIntervalMilliseconds = configuration.getSessionConnectIntervalSeconds() * 1000L;
      channelTree = new ChannelTree<>(configuration.getCodec());

      if (configuration.getProtocols() == null) {
        throw new OumuamuaException("No protocols have been defined");
      } else {
        for (Protocol<V> protocol : configuration.getProtocols()) {
          protocolMap.put(protocol.getName(), protocol);
        }

        protocolNames = protocolMap.keySet().toArray(new String[0]);
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

  private void onProcessing (Session<V> sender, Packet<V> packet) {

    for (Listener<V> listener : listenerList) {
      if (PacketListener.class.isAssignableFrom(listener.getClass())) {
        switch (packet.getPacketType()) {
          case REQUEST:
            ((PacketListener<V>)listener).onRequest(sender, packet);
            break;
          case RESPONSE:
            ((PacketListener<V>)listener).onResponse(sender, packet);
            break;
          case DELIVERY:
            ((PacketListener<V>)listener).onDelivery(sender, packet);
            break;
          default:
            throw new UnknownSwitchCaseException(packet.getPacketType().name());
        }
      }
    }
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
      channelTree.walk(new RemovedSessionOperation<>(removedSession));
    }
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
  public void onRequest (Session<V> sender, Packet<V> packet) {

    onProcessing(sender, packet);
  }

  @Override
  public void onResponse (Session<V> sender, Packet<V> packet) {

    onProcessing(sender, packet);
  }

  @Override
  public void deliver (Session<V> sender, Packet<V> packet, boolean clustered) {

    if (packet.getRoute() != null) {
      onProcessing(sender, packet);

      channelTree.deliver(sender, 0, packet, new HashSet<>());

      if (clustered) {

        Backbone<V> backbone;

        if ((backbone = getBackbone()) != null) {
          backbone.publish(packet);
        }
      }
    }
  }
}
