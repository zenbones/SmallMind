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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelNotice;
import org.smallmind.cometd.oumuamua.extension.ExtensionNotifier;
import org.smallmind.cometd.oumuamua.message.ExtMapMessageGenerator;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.cometd.oumuamua.message.NodeMessageGenerator;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.message.PacketType;
import org.smallmind.cometd.oumuamua.meta.ConnectMessage;
import org.smallmind.cometd.oumuamua.meta.ConnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.DisconnectMessage;
import org.smallmind.cometd.oumuamua.meta.DisconnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.HandshakeMessage;
import org.smallmind.cometd.oumuamua.meta.HandshakeMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.PublishMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.SubscribeMessage;
import org.smallmind.cometd.oumuamua.meta.SubscribeMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.UnsubscribeMessage;
import org.smallmind.cometd.oumuamua.meta.UnsubscribeMessageRequestInView;
import org.smallmind.nutsnbolts.util.Switch;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public interface OumuamuaCarrier {

  String[] getActualSessions ();

  String getUserAgent ();

  void setMaxSessionIdleTimeout (long maxSessionIdleTimeout);

  boolean isConnected ();

  void setConnected (boolean connected);

  void send (OumuamuaPacket... packets)
    throws Exception;

  OumuamuaPacket[] inject (ObjectNode messageNode)
    throws JsonProcessingException;

  void close ()
    throws IOException;

  default OumuamuaPacket[] respond (OumuamuaServer oumuamuaServer, BayeuxContext context, OumuamuaTransport transport, OumuamuaServerSession serverSession, ChannelId channelId, String channel, ObjectNode messageNode)
    throws JsonProcessingException {

    switch (channel) {
      case "/meta/handshake":

        return JsonCodec.read(messageNode, HandshakeMessageRequestInView.class).factory().process(oumuamuaServer, getActualSessions(), serverSession, new NodeMessageGenerator(context, transport, HandshakeMessage.CHANNEL_ID, messageNode, false));
      case "/meta/connect":

        Switch connectSwitch;
        OumuamuaPacket[] connectResponse = JsonCodec.read(messageNode, ConnectMessageRequestInView.class).factory().process(transport, serverSession, connectSwitch = new Switch());

        if (connectSwitch.isOn()) {
          oumuamuaServer.onSessionConnected(serverSession, new NodeMessageGenerator(context, transport, ConnectMessage.CHANNEL_ID, messageNode, false));
        }

        return connectResponse;
      case "/meta/disconnect":

        Switch disconnectSwitch;
        OumuamuaPacket[] disconnectResponse = JsonCodec.read(messageNode, DisconnectMessageRequestInView.class).factory().process(serverSession, disconnectSwitch = new Switch());

        if (disconnectSwitch.isOn()) {
          // disconnect will happen after the response hs been sent
          setConnected(false);
          oumuamuaServer.onSessionDisconnected(serverSession, new NodeMessageGenerator(context, transport, DisconnectMessage.CHANNEL_ID, messageNode, false), false);
        }

        return disconnectResponse;
      case "/meta/subscribe":

        ChannelNotice subscribeNotice;
        NodeMessageGenerator subscribeMessageGenerator;
        OumuamuaPacket[] subscribeResponse = JsonCodec.read(messageNode, SubscribeMessageRequestInView.class).factory().process(oumuamuaServer, serverSession, subscribeNotice = new ChannelNotice(), subscribeMessageGenerator = new NodeMessageGenerator(context, transport, SubscribeMessage.CHANNEL_ID, messageNode, false));

        if (subscribeNotice.isOn()) {
          oumuamuaServer.onChannelSubscribed(serverSession, subscribeNotice.getChannel(), subscribeMessageGenerator);
        }

        return subscribeResponse;
      case "/meta/unsubscribe":

        ChannelNotice unsubscribeNotice;
        OumuamuaPacket[] unsubscribeResponse = JsonCodec.read(messageNode, UnsubscribeMessageRequestInView.class).factory().process(oumuamuaServer, serverSession, unsubscribeNotice = new ChannelNotice());

        if (unsubscribeNotice.isOn()) {
          oumuamuaServer.onChannelUnsubscribed(serverSession, unsubscribeNotice.getChannel(), new NodeMessageGenerator(context, transport, UnsubscribeMessage.CHANNEL_ID, messageNode, false));
        }

        return unsubscribeResponse;
      default:
        if (channel.startsWith("/meta/")) {

          return createErrorPacket(serverSession, channelId, channel, messageNode, "Unknown meta channel");
        } else if (channel.endsWith("/*") || channel.endsWith("/**")) {

          return createErrorPacket(serverSession, channelId, channel, messageNode, "Attempt to publish to a wildcard channel");
        } else if (channel.startsWith("/service/")) {
          return null;
        } else {

          return JsonCodec.read(messageNode, PublishMessageRequestInView.class).factory().process(oumuamuaServer, context, transport, channelId, serverSession, messageNode);
        }
    }
  }

  default String asText (OumuamuaServer oumuamuaServer, BayeuxContext context, OumuamuaTransport transport, OumuamuaServerSession serverSession, OumuamuaPacket... packets)
    throws JsonProcessingException {

    StringBuilder sendBuilder = null;

    for (OumuamuaPacket packet : packets) {
      for (MapLike mapLike : packet.getMessages()) {
        if (ExtensionNotifier.outgoing(oumuamuaServer, packet.getSender(), serverSession, new ExtMapMessageGenerator(context, transport, packet.getChannelId(), mapLike, PacketType.LAZY.equals(packet.getType())))) {
          if (sendBuilder == null) {
            sendBuilder = new StringBuilder("[");
          } else {
            sendBuilder.append(',');
          }

          sendBuilder.append(mapLike.encode());
        }
      }
    }

    return (sendBuilder == null) ? null : sendBuilder.append(']').toString();
  }

  default OumuamuaPacket[] createErrorPacket (OumuamuaServerSession serverSession, ChannelId channelId, String channel, JsonNode messageNode, String error) {

    ObjectNode errorNode = JsonNodeFactory.instance.objectNode();

    errorNode.put(Message.SUCCESSFUL_FIELD, false);
    errorNode.put(Message.CHANNEL_FIELD, channel);
    errorNode.put(Message.ERROR_FIELD, error);

    if (messageNode.has(Message.ID_FIELD)) {
      errorNode.set(Message.ID_FIELD, messageNode.get(Message.ID_FIELD));
    }
    if (serverSession != null) {
      errorNode.put(Message.CLIENT_ID_FIELD, serverSession.getId());
    }

    return OumuamuaPacket.asPackets(serverSession, channelId, errorNode);
  }
}
