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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.message.OumuamuaServerMessage;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.Pledge;
import org.smallmind.web.json.doppelganger.View;
import org.smallmind.web.json.scaffold.util.JsonCodec;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger(pledges = @Pledge(purposes = {"success", "error"}, visibility = OUT))
public class PublishMessage extends MetaMessage {

  @View(idioms = @Idiom(purposes = "request", visibility = IN))
  private JsonNode data;
  @View(idioms = @Idiom(purposes = "request", visibility = IN))
  private String clientId;

  public String process (OumuamuaServer oumuamuaServer, OumuamuaServerSession serverSession, OumuamuaServerMessage serverMessage)
    throws JsonProcessingException {

    if ((serverSession == null) || (!serverSession.getId().equals(getClientId()))) {

      return JsonCodec.writeAsString(new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Handshake required"));
    } else if (!serverSession.isHandshook()) {

      return JsonCodec.writeAsString(new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Handshake required"));
    } else if (!serverSession.isConnected()) {

      return JsonCodec.writeAsString(new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Connection required"));
    } else {

      ServerChannel serverChannel;

      if ((serverChannel = oumuamuaServer.findChannel(getChannel())) == null) {

        return JsonCodec.writeAsString(new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("No such channel"));
      } else {

        SecurityPolicy securityPolicy;

        if (((securityPolicy = oumuamuaServer.getSecurityPolicy()) != null) && (!securityPolicy.canPublish(oumuamuaServer, serverSession, serverChannel, serverMessage))) {
          return JsonCodec.writeAsString(new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Unauthorized"));
        } else {
          try {

            OumuamuaPacket deliveryPacket;

            oumuamuaServer.publishToChannel(getChannel(), deliveryPacket = new OumuamuaPacket(serverSession, new MapLike(null, (ObjectNode)JsonCodec.writeAsJsonNode(new DeliveryMessageSuccessOutView().setChannel(getChannel()).setId(getId()).setData(getData())))));

            if (serverSession.isBroadcastToPublisher()) {
              serverSession.send(deliveryPacket);
            }

            return JsonCodec.writeAsString(new PublishMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(getChannel()).setId(getId()));
          } catch (JsonProcessingException jsonProcessingException) {

            return JsonCodec.writeAsString(new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Not json formatted data"));
          }
        }
      }
    }
  }

  public String getClientId () {

    return clientId;
  }

  public void setClientId (String clientId) {

    this.clientId = clientId;
  }

  public JsonNode getData () {

    return data;
  }

  public void setData (JsonNode data) {

    this.data = data;
  }
}
