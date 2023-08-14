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
package org.smallmind.bayeux.cometd.meta;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.server.SecurityPolicy;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.bayeux.cometd.message.NodeMessageGenerator;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.bayeux.cometd.session.OumuamuaServerSession;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger
public class HandshakeMessage extends AdvisedMetaMessage {

  public static final ChannelId CHANNEL_ID = new ChannelId("/meta/handshake");

  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String[] supportedConnectionTypes;
  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String version;
  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String minimumVersion;
  @View(idioms = @Idiom(purposes = "success", visibility = OUT))
  private String clientId;

  public OumuamuaPacket[] process (OumuamuaServer oumuamuaServer, String[] actualTransports, OumuamuaServerSession serverSession, NodeMessageGenerator messageGenerator) {

    SecurityPolicy securityPolicy;
    ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

    if (((securityPolicy = oumuamuaServer.getSecurityPolicy()) != null) && (!securityPolicy.canHandshake(oumuamuaServer, serverSession, messageGenerator.generate()))) {
      adviceNode.put("reconnect", "handshake");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new HandshakeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setVersion(oumuamuaServer.getProtocolVersion()).setMinimumVersion(oumuamuaServer.getProtocolVersion()).setError("Unauthorized").setSupportedConnectionTypes(oumuamuaServer.getAllowedTransports().toArray(new String[0])).setAdvice(adviceNode));
    } else if (serverSession.isHandshook()) {
      adviceNode.put("reconnect", "retry");

      return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new HandshakeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setVersion(oumuamuaServer.getProtocolVersion()).setMinimumVersion(oumuamuaServer.getProtocolVersion()).setError("Handshake was previously completed").setSupportedConnectionTypes(oumuamuaServer.getAllowedTransports().toArray(new String[0])).setAdvice(adviceNode));
    } else {
      try {

        String[] negotiatedTransports = TransportNegotiator.negotiate(actualTransports, oumuamuaServer.getAllowedTransports(), supportedConnectionTypes);

        serverSession.setNegotiatedTransports(negotiatedTransports);
        serverSession.setHandshook(true);

        return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new HandshakeMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(CHANNEL_ID.getId()).setId(getId()).setVersion(oumuamuaServer.getProtocolVersion()).setMinimumVersion(oumuamuaServer.getProtocolVersion()).setClientId(serverSession.getId()).setSupportedConnectionTypes(negotiatedTransports));
      } catch (TransportNegotiationFailure transportNegotiationFailure) {
        adviceNode.put("reconnect", "handshake");

        return OumuamuaPacket.asPackets(serverSession, CHANNEL_ID, new HandshakeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(CHANNEL_ID.getId()).setId(getId()).setVersion(oumuamuaServer.getProtocolVersion()).setMinimumVersion(oumuamuaServer.getProtocolVersion()).setError(transportNegotiationFailure.getMessage()).setSupportedConnectionTypes(oumuamuaServer.getAllowedTransports().toArray(new String[0])).setAdvice(adviceNode));
      }
    }
  }

  public String[] getSupportedConnectionTypes () {

    return supportedConnectionTypes;
  }

  public void setSupportedConnectionTypes (String[] supportedConnectionTypes) {

    this.supportedConnectionTypes = supportedConnectionTypes;
  }

  public String getVersion () {

    return version;
  }

  public void setVersion (String version) {

    this.version = version;
  }

  public String getMinimumVersion () {

    return minimumVersion;
  }

  public void setMinimumVersion (String minimumVersion) {

    this.minimumVersion = minimumVersion;
  }

  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }
}
