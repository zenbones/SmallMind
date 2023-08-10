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
package org.smallmind.bayeux.cometd;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.Transport;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.bayeux.cometd.backbone.PacketCodec;
import org.smallmind.bayeux.cometd.backbone.ServerBackbone;
import org.smallmind.bayeux.cometd.channel.ChannelIdCache;
import org.smallmind.bayeux.cometd.channel.ChannelIterator;
import org.smallmind.bayeux.cometd.channel.ChannelOperation;
import org.smallmind.bayeux.cometd.channel.ChannelTree;
import org.smallmind.bayeux.cometd.channel.ExpirationOperation;
import org.smallmind.bayeux.cometd.channel.ListChannelsOperation;
import org.smallmind.bayeux.cometd.channel.ListSubscriptionsOperation;
import org.smallmind.bayeux.cometd.channel.OumuamuaServerChannel;
import org.smallmind.bayeux.cometd.channel.UnsubscribeOperation;
import org.smallmind.bayeux.cometd.message.MapLike;
import org.smallmind.bayeux.cometd.message.NodeMessageGenerator;
import org.smallmind.bayeux.cometd.message.OumuamuaLazyPacket;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.bayeux.cometd.message.OumuamuaServerMessage;
import org.smallmind.bayeux.cometd.session.OumuamuaServerSession;
import org.smallmind.bayeux.cometd.session.VeridicalServerSession;
import org.smallmind.bayeux.cometd.transport.LocalTransport;
import org.smallmind.bayeux.cometd.transport.OumuamuaTransport;
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaServer implements BayeuxServer {

  private final ReentrantLock channelChangeLock = new ReentrantLock();
  private final ReentrantLock sessionChangeLock = new ReentrantLock();
  private final ServerBackbone serverBackbone;
  private final OumuamuaConfiguration configuration;
  private final ChannelTree channelTree = new ChannelTree();
  private final HashMap<String, OumuamuaTransport> transportMap = new HashMap<>();
  private final ConcurrentHashMap<String, VeridicalServerSession> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Extension> extensionList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<BayeuxServerListener> listenerList = new ConcurrentLinkedQueue<>();
  private ExpiredChannelSifter expiredChannelSifter;
  private LazyMessageSifter lazyMessageSifter;
  private SecurityPolicy securityPolicy;

  public OumuamuaServer (ServerBackbone serverBackbone, OumuamuaConfiguration configuration) {

    this.serverBackbone = serverBackbone;
    this.configuration = configuration;
  }

  public OumuamuaConfiguration getConfiguration () {

    return configuration;
  }

  public void start (ServletConfig servletConfig)
    throws ServletException {

    try {
      serverBackbone.startUp(this);
    } catch (Exception exception) {
      throw new ServletException(exception);
    }

    for (OumuamuaTransport transport : configuration.getTransports()) {
      transportMap.put(transport.getName(), transport);
      transport.init(this, servletConfig);
    }

    new Thread(expiredChannelSifter = new ExpiredChannelSifter()).start();
    new Thread(lazyMessageSifter = new LazyMessageSifter()).start();
  }

  public void stop () {

    try {
      serverBackbone.shutDown();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    }
    try {
      lazyMessageSifter.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    }
    try {
      expiredChannelSifter.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    }
  }

  public String getProtocolVersion () {

    return "1.0";
  }

  @Override
  public Set<String> getKnownTransportNames () {

    return transportMap.keySet();
  }

  @Override
  public Transport getTransport (String transport) {

    return transportMap.get(transport);
  }

  @Override
  public List<String> getAllowedTransports () {

    return Arrays.asList(configuration.getAllowedTransports());
  }

  @Override
  public SecurityPolicy getSecurityPolicy () {

    return securityPolicy;
  }

  @Override
  public void setSecurityPolicy (SecurityPolicy securityPolicy) {

    this.securityPolicy = securityPolicy;
  }

  @Override
  public Object getOption (String qualifiedName) {

    for (OumuamuaTransport transport : transportMap.values()) {
      if (qualifiedName.startsWith(transport.getOptionPrefix())) {

        return transport.getOption(qualifiedName.substring(transport.getOptionPrefix().length()));
      }
    }

    return null;
  }

  @Override
  public void setOption (String qualifiedName, Object value) {

    for (OumuamuaTransport transport : transportMap.values()) {
      if (qualifiedName.startsWith(transport.getOptionPrefix())) {

        transport.setOption(qualifiedName.substring(transport.getOptionPrefix().length()), value);
      }
    }
  }

  @Override
  public Set<String> getOptionNames () {

    HashSet<String> nameSet = new HashSet<>();

    for (OumuamuaTransport transport : transportMap.values()) {
      for (String name : transport.getOptionNames()) {
        nameSet.add(transport.getOptionPrefix() + name);
      }
    }

    return nameSet;
  }

  public Iterator<Extension> iterateExtensions () {

    return extensionList.iterator();
  }

  @Override
  public List<Extension> getExtensions () {

    return new LinkedList<>(extensionList);
  }

  @Override
  public void addExtension (Extension extension) {

    extensionList.add(extension);
  }

  @Override
  public void removeExtension (Extension extension) {

    extensionList.remove(extension);
  }

  @Override
  public void addListener (BayeuxServerListener bayeuxServerListener) {

    listenerList.add(bayeuxServerListener);
  }

  @Override
  public void removeListener (BayeuxServerListener bayeuxServerListener) {

    listenerList.remove(bayeuxServerListener);
  }

  @Override
  public ServerSession getSession (String id) {

    return sessionMap.get(id);
  }

  @Override
  public List<ServerSession> getSessions () {

    return new LinkedList<>(sessionMap.values());
  }

  public void addSession (VeridicalServerSession serverSession) {

    sessionChangeLock.lock();

    try {
      sessionMap.put(serverSession.getId(), serverSession);
    } finally {
      sessionChangeLock.unlock();
    }
  }

  public void onSessionConnected (ServerSession serverSession, NodeMessageGenerator messageGenerator) {

    for (BayeuxListener bayeuxListener : listenerList) {
      if (SessionListener.class.isAssignableFrom(bayeuxListener.getClass())) {
        ((SessionListener)bayeuxListener).sessionAdded(serverSession, messageGenerator.generate());
      }
    }

    ((VeridicalServerSession)serverSession).onConnected(messageGenerator);
  }

  @Override
  public boolean removeSession (ServerSession serverSession) {

    sessionChangeLock.lock();

    try {
      operateOnChannels(new UnsubscribeOperation((OumuamuaServerSession)serverSession));

      return sessionMap.remove(serverSession.getId()) != null;
    } finally {
      sessionChangeLock.unlock();
    }
  }

  public void onSessionDisconnected (ServerSession serverSession, NodeMessageGenerator messageGenerator, boolean timeout) {

    for (BayeuxListener bayeuxListener : listenerList) {
      if (SessionListener.class.isAssignableFrom(bayeuxListener.getClass())) {
        ((SessionListener)bayeuxListener).sessionRemoved(serverSession, (messageGenerator == null) ? null : messageGenerator.generate(), timeout);
      }
    }

    ((VeridicalServerSession)serverSession).onDisconnected(messageGenerator, timeout);
  }

  @Override
  public LocalSession newLocalSession (String idHint) {

    LocalTransport localTransport;

    if ((localTransport = (LocalTransport)transportMap.get("local")) == null) {
      throw new UnsupportedOperationException("No local transport has been defined in the server configuration");
    } else {

      return localTransport.createCarrier(idHint).getLocalSession();
    }
  }

  public void onChannelSubscribed (ServerSession serverSession, ServerChannel serverChannel, NodeMessageGenerator messageGenerator) {

    for (BayeuxListener bayeuxListener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(bayeuxListener.getClass())) {
        ((SubscriptionListener)bayeuxListener).subscribed(serverSession, serverChannel, messageGenerator.generate());
      }
    }
  }

  public void onChannelUnsubscribed (ServerSession serverSession, ServerChannel serverChannel, NodeMessageGenerator messageGenerator) {

    for (BayeuxListener bayeuxListener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(bayeuxListener.getClass())) {
        ((SubscriptionListener)bayeuxListener).unsubscribed(serverSession, serverChannel, messageGenerator.generate());
      }
    }
  }

  @Override
  public ServerMessage.Mutable newMessage () {

    return new OumuamuaServerMessage(null, null, null, null, false, new MapLike(JsonNodeFactory.instance.objectNode()));
  }

  @Override
  public ServerChannel getChannel (String id) {

    return findChannel(id);
  }

  @Override
  public List<ServerChannel> getChannels () {

    ListChannelsOperation listChannelsOperation;

    channelTree.walk(listChannelsOperation = new ListChannelsOperation());

    return listChannelsOperation.getChannelList();
  }

  public Set<ServerChannel> getSubscriptions (OumuamuaServerSession serverSession) {

    ListSubscriptionsOperation listSubscriptionsOperation;

    channelTree.walk(listSubscriptionsOperation = new ListSubscriptionsOperation(serverSession.getId()));

    return listSubscriptionsOperation.getChannelSet();
  }

  public OumuamuaServerChannel findChannel (String id) {

    ChannelTree channelBranch;

    return ((channelBranch = channelTree.find(0, ChannelIdCache.generate(id))) == null) ? null : channelBranch.getServerChannel();
  }

  @Override
  public MarkedReference<ServerChannel> createChannelIfAbsent (String id, ConfigurableServerChannel.Initializer... initializers) {

    MarkedReference<ServerChannel> serverChannelRef;

    // Addition and removal of channels should be done only via createChannelIfAbsent(), removeChannel() and ExpirationOperation.operate()
    channelChangeLock.lock();

    try {

      ChannelTree channelBranch = channelTree.createIfAbsent(this, 0, ChannelIdCache.generate(id));
      boolean created = false;

      if (!channelBranch.getServerChannel().isInitialized()) {
        created = true;

        if (initializers != null) {
          for (ConfigurableServerChannel.Initializer initializer : initializers) {
            initializer.configureChannel(channelBranch.getServerChannel());
          }
        }

        channelBranch.getServerChannel().setInitialized(true);
      }

      serverChannelRef = new MarkedReference<>(channelBranch.getServerChannel(), created);
    } finally {
      channelChangeLock.unlock();
    }

    if (serverChannelRef.isMarked()) {
      for (BayeuxListener bayeuxListener : listenerList) {
        if (ChannelListener.class.isAssignableFrom(bayeuxListener.getClass())) {
          ((ChannelListener)bayeuxListener).channelAdded(serverChannelRef.getReference());
        }
      }
    }

    return serverChannelRef;
  }

  public void remotePublish (OumuamuaPacket packet) {

    if (serverBackbone != null) {
      try {
        serverBackbone.publish(PacketCodec.encode(packet.getSender(), packet));
      } catch (IOException ioException) {
        LoggerManager.getLogger(OumuamuaServer.class).error(ioException);
      }
    }
  }

  public void publishToChannel (OumuamuaTransport transport, String channel, OumuamuaPacket packet) {

    channelTree.publish(transport, new ChannelIterator(channel), packet, new HashSet<>());
  }

  public void operateOnChannels (ChannelOperation operation) {

    channelTree.walk(operation);
  }

  public void removeChannel (ServerChannel channel) {

    // Addition and removal of channels should be done only via createChannelIfAbsent(), removeChannel() and ExpirationOperation.operate()
    channelChangeLock.lock();

    try {
      channelTree.removeIfPresent(0, channel.getChannelId());
    } finally {
      channelChangeLock.unlock();
    }

    for (BayeuxListener bayeuxListener : listenerList) {
      if (ChannelListener.class.isAssignableFrom(bayeuxListener.getClass())) {
        ((ChannelListener)bayeuxListener).channelRemoved(channel.getId());
      }
    }
  }

  private class LazyMessageSifter implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!finishLatch.await(configuration.getLazyMessageCycleMilliseconds(), TimeUnit.MILLISECONDS)) {

          long now = System.currentTimeMillis();

          for (VeridicalServerSession serverSession : sessionMap.values()) {
            if (serverSession.isConnected()) {

              LinkedList<OumuamuaLazyPacket> enqueuedLazyPacketList = new LinkedList<>();
              OumuamuaLazyPacket[] enqueuedLazyPackets;

              while (((enqueuedLazyPackets = serverSession.pollLazy(now)) != null) && (enqueuedLazyPackets.length > 0)) {
                enqueuedLazyPacketList.addAll(Arrays.asList(enqueuedLazyPackets));
              }

              if (!enqueuedLazyPacketList.isEmpty()) {
                try {
                  serverSession.getCarrier().send(enqueuedLazyPacketList.toArray(new OumuamuaLazyPacket[0]));
                } catch (Exception exception) {
                  LoggerManager.getLogger(OumuamuaServer.class).error(exception);
                }
              }
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
      } finally {
        exitLatch.countDown();
      }
    }
  }

  private class ExpiredChannelSifter implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!finishLatch.await(configuration.getExpiredChannelCycleMinutes(), TimeUnit.MINUTES)) {
          channelTree.walk(new ExpirationOperation(channelChangeLock, System.currentTimeMillis()));

          channelChangeLock.lock();
          try {
            channelTree.trim(null);
          } finally {
            channelChangeLock.unlock();
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
      } finally {
        exitLatch.countDown();
      }
    }
  }
}
