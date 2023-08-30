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

import java.util.Arrays;
import java.util.LinkedList;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.MetaProcessingException;
import org.smallmind.nutsnbolts.util.MutationUtility;

public enum Meta {

  HANDSHAKE(DefaultRoute.HANDSHAKE_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request) {

      SecurityPolicy securityPolicy;

      if (((securityPolicy = server.getSecurityPolicy()) != null) && (!securityPolicy.canHandshake(session, request))) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructHandshakeErrorResponse(server, getRoute().getPath(), request.getId(), session.getId(), "Unauthorized", Reconnect.NONE));
      } else if (session.getState().gte(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructHandshakeErrorResponse(server, getRoute().getPath(), request.getId(), session.getId(), "Handshake was previously completed", Reconnect.RETRY));
      } else if (!supportsConnectionType(protocol, request)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructHandshakeErrorResponse(server, getRoute().getPath(), request.getId(), session.getId(), "Handshake attempted on an unsupported transport", Reconnect.HANDSHAKE));
      } else {
        session.completeHandshake();

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructHandshakeSuccessResponse(protocol, server, getRoute().getPath(), request.getId(), session.getId()));
      }
    }

    private <V extends Value<V>> Message<V> constructHandshakeSuccessResponse (Protocol<V> protocol, Server<V> server, String path, String id, String sessionId) {

      Message<V> response;

      return (Message<V>)(response = constructResponse(server, path, id, sessionId)).put(Message.SUCCESSFUL, true).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(protocol.getTransportNames(), text -> response.getFactory().textValue(text))));
    }

    private <V extends Value<V>> Message<V> constructHandshakeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      Message<V> response;

      return (Message<V>)(response = constructResponse(server, path, id, sessionId)).put(Message.SUCCESSFUL, false).put(Message.ERROR, error).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(TransportUtility.accumulateSupportedTransportNames(server), text -> response.getFactory().textValue(text)))).put(Message.ADVICE, response.getFactory().objectValue().put(Advice.RECONNECT.getField(), reconnect.getCode()));
    }

    private <V extends Value<V>> boolean supportsConnectionType (Protocol<V> protocol, Message<V> request) {

      String[] supportedTransportNames;

      if (((supportedTransportNames = protocol.getTransportNames()) != null) && (supportedTransportNames.length > 0)) {

        Value<V> supportedConnectionTypesValue;

        if (((supportedConnectionTypesValue = request.get(Message.SUPPORTED_CONNECTION_TYPES)) != null) && ValueType.ARRAY.equals(supportedConnectionTypesValue.getType())) {
          for (int supportedTypeIndex = 0; supportedTypeIndex < ((ArrayValue<V>)supportedConnectionTypesValue).size(); supportedTypeIndex++) {

            Value<V> supportedConnectionTypeValue;

            if (((supportedConnectionTypeValue = ((ArrayValue<V>)supportedConnectionTypesValue).get(supportedTypeIndex)) != null) && ValueType.STRING.equals(supportedConnectionTypeValue.getType())) {

              String supportedConnectionType;

              if ((supportedConnectionType = ((StringValue<V>)supportedConnectionTypeValue).asText()) != null) {
                for (String supportedTransportName : supportedTransportNames) {
                  if (supportedConnectionType.equals(supportedTransportName)) {

                    return true;
                  }
                }
              }
            }
          }
        }
      }

      return false;
    }
  }, CONNECT(DefaultRoute.CONNECT_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request) {

      ObjectNode adviceNode = JsonNodeFactory.instance.objectNode();

      if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<V>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructConnectErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Handshake required", Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED) && (!supportsConnectionType(protocol, request))) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructConnectErrorResponse(server, getRoute().getPath(), request.getId(), session.getId(), "Connection requested on an unsupported transport", Reconnect.RETRY));
      } else {

        LinkedList<Message<V>> enqueuedMessageList = null;
        Message<V>[] messages;
        Message<V> responseMessage;
        long longPollingTimeout = getLongPollingIntervalMilliseconds(protocol, session, request);

        responseMessage = constructConnectSuccessResponse(server, getRoute().getPath(), request.getId(), session.getId(), longPollingTimeout);

        if (session.getState().lt(SessionState.CONNECTED)) {
          session.completeConnection();
        }

        if (longPollingTimeout > 0) {

          long start = System.currentTimeMillis();

          do {

            Packet<V> enqueuedPacket;

            if ((enqueuedPacket = session.poll()) != null) {
              if (enqueuedMessageList == null) {
                enqueuedMessageList = new LinkedList<>();
              }
              enqueuedMessageList.addAll(Arrays.asList(enqueuedPacket.getMessages()));
            }
          } while (longPollingTimeout + start - System.currentTimeMillis() > 0);
        }

        if (enqueuedMessageList == null) {
          messages = new Message[] {responseMessage};
        } else {
          enqueuedMessageList.addFirst(responseMessage);
          messages = enqueuedMessageList.toArray(new Message[0]);
        }

        adviceNode.put("interval", protocol.getLongPollIntervalMilliseconds());

        return new Packet<V>(PacketType.RESPONSE, session.getId(), getRoute(), messages);
      }
    }

    private <V extends Value<V>> long getLongPollingIntervalMilliseconds (Protocol<V> protocol, Session<V> session, Message<V> request) {

      if (session.isLongPolling()) {

        ObjectValue<V> adviceValue;

        if ((adviceValue = request.getAdvice()) != null) {

          Value<V> timeoutValue;

          if (((timeoutValue = adviceValue.get(Advice.TIMEOUT.getField())) != null) && ValueType.NUMBER.equals(timeoutValue.getType())) {

            return ((NumberValue<V>)timeoutValue).asLong();
          } else {

            return protocol.getLongPollTimeoutMilliseconds();
          }
        }
      }

      return 0;
    }

    private <V extends Value<V>> Message<V> constructConnectSuccessResponse (Server<V> server, String path, String id, String sessionId, long longPollingIntervalMilliseconds) {

      Message<V> response;

      return (Message<V>)(response = constructResponse(server, path, id, sessionId)).put(Message.SUCCESSFUL, true).put(Message.ADVICE, response.getFactory().objectValue().put(Advice.INTERVAL.getField(), longPollingIntervalMilliseconds));
    }

    private <V extends Value<V>> Message<V> constructConnectErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      Message<V> response;

      return (Message<V>)(response = constructResponse(server, path, id, sessionId)).put(Message.SUCCESSFUL, false).put(Message.ERROR, error).put(Message.ADVICE, response.getFactory().objectValue().put(Advice.RECONNECT.getField(), reconnect.getCode()));
    }

    private <V extends Value<V>> boolean supportsConnectionType (Protocol<V> protocol, Message<V> request) {

      Value<V> connectionTypeValue;

      if (((connectionTypeValue = request.get(Message.CONNECTION_TYPE)) != null) && ValueType.STRING.equals(connectionTypeValue.getType())) {

        String[] supportedTransportNames;

        if (((supportedTransportNames = protocol.getTransportNames()) != null)) {
          for (String supportedTransportName : supportedTransportNames) {
            if ((supportedTransportName != null) && supportedTransportName.equals(((StringValue<V>)connectionTypeValue).asText())) {

              return true;
            }
          }
        }
      }

      return false;
    }
  },
  DISCONNECT(DefaultRoute.DISCONNECT_ROUTE) {

  },
  SUBSCRIBE(DefaultRoute.SUBSCRIBE_ROUTE) {

  },
  UNSUBSCRIBE(DefaultRoute.UNSUBSCRIBE_ROUTE) {

  },
  PUBLISH(null) {

  };

  private static final Meta[] COMMANDS = new Meta[] {HANDSHAKE, CONNECT, DISCONNECT, SUBSCRIBE, UNSUBSCRIBE};
  private final Route route;

  Meta (Route route) {

    this.route = route;
  }

  private static <V extends Value<V>> Message<V> constructErrorResponse (Server<V> server, String path, String id, String sessionId, String error) {

    return (Message<V>)constructResponse(server, path, id, sessionId).put(Message.SUCCESSFUL, false).put(Message.ERROR, error);
  }

  private static <V extends Value<V>> Message<V> constructResponse (Server<V> server, String path, String id, String sessionId) {

    return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.SESSION_ID, sessionId);
  }

  public Route getRoute () {

    return route;
  }

  public Meta from (String path)
    throws MetaProcessingException {

    if (path == null) {
      throw new MetaProcessingException("All messages require a channel attribute");
    } else {

      for (Meta meta : COMMANDS) {
        if (meta.getRoute().getPath().equals(path)) {

          return meta;
        }
      }

      if (path.startsWith("/meta")) {
        throw new MetaProcessingException("Attempt to publish to a meta channel");
      } else {

        return PUBLISH;
      }
    }
  }
}
