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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.spi.Advice;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.MetaProcessingException;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;
import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger
public class UnsubscribeMessage extends AdvisedMetaMessage {

  public static final DefaultRoute ROUTE;

  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String clientId;
  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String subscription;

  static {

    try {
      ROUTE = new DefaultRoute("/meta/unsubscribe");
    } catch (InvalidPathException invalidPathException) {
      throw new StaticInitializationError(invalidPathException);
    }
  }

  public <V extends Value<V>> Packet<V> process (Server<V> server, Session<V> session, SubscribeMessageRequestInView view)
    throws MetaProcessingException, InvalidPathException {

    ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

    if ((!session.getId().equals(getClientId())) || session.getState().lt(SessionState.HANDSHOOK)) {
      adviceNode.put(Advice.RECONNECT.getField(), "handshake");

      return new Packet<V>(PacketType.RESPONSE, getClientId(), ROUTE, toMessage(server.getCodec(), new UnsubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(ROUTE.getPath()).setId(getId()).setError("Handshake required").setSubscription(getSubscription()).setAdvice(adviceNode)));
    } else if (session.getState().lt(SessionState.CONNECTED)) {
      adviceNode.put(Advice.RECONNECT.getField(), "retry");

      return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, toMessage(server.getCodec(), new UnsubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(ROUTE.getPath()).setId(getId()).setError("Connection required").setSubscription(getSubscription()).setAdvice(adviceNode)));
    } else {

      if (getSubscription().startsWith("/meta/")) {

        return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, toMessage(server.getCodec(), new UnsubscribeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(ROUTE.getPath()).setId(getId()).setError("Attempted subscription to a meta channel").setSubscription(getSubscription()).setAdvice(adviceNode)));
      } else {

        Channel<V> channel;

        if ((channel = server.findChannel(getSubscription())) != null) {
          if (channel.unsubscribe(session)) {
            // TODO: needed???
          }
        }

        return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, toMessage(server.getCodec(), new UnsubscribeMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(ROUTE.getPath()).setClientId(session.getId()).setId(getId()).setSubscription(getSubscription())));
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
