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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.client.ConnectionMonitor;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.cometd.oumuamua.message.OumuamuaClientMessage;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.meta.ConnectMessage;
import org.smallmind.cometd.oumuamua.meta.ConnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.DisconnectMessage;
import org.smallmind.cometd.oumuamua.meta.DisconnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.HandshakeMessage;
import org.smallmind.cometd.oumuamua.meta.HandshakeMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.UnsubscribeMessage;
import org.smallmind.cometd.oumuamua.transport.LocalTransport;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class OumuamuaLocalSession implements LocalSession {

  private static final ThreadLocal<LinkedList<OumuamuaPacket>> BATCHED_PACKET_LIST_LOCAL = new ThreadLocal<>();
  private final ReentrantLock connectLock = new ReentrantLock();
  private final ConcurrentHashMap<String, Object> attributeMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, OumuamuaClientSessionChannel> channelMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<ClientSession.Extension> extensionList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<ServerSession.ServerSessionListener> listenerList = new ConcurrentLinkedQueue<>();
  private final OumuamuaServerSession serverSession;
  private final ConnectionMonitor connectionMonitor;
  private final AtomicLong messageId = new AtomicLong(0);

  public OumuamuaLocalSession (OumuamuaServerSession serverSession) {

    this.serverSession = serverSession;

    connectionMonitor = new ConnectionMonitor((LocalTransport)serverSession.getServerTransport(), this);
  }

  @Override
  public ServerSession getServerSession () {

    return serverSession;
  }

  @Override
  public String getId () {

    return serverSession.getId();
  }

  public String nextMessageId () {

    return String.valueOf(messageId.incrementAndGet());
  }

  @Override
  public boolean isConnected () {

    return serverSession.isConnected();
  }

  @Override
  public boolean isHandshook () {

    return serverSession.isHandshook();
  }

  @Override
  public void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  @Override
  public Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  @Override
  public Set<String> getAttributeNames () {

    return attributeMap.keySet();
  }

  @Override
  public Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }

  public Iterator<ClientSession.Extension> iterateExtensions () {

    return extensionList.iterator();
  }

  @Override
  public List<ClientSession.Extension> getExtensions () {

    return new LinkedList<>(extensionList);
  }

  @Override
  public void addExtension (ClientSession.Extension extension) {

    extensionList.add(extension);
  }

  @Override
  public void removeExtension (ClientSession.Extension extension) {

    extensionList.remove(extension);
  }

  @Override
  public ClientSessionChannel getChannel (String channelName) {

    synchronized (channelMap) {

      OumuamuaClientSessionChannel channel;

      if ((channel = channelMap.get(channelName)) == null) {
        channelMap.put(channelName, channel = new OumuamuaClientSessionChannel(this, ChannelIdCache.generate(channelName)));
      }

      return channel;
    }
  }

  protected void releaseChannel (String channelName) {

    channelMap.remove(channelName);
  }

  @Override
  public void remoteCall (String target, Object data, MessageListener callback) {

    if (callback != null) {
      callback.onMessage(OumuamuaClientMessage.failed("No matching remote taerget"));
    }
  }

  @Override
  public void handshake (Map<String, Object> template, MessageListener callback) {

    try {

      HandshakeMessageRequestInView handshakeView = new HandshakeMessageRequestInView().setChannel(HandshakeMessage.CHANNEL_ID.getId()).setId(nextMessageId()).setSupportedConnectionTypes(new String[] {"local"});
      MapLike[] messages;

      if ((messages = inject((ObjectNode)JsonCodec.writeAsJsonNode(handshakeView))) != null) {
        if (callback != null) {
          callback.onMessage(new OumuamuaClientMessage(messages[0].flatten()));
        }

        if (Boolean.TRUE.equals(messages[0].flatten().get(Message.SUCCESSFUL_FIELD).asBoolean())) {
          connect(callback);
        }
      }
    } catch (JsonProcessingException jsonProcessingException) {
      LoggerManager.getLogger(OumuamuaClientSessionChannel.class).error(jsonProcessingException);

      if (callback != null) {
        callback.onMessage(OumuamuaClientMessage.failed(jsonProcessingException.getMessage()));
      }
    }
  }

  public void connect (MessageListener callback) {

    connectLock.lock();

    try {

      connectionMonitor.connecting();

      ConnectMessageRequestInView connectView = new ConnectMessageRequestInView().setChannel(ConnectMessage.CHANNEL_ID.getId()).setId(nextMessageId()).setClientId(serverSession.getId()).setConnectionType("local");
      MapLike[] messages;

      if ((messages = inject((ObjectNode)JsonCodec.writeAsJsonNode(connectView))) != null) {
        for (MapLike message : messages) {

          ObjectNode node = message.flatten();

          if (callback != null) {
            callback.onMessage(new OumuamuaClientMessage(node));
          }

          if (node.has(Message.CHANNEL_FIELD) && ConnectMessage.CHANNEL_ID.getId().equals(node.get(Message.CHANNEL_FIELD).asText())) {
            if (node.has(Message.SUCCESSFUL_FIELD) && node.get(Message.SUCCESSFUL_FIELD).asBoolean()) {
              if (node.has(Message.ADVICE_FIELD) && node.get(Message.ADVICE_FIELD).has(Message.INTERVAL_FIELD)) {
                connectionMonitor.start(node.get(Message.ADVICE_FIELD).get(Message.INTERVAL_FIELD).asLong());
              }
            }
          }
        }
      }
    } catch (JsonProcessingException jsonProcessingException) {
      LoggerManager.getLogger(OumuamuaClientSessionChannel.class).error(jsonProcessingException);

      if (callback != null) {
        callback.onMessage(OumuamuaClientMessage.failed(jsonProcessingException.getMessage()));
      }
    } finally {
      connectLock.unlock();
    }
  }

  @Override
  public void disconnect (MessageListener callback) {

    connectLock.lock();

    try {

      DisconnectMessageRequestInView disconnectView = new DisconnectMessageRequestInView().setChannel(DisconnectMessage.CHANNEL_ID.getId()).setId(nextMessageId()).setClientId(serverSession.getId());
      MapLike[] messages;

      if ((messages = inject((ObjectNode)JsonCodec.writeAsJsonNode(disconnectView))) != null) {

        ObjectNode node = messages[0].flatten();

        if (callback != null) {
          callback.onMessage(new OumuamuaClientMessage(node));
        }

        if (node.has(Message.SUCCESSFUL_FIELD) && node.get(Message.SUCCESSFUL_FIELD).asBoolean()) {
          connectionMonitor.stop();
        }
      }
    } catch (JsonProcessingException jsonProcessingException) {
      LoggerManager.getLogger(OumuamuaClientSessionChannel.class).error(jsonProcessingException);

      if (callback != null) {
        callback.onMessage(OumuamuaClientMessage.failed(jsonProcessingException.getMessage()));
      }
    } finally {
      connectLock.unlock();
    }
  }

  public void receive (String text) {

    try {

      JsonNode node;

      switch ((node = JsonCodec.readAsJsonNode(text)).getNodeType()) {
        case OBJECT:
          dispatch(new OumuamuaClientMessage((ObjectNode)node));
          break;
        case ARRAY:
          for (JsonNode item : node) {
            if (JsonNodeType.OBJECT.equals(item.getNodeType())) {
              dispatch(new OumuamuaClientMessage((ObjectNode)item));
            }
          }
          break;
      }
    } catch (JsonProcessingException jsonProcessingException) {
      LoggerManager.getLogger(OumuamuaLocalSession.class).error(jsonProcessingException);
    }
  }

  public void dispatch (OumuamuaClientMessage message) {

    if (message.flatten().has(Message.CHANNEL_FIELD)) {

      OumuamuaClientSessionChannel channel;

      if ((channel = channelMap.get(message.flatten().get(Message.CHANNEL_FIELD).asText())) != null) {
        channel.receive(message);
      }
    }
  }

  @Override
  public void batch (Runnable batch) {

    new Thread(() -> {
      startBatch();

      try {
        batch.run();
      } finally {
        endBatch();
      }
    }).start();
  }

  @Override
  public synchronized void startBatch () {

    if (BATCHED_PACKET_LIST_LOCAL.get() == null) {
      BATCHED_PACKET_LIST_LOCAL.set(new LinkedList<>());
    }
  }

  @Override
  public synchronized boolean endBatch () {

    LinkedList<OumuamuaPacket> batchedPacketList = BATCHED_PACKET_LIST_LOCAL.get();

    if (batchedPacketList != null) {
      BATCHED_PACKET_LIST_LOCAL.remove();

      if (!batchedPacketList.isEmpty()) {
        for (OumuamuaPacket batchedPacket : batchedPacketList) {
          for (MapLike mapLike : batchedPacket.getMessages()) {
            try {
              inject(mapLike.flatten());
            } catch (JsonProcessingException jsonProcessingException) {
              LoggerManager.getLogger(OumuamuaLocalSession.class).error(jsonProcessingException);
            }
          }
        }

        return true;
      }
    }

    return false;
  }

  protected MapLike[] inject (ObjectNode messageNode)
    throws JsonProcessingException {

    OumuamuaPacket[] packets;

    if (((packets = serverSession.getCarrier().inject(UnsubscribeMessage.CHANNEL_ID, messageNode)) != null) && (packets.length > 0)) {

      LinkedList<MapLike> messages = new LinkedList<>();

      for (OumuamuaPacket packet : packets) {
        messages.addAll(Arrays.asList(packet.getMessages()));
      }

      if (!messages.isEmpty()) {

        return messages.toArray(new MapLike[0]);
      }
    }

    return null;
  }
}
