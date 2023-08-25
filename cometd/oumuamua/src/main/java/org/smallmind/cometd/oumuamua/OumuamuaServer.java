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
package org.smallmind.cometd.oumuamua;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.Transport;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.channel.ChannelIterator;
import org.smallmind.cometd.oumuamua.channel.ChannelOperation;
import org.smallmind.cometd.oumuamua.channel.ChannelTree;
import org.smallmind.cometd.oumuamua.channel.ExpirationOperation;
import org.smallmind.cometd.oumuamua.channel.ListOperation;
import org.smallmind.cometd.oumuamua.channel.UnsubscribeOperation;
import org.smallmind.cometd.oumuamua.message.OumuamuaLazyPacket;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaServer implements BayeuxServer {

  private final ReentrantLock channelChangeLock = new ReentrantLock();
  private final ReentrantLock sessionChangeLock = new ReentrantLock();
  private final OumuamuaConfiguration configuration;
  private final ChannelTree channelTree = new ChannelTree();
  private final ConcurrentHashMap<String, OumuamuaServerSession> sessionMap = new ConcurrentHashMap<>();
  private ExpiredChannelSifter expiredChannelSifter;
  private LazyMessageSifter lazyMessageSifter;
  private SecurityPolicy securityPolicy;

  public OumuamuaServer (OumuamuaConfiguration configuration) {

    this.configuration = configuration;
  }

  public OumuamuaConfiguration getConfiguration () {

    return configuration;
  }

  public void start (ServletConfig servletConfig)
    throws ServletException {

    for (OumuamuaTransport transport : configuration.getTransportMap().values()) {
      transport.init(this, servletConfig);
    }

    new Thread(expiredChannelSifter = new ExpiredChannelSifter()).start();
    new Thread(lazyMessageSifter = new LazyMessageSifter()).start();
  }

  public void stop () {

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

    //TODO: ?????
  }

  public String getProtocolVersion () {

    return "1.0";
  }

  @Override
  public Set<String> getKnownTransportNames () {

    return configuration.getTransportMap().keySet();
  }

  @Override
  public Transport getTransport (String transport) {

    return configuration.getTransportMap().get(transport);
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

    for (OumuamuaTransport transport : configuration.getTransportMap().values()) {
      if (qualifiedName.startsWith(transport.getOptionPrefix())) {

        return transport.getOption(qualifiedName.substring(transport.getOptionPrefix().length()));
      }
    }

    return null;
  }

  @Override
  public void setOption (String qualifiedName, Object value) {

    for (OumuamuaTransport transport : configuration.getTransportMap().values()) {
      if (qualifiedName.startsWith(transport.getOptionPrefix())) {

        transport.setOption(qualifiedName.substring(transport.getOptionPrefix().length()), value);
      }
    }
  }

  @Override
  public Set<String> getOptionNames () {

    HashSet<String> nameSet = new HashSet<>();

    for (OumuamuaTransport transport : configuration.getTransportMap().values()) {
      for (String name : transport.getOptionNames()) {
        nameSet.add(transport.getOptionPrefix() + name);
      }
    }

    return nameSet;
  }

  @Override
  public void addExtension (Extension extension) {

  }

  @Override
  public void removeExtension (Extension extension) {

  }

  @Override
  public List<Extension> getExtensions () {

    return null;
  }

  @Override
  public void addListener (BayeuxServerListener bayeuxServerListener) {

  }

  @Override
  public void removeListener (BayeuxServerListener bayeuxServerListener) {

  }

  @Override
  public ServerSession getSession (String id) {

    return sessionMap.get(id);
  }

  @Override
  public List<ServerSession> getSessions () {

    return new LinkedList<>(sessionMap.values());
  }

  public void addSession (OumuamuaServerSession serverSession) {

    sessionChangeLock.lock();

    try {
      sessionMap.put(serverSession.getId(), serverSession);
    } finally {
      sessionChangeLock.unlock();
    }
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

  @Override
  public LocalSession newLocalSession (String s) {

    return null;
  }

  @Override
  public ServerMessage.Mutable newMessage () {

    return null;
  }

  @Override
  public ServerChannel getChannel (String id) {

    return findChannel(id);
  }

  @Override
  public List<ServerChannel> getChannels () {

    ListOperation listOperation;

    channelTree.walk(listOperation = new ListOperation());

    return listOperation.getChannelList();
  }

  public OumuamuaServerChannel findChannel (String id) {

    ChannelTree channelBranch;

    return ((channelBranch = channelTree.find(0, ChannelIdCache.generate(id))) == null) ? null : channelBranch.getServerChannel();
  }

  @Override
  public MarkedReference<ServerChannel> createChannelIfAbsent (String id, ConfigurableServerChannel.Initializer... initializers) {

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

      return new MarkedReference<>(channelBranch.getServerChannel(), created);
    } finally {
      channelChangeLock.unlock();
    }
  }

  public void publishToChannel (String id, OumuamuaPacket packet) {

    channelTree.publish(new ChannelIterator(id), packet, new HashSet<>());
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

          for (OumuamuaServerSession serverSession : sessionMap.values()) {

            LinkedList<OumuamuaLazyPacket> enqueuedLazyPacketList = new LinkedList<>();
            OumuamuaLazyPacket[] enqueuedLazyPackets;

            while (((enqueuedLazyPackets = serverSession.pollLazy(now)) != null) && (enqueuedLazyPackets.length > 0)) {
              enqueuedLazyPacketList.addAll(Arrays.asList(enqueuedLazyPackets));
            }

            if (!enqueuedLazyPacketList.isEmpty()) {
              try {
                serverSession.getCarrier().send(serverSession, enqueuedLazyPacketList.toArray(new OumuamuaLazyPacket[0]));
              } catch (Exception exception) {
                LoggerManager.getLogger(OumuamuaServer.class).error(exception);
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
