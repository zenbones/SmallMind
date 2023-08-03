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
package org.smallmind.cometd.oumuamua.transport;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.OumuamuaLocalSession;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.context.OumuamuaLocalContext;
import org.smallmind.cometd.oumuamua.extension.ExtensionNotifier;
import org.smallmind.cometd.oumuamua.logging.DataRecord;
import org.smallmind.cometd.oumuamua.logging.NodeRecord;
import org.smallmind.cometd.oumuamua.logging.PacketRecord;
import org.smallmind.cometd.oumuamua.message.NodeMessageGenerator;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class LocalCarrier implements OumuamuaCarrier {

  private static final OumuamuaLocalContext LOCAL_CONTEXT = new OumuamuaLocalContext();
  private static final String[] ACTUAL_TRANSPORTS = new String[] {"local"};
  private static final long DEFAULT_MAX_SESSION_IDLE_TIMEOUT = 300000;

  private final OumuamuaServer oumuamuaServer;
  private final LocalTransport localTransport;
  private final IdleCheck idleCheck;
  private OumuamuaServerSession serverSession;
  private boolean connected;
  private long lastContactMilliseconds;
  private long maxSessionIdleTimeout;

  public LocalCarrier (OumuamuaServer oumuamuaServer, LocalTransport localTransport, String idHint) {

    this.oumuamuaServer = oumuamuaServer;
    this.localTransport = localTransport;

    lastContactMilliseconds = System.currentTimeMillis();
    maxSessionIdleTimeout = (localTransport.getMaxInterval() > 0) ? localTransport.getMaxInterval() : DEFAULT_MAX_SESSION_IDLE_TIMEOUT;

    oumuamuaServer.addSession(serverSession = new OumuamuaServerSession(oumuamuaServer, localTransport, this, true, idHint, oumuamuaServer.getConfiguration().getMaximumMessageQueueSize(), oumuamuaServer.getConfiguration().getMaximumUndeliveredLazyMessageCount()));
    setConnected(true);

    new Thread(idleCheck = new IdleCheck()).start();
  }

  public OumuamuaLocalSession getLocalSession () {

    return (OumuamuaLocalSession)serverSession.getLocalSession();
  }

  @Override
  public BayeuxContext getContext () {

    return LOCAL_CONTEXT;
  }

  @Override
  public String[] getActualTransports () {

    return ACTUAL_TRANSPORTS;
  }

  @Override
  public String getUserAgent () {

    return null;
  }

  @Override
  public void setMaxSessionIdleTimeout (long maxSessionIdleTimeout) {

    long adjustedIdleTimeout = (maxSessionIdleTimeout >= 0) ? maxSessionIdleTimeout : localTransport.getMaxInterval();

    this.maxSessionIdleTimeout = (adjustedIdleTimeout >= 0) ? adjustedIdleTimeout : DEFAULT_MAX_SESSION_IDLE_TIMEOUT;
  }

  @Override
  public synchronized boolean isConnected () {

    return connected;
  }

  @Override
  public synchronized void setConnected (boolean connected) {

    this.connected = connected;
  }

  @Override
  public synchronized void send (OumuamuaPacket... packets)
    throws IOException {

    if (isConnected()) {

      String text;

      if ((text = asText(packets)) != null) {
        System.out.println("=>" + text);
        LoggerManager.getLogger(LocalCarrier.class).debug(new DataRecord(text, false));

        ((OumuamuaLocalSession)serverSession.getLocalSession()).receive(text);
      }
    }
  }

  @Override
  public synchronized OumuamuaPacket[] inject (ObjectNode messageNode)
    throws JsonProcessingException {

    System.out.println("<=" + JsonCodec.writeAsString(messageNode));
    LoggerManager.getLogger(LocalCarrier.class).debug(new NodeRecord(messageNode, true));

    try {

      String channel = messageNode.get(Message.CHANNEL_FIELD).asText();
      ChannelId channelId = ChannelIdCache.generate(channel);

      lastContactMilliseconds = System.currentTimeMillis();

      if (ExtensionNotifier.incoming(oumuamuaServer, serverSession, new NodeMessageGenerator(LOCAL_CONTEXT, localTransport, channelId, messageNode, false))) {

        OumuamuaPacket[] packets = respond(oumuamuaServer, LOCAL_CONTEXT, localTransport, serverSession, channelId, channelId.getId(), messageNode);

        if (!isConnected()) {
          close();
        }

        System.out.println(new PacketRecord(packets, false));
        LoggerManager.getLogger(LocalCarrier.class).debug(new PacketRecord(packets, false));

        return packets;
      } else {

        return createErrorPacket(serverSession, channelId, channel, messageNode, "Processing was denied");
      }
    } finally {
      // Keep our threads clean and tidy
      ChannelIdCache.clear();
    }
  }

  @Override
  public synchronized void close () {

    try {
      idleCheck.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(LocalCarrier.class).error(interruptedException);
    } finally {
      finishClosing();
    }
  }

  private synchronized void finishClosing () {

    if (serverSession != null) {
      oumuamuaServer.removeSession(serverSession);

      if (isConnected()) {
        oumuamuaServer.onSessionDisconnected(serverSession, null, true);
      }

      serverSession = null;
    }

    setConnected(false);
  }

  private class IdleCheck implements Runnable {

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
        while (!finishLatch.await(localTransport.getIdleCheckCycleMilliseconds(), TimeUnit.MILLISECONDS)) {
          if (lastContactMilliseconds > 0) {
            if (System.currentTimeMillis() > lastContactMilliseconds + maxSessionIdleTimeout) {
              finishLatch.countDown();
              finishClosing();
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
}
