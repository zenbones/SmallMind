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
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
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
public class HandshakeMessage extends AdvisedMetaMessage {

  public static final DefaultRoute ROUTE;

  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String[] supportedConnectionTypes;
  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String version;
  @View(idioms = {@Idiom(purposes = "request", visibility = IN), @Idiom(purposes = {"success", "error"}, visibility = OUT)})
  private String minimumVersion;
  @View(idioms = @Idiom(purposes = "success", visibility = OUT))
  private String clientId;

  static {

    try {
      ROUTE = new DefaultRoute("/meta/handshake");
    } catch (InvalidPathException invalidPathException) {
      throw new StaticInitializationError(invalidPathException);
    }
  }

  public <V extends Value<V>> Packet<V> process (Protocol protocol, Server<V> server, Session<V> session, HandshakeMessageRequestInView view)
    throws MetaProcessingException {

    ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();
    SecurityPolicy securityPolicy;

    if (((securityPolicy = server.getSecurityPolicy()) != null) && (!securityPolicy.canHandshake(session, toMessage(server.getCodec(), view)))) {

      return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, new Message[] {toMessage(server.getCodec(), new HandshakeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(ROUTE.getPath()).setId(getId()).setVersion(server.getBayeuxVersion()).setMinimumVersion(server.getMinimumBayeuxVersion()).setError("Unauthorized").setSupportedConnectionTypes(TransportUtility.accumulateSupportedTransportNames(server)).setAdvice(adviceNode))});
    } else if (session.getState().gte(SessionState.HANDSHOOK)) {
      adviceNode.put(Advice.RECONNECT.getField(), "retry");

      return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, new Message[] {toMessage(server.getCodec(), new HandshakeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(ROUTE.getPath()).setId(getId()).setVersion(server.getBayeuxVersion()).setMinimumVersion(server.getMinimumBayeuxVersion()).setError("Handshake was previously completed").setSupportedConnectionTypes(TransportUtility.accumulateSupportedTransportNames(server)).setAdvice(adviceNode))});
    } else if (!supportsConnectionType(protocol)) {
      adviceNode.put(Advice.RECONNECT.getField(), "handshake");

      return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, new Message[] {toMessage(server.getCodec(), new HandshakeMessageErrorOutView().setSuccessful(Boolean.FALSE).setChannel(ROUTE.getPath()).setId(getId()).setVersion(server.getBayeuxVersion()).setMinimumVersion(server.getMinimumBayeuxVersion()).setError("Handshake attempted on an unsupported transport").setSupportedConnectionTypes(TransportUtility.accumulateSupportedTransportNames(server)).setAdvice(adviceNode))});
    } else {
      session.completeHandshake();

      return new Packet<V>(PacketType.RESPONSE, session.getId(), ROUTE, new Message[] {toMessage(server.getCodec(), new HandshakeMessageSuccessOutView().setSuccessful(Boolean.TRUE).setChannel(ROUTE.getPath()).setId(getId()).setVersion(server.getBayeuxVersion()).setMinimumVersion(server.getMinimumBayeuxVersion()).setClientId(session.getId()).setSupportedConnectionTypes(protocol.getTransportNames()))});
    }
  }

  private boolean supportsConnectionType (Protocol protocol) {

    String[] supportedTransportNames;

    if (((supportedTransportNames = protocol.getTransportNames()) != null) && (supportedTransportNames.length > 0)) {
      if (getSupportedConnectionTypes() != null) {
        for (String supportedConnectionType : getSupportedConnectionTypes()) {
          if (supportedConnectionType != null) {
            for (String supportedTransportName : supportedTransportNames) {
              if (supportedConnectionType.equals(supportedTransportName)) {

                return true;
              }
            }
          }
        }
      }
    }

    return false;
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
