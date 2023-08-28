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
package org.smallmind.bayeux.oumuamua.server.spi.meta;

import com.fasterxml.jackson.databind.JsonNode;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.MetaProcessingException;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.Pledge;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger(pledges = @Pledge(purposes = {"success", "error"}, visibility = OUT))
public class PublishMessage extends MetaMessage {

  @View(idioms = @Idiom(purposes = "request", visibility = IN))
  private JsonNode data;
  @View(idioms = @Idiom(purposes = "request", visibility = IN))
  private String clientId;

  public <V extends Value<V>> Packet<V> process (Server<V> server, Session<V> session, SubscribeMessageRequestInView view)
    throws MetaProcessingException {

    Route route;

    try {
      route = new DefaultRoute(getChannel());
    } catch (InvalidPathException invalidPathException) {
      return new Packet<V>(PacketType.RESPONSE, getClientId(), null, toMessage(server.getCodec(), new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError(invalidPathException.getMessage())));
    }

    if ((!session.getId().equals(getClientId())) || session.getState().lt(SessionState.HANDSHOOK)) {

      return new Packet<V>(PacketType.RESPONSE, getClientId(), route, toMessage(server.getCodec(), new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Handshake required")));
    } else if (session.getState().lt(SessionState.CONNECTED)) {

      return new Packet<V>(PacketType.RESPONSE, session.getId(), route, toMessage(server.getCodec(), new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Connection required")));
    } else if (getChannel().startsWith("/meta/")) {

      return new Packet<V>(PacketType.RESPONSE, session.getId(), route, toMessage(server.getCodec(), new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Attempted to publish to a meta channel")));
    } else {

      SecurityPolicy securityPolicy = server.getSecurityPolicy();
      Channel<V> channel;

      try {
        channel = server.findChannel(getChannel());
      } catch (InvalidPathException invalidPathException) {
        return new Packet<V>(PacketType.RESPONSE, getClientId(), null, toMessage(server.getCodec(), new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError(invalidPathException.getMessage())));
      }

      if ((securityPolicy != null) && (!securityPolicy.canPublish(session, channel, toMessage(server.getCodec(), view)))) {

        return new Packet<V>(PacketType.RESPONSE, session.getId(), route, toMessage(server.getCodec(), new PublishMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(getChannel()).setId(getId()).setError("Unauthorized")));
      } else if (channel.subscribe(session)) {
        // TODO: needed???
      }

      server.deliver(new Packet<V>(PacketType.DELIVERY, session.getId(), route, toMessage(server.getCodec(), new DeliveryMessageSuccessOutView().setChannel(getChannel()).setId(getId()).setData(getData()))));

      return new Packet<V>(PacketType.RESPONSE, session.getId(), route, toMessage(server.getCodec(), new PublishMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(getChannel()).setId(getId())));
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
