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
import java.util.concurrent.TimeUnit;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
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

      return (Message<V>)(response = constructSuccessResponse(server, path, id, sessionId)).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(protocol.getTransportNames(), text -> response.getFactory().textValue(text))));
    }

    private <V extends Value<V>> Message<V> constructHandshakeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      Message<V> response;

      return (Message<V>)(response = constructErrorResponse(server, path, id, sessionId, error, reconnect)).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(TransportUtility.accumulateSupportedTransportNames(server), text -> response.getFactory().textValue(text))));
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
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request)
      throws InterruptedException {

      if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructConnectErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Handshake required", Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED) && (!supportsConnectionType(protocol, request))) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructConnectErrorResponse(server, getRoute().getPath(), request.getId(), session.getId(), "Connection requested on an unsupported transport", Reconnect.HANDSHAKE));
      } else {

        Message<V>[] messages;
        Message<V> responseMessage;

        responseMessage = constructConnectSuccessResponse(server, getRoute().getPath(), request.getId(), session.getId(), SessionState.CONNECTED.equals(session.getState()) ? getLongPollIntervalMilliseconds(protocol, request) : 0);

        if (session.getState().lt(SessionState.CONNECTED)) {
          session.completeConnection();
        }

        if (protocol.isLongPolling()) {

          LinkedList<Message<V>> enqueuedMessageList = null;
          boolean initial = true;
          long longPollTimeoutMilliseconds = getLongPollTimeoutMilliseconds(protocol, request);
          long remainingMilliseconds = 0;
          long start = System.currentTimeMillis();

          do {

            Packet<V> enqueuedPacket;

            if ((enqueuedPacket = session.poll(initial ? protocol.getLongPollIntervalMilliseconds() : remainingMilliseconds, TimeUnit.MILLISECONDS)) != null) {
              if (enqueuedPacket.getMessages() != null) {
                if (enqueuedMessageList == null) {
                  enqueuedMessageList = new LinkedList<>();
                }
                enqueuedMessageList.addAll(Arrays.asList(enqueuedPacket.getMessages()));
              }
            }

            initial = false;
          } while ((remainingMilliseconds = longPollTimeoutMilliseconds + start - System.currentTimeMillis()) > 0);

          if (enqueuedMessageList == null) {
            messages = new Message[] {responseMessage};
          } else {
            enqueuedMessageList.addFirst(responseMessage);
            messages = enqueuedMessageList.toArray(new Message[0]);
          }
        } else {
          messages = new Message[] {responseMessage};
        }

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), messages);
      }
    }

    private <V extends Value<V>> long getLongPollTimeoutMilliseconds (Protocol<V> protocol, Message<V> request) {

      ObjectValue<V> adviceValue;

      if ((adviceValue = request.getAdvice()) != null) {

        Value<V> timeoutValue;

        if (((timeoutValue = adviceValue.get(Advice.TIMEOUT.getField())) != null) && ValueType.NUMBER.equals(timeoutValue.getType())) {

          return protocol.getLongPollTimeoutMilliseconds() > 0 ? Math.max(protocol.getLongPollTimeoutMilliseconds(), ((NumberValue<V>)timeoutValue).asLong()) : Math.max(0, ((NumberValue<V>)timeoutValue).asLong());
        }
      }

      return Math.max(0, protocol.getLongPollTimeoutMilliseconds());
    }

    private <V extends Value<V>> long getLongPollIntervalMilliseconds (Protocol<V> protocol, Message<V> request) {

      ObjectValue<V> adviceValue;

      if ((adviceValue = request.getAdvice()) != null) {

        Value<V> timeoutValue;

        if (((timeoutValue = adviceValue.get(Advice.INTERVAL.getField())) != null) && ValueType.NUMBER.equals(timeoutValue.getType())) {

          return ((NumberValue<V>)timeoutValue).asLong();
        }
      }

      return protocol.getLongPollIntervalMilliseconds();
    }

    private <V extends Value<V>> Message<V> constructConnectSuccessResponse (Server<V> server, String path, String id, String sessionId, long longPollIntervalMilliseconds) {

      Message<V> response;

      return (Message<V>)(response = constructSuccessResponse(server, path, id, sessionId)).put(Message.ADVICE, response.getFactory().objectValue().put(Advice.INTERVAL.getField(), longPollIntervalMilliseconds));
    }

    private <V extends Value<V>> Message<V> constructConnectErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      return constructErrorResponse(server, path, id, sessionId, error, reconnect);
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
  }, DISCONNECT(DefaultRoute.DISCONNECT_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request) {

      session.completeDisconnect();

      return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructDisconnectSuccessResponse(server, getRoute().getPath(), request.getId(), request.getSessionId()));
    }

    private <V extends Value<V>> Message<V> constructDisconnectSuccessResponse (Server<V> server, String path, String id, String sessionId) {

      return constructSuccessResponse(server, path, id, sessionId);
    }
  }, SUBSCRIBE(DefaultRoute.SUBSCRIBE_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request) {

      String subscription;

      if ((subscription = getSubscription(request)) == null) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Missing subscription", null, null));
      } else if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Handshake required", subscription, Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Connection required", subscription, Reconnect.RETRY));
      } else if (subscription.startsWith("/meta/")) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Attempted subscription to a meta channel", subscription, null));
      } else {

        SecurityPolicy securityPolicy = server.getSecurityPolicy();
        Channel<V> channel;

        try {
          if ((channel = server.findChannel(subscription)) == null) {
            if ((securityPolicy != null) && (!securityPolicy.canCreate(session, subscription, request))) {

              return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Unauthorized", subscription, Reconnect.NONE));
            } else {
              channel = server.requireChannel(subscription);
            }
          }
        } catch (InvalidPathException invalidPathException) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), invalidPathException.getMessage(), subscription, null));
        }

        if ((securityPolicy != null) && (!securityPolicy.canSubscribe(session, channel, request))) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Unauthorized", subscription, Reconnect.NONE));
        } else if (!channel.subscribe(session)) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Attempted subscription to a closed channel", subscription, null));
        } else {

          return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructSubscribeSuccessResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), subscription));
        }
      }
    }

    private <V extends Value<V>> Message<V> constructSubscribeSuccessResponse (Server<V> server, String path, String id, String sessionId, String subscription) {

      return (Message<V>)constructSuccessResponse(server, path, id, sessionId).put(Message.SUBSCRIPTION, subscription);
    }

    private <V extends Value<V>> Message<V> constructSubscribeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, String subscription, Reconnect reconnect) {

      return (Message<V>)constructErrorResponse(server, path, id, sessionId, error, reconnect).put(Message.SUBSCRIPTION, subscription);
    }

    private <V extends Value<V>> String getSubscription (Message<V> request) {

      Value<V> subscriptionValue;

      return (((subscriptionValue = request.get(Message.SUBSCRIPTION)) != null) && ValueType.STRING.equals(subscriptionValue.getType())) ? ((StringValue<V>)subscriptionValue).asText() : null;
    }
  }, UNSUBSCRIBE(DefaultRoute.UNSUBSCRIBE_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request)
      throws InvalidPathException {

      String subscription;

      if ((subscription = getSubscription(request)) == null) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructUnsubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Missing subscription", null, null));
      } else if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), getRoute(), constructUnsubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Handshake required", subscription, Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructUnsubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Connection required", subscription, Reconnect.RETRY));
      } else if (subscription.startsWith("/meta/")) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructUnsubscribeErrorResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), "Attempted subscription to a meta channel", subscription, null));
      } else {

        Channel<V> channel;

        if ((channel = server.findChannel(subscription)) != null) {
          channel.unsubscribe(session);
        }

        return new Packet<>(PacketType.RESPONSE, session.getId(), getRoute(), constructUnsubscribeSuccessResponse(server, getRoute().getPath(), request.getId(), request.getSessionId(), subscription));
      }
    }

    private <V extends Value<V>> Message<V> constructUnsubscribeSuccessResponse (Server<V> server, String path, String id, String sessionId, String subscription) {

      return (Message<V>)constructSuccessResponse(server, path, id, sessionId).put(Message.SUBSCRIPTION, subscription);
    }

    private <V extends Value<V>> Message<V> constructUnsubscribeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, String subscription, Reconnect reconnect) {

      return (Message<V>)constructErrorResponse(server, path, id, sessionId, error, reconnect).put(Message.SUBSCRIPTION, subscription);
    }

    private <V extends Value<V>> String getSubscription (Message<V> request) {

      Value<V> subscriptionValue;

      return (((subscriptionValue = request.get(Message.SUBSCRIPTION)) != null) && ValueType.STRING.equals(subscriptionValue.getType())) ? ((StringValue<V>)subscriptionValue).asText() : null;
    }
  }, PUBLISH(null) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request) {

      String path;

      if ((path = request.getChannel()) == null) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), null, constructPublishErrorResponse(server, null, request.getId(), request.getSessionId(), "Missing channel", null));
      } else {

        Route route;

        try {
          route = new DefaultRoute(path);
        } catch (InvalidPathException invalidPathException) {

          return new Packet<>(PacketType.RESPONSE, request.getSessionId(), null, constructPublishErrorResponse(server, path, request.getId(), request.getSessionId(), invalidPathException.getMessage(), null));
        }

        if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

          return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructPublishErrorResponse(server, path, request.getId(), request.getSessionId(), "Handshake required", Reconnect.HANDSHAKE));
        } else if (session.getState().lt(SessionState.CONNECTED)) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, path, request.getId(), request.getSessionId(), "Connection required", Reconnect.RETRY));
        } else if (path.startsWith("/meta/")) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, path, request.getId(), request.getSessionId(), "Attempted to publish to a meta channel", null));
        } else {

          SecurityPolicy securityPolicy = server.getSecurityPolicy();
          Channel<V> channel;

          try {
            channel = server.findChannel(path);
          } catch (InvalidPathException invalidPathException) {

            return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, path, request.getId(), request.getSessionId(), invalidPathException.getMessage(), null));
          }

          if ((securityPolicy != null) && (!securityPolicy.canPublish(session, channel, request))) {

            return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, path, request.getId(), request.getSessionId(), "Unauthorized", Reconnect.NONE));
          } else {
            server.deliver(new Packet<>(PacketType.DELIVERY, session.getId(), route, constructDeliveryMessage(server, path, request.getId(), request.get(Message.DATA))));

            return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishSuccessResponse(server, path, request.getId(), session.getId()));
          }
        }
      }
    }

    private <V extends Value<V>> Message<V> constructDeliveryMessage (Server<V> server, String path, String id, Value<V> data) {

      return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.DATA, data);
    }

    private <V extends Value<V>> Message<V> constructPublishSuccessResponse (Server<V> server, String path, String id, String sessionId) {

      return constructSuccessResponse(server, path, id, sessionId);
    }

    private <V extends Value<V>> Message<V> constructPublishErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      return constructErrorResponse(server, path, id, sessionId, error, reconnect);
    }
  };

  private static final Meta[] COMMANDS = new Meta[] {HANDSHAKE, CONNECT, DISCONNECT, SUBSCRIBE, UNSUBSCRIBE};
  private final Route route;

  Meta (Route route) {

    this.route = route;
  }

  private static <V extends Value<V>> Message<V> constructSuccessResponse (Server<V> server, String path, String id, String sessionId) {

    return (Message<V>)constructResponse(server, path, id, sessionId).put(Message.SUCCESSFUL, true);
  }

  public static <V extends Value<V>> Message<V> constructErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

    Message<V> response = constructResponse(server, path, id, sessionId);

    response.put(Message.SUCCESSFUL, false).put(Message.ERROR, error);

    if (reconnect != null) {
      response.put(Message.ADVICE, response.getFactory().objectValue().put(Advice.RECONNECT.getField(), reconnect.getCode()));
    }

    return response;
  }

  private static <V extends Value<V>> Message<V> constructResponse (Server<V> server, String path, String id, String sessionId) {

    return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.SESSION_ID, sessionId);
  }

  public static Meta from (String path)
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

  public Route getRoute () {

    return route;
  }

  public abstract <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request)
    throws InterruptedException, InvalidPathException;
}
