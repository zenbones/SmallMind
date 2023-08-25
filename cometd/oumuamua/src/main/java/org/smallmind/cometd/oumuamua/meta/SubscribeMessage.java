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
package org.smallmind.cometd.oumuamua.meta;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.SecurityPolicy;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.channel.AuthenticatorUtility;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.channel.ChannelNotice;
import org.smallmind.cometd.oumuamua.channel.OumuamuaServerChannel;
import org.smallmind.cometd.oumuamua.message.NodeMessageGenerator;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.session.OumuamuaServerSession;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger
public class SubscribeMessage extends AdvisedMetaMessage {

  public static final ChannelId CHANNEL_ID = new ChannelId("/meta/subscribe");

  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String clientId;
  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String subscription;

  public OumuamuaPacket[] process (OumuamuaServer oumuamuaServer, OumuamuaServerSession serverSession, ChannelNotice subscribeNotice, NodeMessageGenerator messageGenerator) {

    if ((serverSession == null) || (!serverSession.getId().equals(getClientId()))) {

      ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

      adviceNode.put("reconnect", "handshake");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setError("Handshake required").setSubscription(getSubscription()).setAdvice(adviceNode));
    } else if (!serverSession.isHandshook()) {

      ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

      adviceNode.put("reconnect", "handshake");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Handshake required").setSubscription(getSubscription()).setAdvice(adviceNode));
    } else if (!serverSession.isConnected()) {

      ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

      adviceNode.put("reconnect", "retry");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Connection required").setSubscription(getSubscription()).setAdvice(adviceNode));
    } else {

      if (getSubscription().startsWith("/meta/")) {

        return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Attempted subscription to a meta channel").setSubscription(getSubscription()));
      } else {

        SecurityPolicy securityPolicy = oumuamuaServer.getSecurityPolicy();
        OumuamuaServerChannel serverChannel;

        if ((serverChannel = oumuamuaServer.findChannel(getChannel())) == null) {
          if ((securityPolicy != null) && (!securityPolicy.canCreate(oumuamuaServer, serverSession, getChannel(), messageGenerator.generate()))) {

            return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Unauthorized").setSubscription(getSubscription()));
          } else if (!AuthenticatorUtility.canOperate(oumuamuaServer, serverSession, null, ChannelIdCache.generate(getChannel()), messageGenerator, Authorizer.Operation.CREATE)) {

            return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Unauthorized").setSubscription(getSubscription()));
          } else {
            serverChannel = (OumuamuaServerChannel)oumuamuaServer.createChannelIfAbsent(getSubscription()).getReference();
          }
        }

        if ((securityPolicy != null) && (!securityPolicy.canSubscribe(oumuamuaServer, serverSession, serverChannel, messageGenerator.generate()))) {

          return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Unauthorized").setSubscription(getSubscription()));
        } else if (!AuthenticatorUtility.canOperate(oumuamuaServer, serverSession, serverChannel, ChannelIdCache.generate(getChannel()), messageGenerator, Authorizer.Operation.SUBSCRIBE)) {

          return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setError("Unauthorized").setSubscription(getSubscription()));
        } else if (serverChannel.subscribe(serverSession, messageGenerator)) {
          subscribeNotice.setChannel(serverChannel);
        }

        return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new SubscribeMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(CHANNEL_ID.getId()).setClientId(serverSession.getId()).setId(getId()).setSubscription(getSubscription()));
      }
    }
  }

  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  public String getSubscription () {

    return subscription;
  }

  public void setSubscription (String subscription) {

    this.subscription = subscription;
  }
}
