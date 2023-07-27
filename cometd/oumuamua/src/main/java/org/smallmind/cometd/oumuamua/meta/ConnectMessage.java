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

import java.util.Arrays;
import java.util.LinkedList;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger
public class ConnectMessage extends AdvisedMetaMessage {

  public static final ChannelId CHANNEL_ID = new ChannelId("/meta/connect");

  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String clientId;
  @View(idioms = @Idiom(purposes = "request", visibility = IN))
  private String connectionType;

  public OumuamuaPacket[] process (OumuamuaTransport transport, OumuamuaServerSession serverSession) {

    //TODO: In theory there's a way to handle LongPollResponseDelayMilliseconds... but I'm not going to thread sleep, so...
    ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

    if ((serverSession == null) || (!serverSession.getId().equals(getClientId()))) {
      adviceNode.put("reconnect", "handshake");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new ConnectMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setError("Handshake required").setAdvice(adviceNode));
    } else if (!serverSession.isHandshook()) {
      adviceNode.put("reconnect", "handshake");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new ConnectMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setClientId(serverSession.getId()).setError("Handshake required").setAdvice(adviceNode));
    } else {
      for (String negotiatedTransport : serverSession.getNegotiatedTransports()) {
        if (negotiatedTransport.equalsIgnoreCase(connectionType)) {

          LinkedList<OumuamuaPacket> enqueuedPacketList = new LinkedList<>();
          OumuamuaPacket[] enqueuedPackets;
          long longPollingTimeout = (serverSession.getTimeout() >= 0) ? serverSession.getTimeout() : transport.getTimeout();

          serverSession.setConnected(true);
          if (longPollingTimeout > 0) {
            adviceNode.put("timeout", longPollingTimeout);
          }
          adviceNode.put("interval", (serverSession.getInterval() >= 0) ? serverSession.getInterval() : transport.getInterval());

          while (((enqueuedPackets = serverSession.poll()) != null) && (enqueuedPackets.length > 0)) {
            enqueuedPacketList.addAll(Arrays.asList(enqueuedPackets));
          }

          return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new ConnectMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(CHANNEL_ID.getId()).setId(getId()).setClientId(serverSession.getId()).setAdvice(adviceNode), enqueuedPacketList.toArray(new OumuamuaPacket[0]));
        }
      }

      adviceNode.put("reconnect", "retry");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new ConnectMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setClientId(serverSession.getId()).setError("The requested connection type does match one of the negotiated connection type").setAdvice(adviceNode));
    }
  }

  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  public String getConnectionType () {

    return connectionType;
  }

  public void setConnectionType (String connectionType) {

    this.connectionType = connectionType;
  }
}
