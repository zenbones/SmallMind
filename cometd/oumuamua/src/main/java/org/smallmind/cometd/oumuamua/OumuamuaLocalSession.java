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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.cometd.oumuamua.message.OumuamuaClientMessage;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.meta.UnsubscribeMessage;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class OumuamuaLocalSession implements LocalSession {

  private final ConcurrentHashMap<String, Object> attributeMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, OumuamuaClientSessionChannel> channelMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<ClientSession.Extension> extensionList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<ServerSession.ServerSessionListener> listenerList = new ConcurrentLinkedQueue<>();
  private final OumuamuaServerSession serverSession;

  public OumuamuaLocalSession (OumuamuaServerSession serverSession) {

    this.serverSession = serverSession;
  }

  @Override
  public ServerSession getServerSession () {

    return serverSession;
  }

  @Override
  public String getId () {

    return serverSession.getId();
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

  }

  @Override
  public void handshake (Map<String, Object> template, MessageListener callback) {

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

  private void dispatch (OumuamuaClientMessage message) {

  }

  @Override
  public void disconnect (MessageListener callback) {

  }

  @Override
  public void batch (Runnable batch) {

  }

  @Override
  public void startBatch () {

  }

  @Override
  public boolean endBatch () {

    return false;
  }

  protected MapLike inject (ObjectNode messageNode)
    throws JsonProcessingException {

    OumuamuaPacket[] packets;

    if (((packets = serverSession.getCarrier().inject(UnsubscribeMessage.CHANNEL_ID, messageNode)) != null) && (packets.length > 0)) {

      MapLike[] mapLikes;

      if (((mapLikes = packets[0].getMessages()) != null) && (mapLikes.length > 0)) {

        return mapLikes[0];
      }
    }

    return null;
  }
}
