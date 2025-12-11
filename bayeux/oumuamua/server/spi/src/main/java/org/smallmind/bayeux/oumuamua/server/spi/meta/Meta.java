/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.SecurityRejection;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.MetaProcessingException;
import org.smallmind.nutsnbolts.util.MutationUtility;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Implements server-side handling for Bayeux meta channels and related operations.
 */
public enum Meta {

  HANDSHAKE(DefaultRoute.HANDSHAKE_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request) {

      SecurityPolicy<V> securityPolicy;
      SecurityRejection rejection;

      if (((securityPolicy = server.getSecurityPolicy()) != null) && ((rejection = securityPolicy.canHandshake(session, request)) != null)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructHandshakeErrorResponse(server, route.getPath(), request.getId(), session.getId(), rejection.hasReason() ? "Unauthorized: " + rejection.getReason() : "Unauthorized", Reconnect.NONE));
      } else if (session.getState().gte(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructHandshakeErrorResponse(server, route.getPath(), request.getId(), session.getId(), "Handshake was previously completed", Reconnect.RETRY));
      } else if (!supportsConnectionType(protocol, request)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructHandshakeErrorResponse(server, route.getPath(), request.getId(), session.getId(), "Handshake attempted on an unsupported transport", Reconnect.HANDSHAKE));
      } else {
        session.completeHandshake();

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructHandshakeSuccessResponse(protocol, server, route.getPath(), request.getId(), session.getId()));
      }
    }

    /**
     * Builds a successful handshake response describing the server and supported transports.
     *
     * @param protocol protocol used for the handshake
     * @param server owning server instance
     * @param path meta handshake channel path
     * @param id request message id
     * @param sessionId client session identifier
     * @param <V> value type
     * @return populated handshake response message
     */
    private <V extends Value<V>> Message<V> constructHandshakeSuccessResponse (Protocol<V> protocol, Server<V> server, String path, String id, String sessionId) {

      Message<V> response;

      return (Message<V>)(response = constructSuccessResponse(server, path, id, sessionId, null)).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(protocol.getTransportNames(), text -> response.getFactory().textValue(text))));
    }

    /**
     * Builds an error response for a failed handshake, including server capabilities.
     *
     * @param server owning server instance
     * @param path meta handshake channel path
     * @param id request message id
     * @param sessionId client session identifier
     * @param error explanation of the failure
     * @param reconnect reconnect advice to emit, if any
     * @param <V> value type
     * @return error response message
     */
    private <V extends Value<V>> Message<V> constructHandshakeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      Message<V> response;

      return (Message<V>)(response = constructErrorResponse(server, path, id, sessionId, error, reconnect)).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(TransportUtility.accumulateSupportedTransportNames(server), text -> response.getFactory().textValue(text))));
    }

    /**
     * Checks whether the client proposed a connection type supported by the protocol.
     *
     * @param protocol protocol handling the request
     * @param request incoming handshake message
     * @param <V> value type
     * @return {@code true} when at least one supported transport is offered
     */
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
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request)
      throws InterruptedException {

      if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructConnectErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Handshake required", Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED) && (!supportsConnectionType(protocol, request))) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructConnectErrorResponse(server, route.getPath(), request.getId(), session.getId(), "Connection requested on an unsupported transport", Reconnect.HANDSHAKE));
      } else {

        Message<V>[] messages;
        long sessionConnectIntervalMilliseconds = Math.max(0, server.getSessionConnectionIntervalMilliseconds());

        if (session.isLongPolling()) {

          Message<V> responseMessage = constructConnectSuccessResponse(server, route.getPath(), request.getId(), session.getId(), 0);
          LinkedList<Message<V>> enqueuedMessageList = null;
          boolean initial = true;
          boolean timeoutAdvised = false;
          boolean connected = SessionState.CONNECTED.equals(session.getState());
          long longPollTimeoutMilliseconds = getLongPollTimeoutMilliseconds(request);
          long remainingMilliseconds = 0;
          long connectStartTime = System.currentTimeMillis();
          long firstPollTime = connectStartTime;

          if (longPollTimeoutMilliseconds < 0) {
            longPollTimeoutMilliseconds = Math.max(0, protocol.getLongPollTimeoutMilliseconds());
          } else {
            timeoutAdvised = true;
          }

          do {

            Packet<V> enqueuedPacket;

            if ((enqueuedPacket = session.poll(connected ? initial ? timeoutAdvised ? longPollTimeoutMilliseconds : sessionConnectIntervalMilliseconds : remainingMilliseconds : 0, TimeUnit.MILLISECONDS)) != null) {
              if (enqueuedPacket.getMessages() != null) {
                if (enqueuedMessageList == null) {
                  enqueuedMessageList = new LinkedList<>();
                  firstPollTime = System.currentTimeMillis();
                }
                enqueuedMessageList.addAll(Arrays.asList(enqueuedPacket.getMessages()));
              }
            }

            initial = false;
          } while (connected && ((remainingMilliseconds = (timeoutAdvised ? longPollTimeoutMilliseconds + connectStartTime : ((enqueuedMessageList == null) || enqueuedMessageList.isEmpty()) ? sessionConnectIntervalMilliseconds + connectStartTime : longPollTimeoutMilliseconds + firstPollTime) - System.currentTimeMillis()) > 0));

          if (enqueuedMessageList == null) {
            messages = new Message[] {responseMessage};
          } else {
            enqueuedMessageList.addFirst(responseMessage);
            messages = enqueuedMessageList.toArray(new Message[0]);
          }
        } else {
          messages = new Message[] {constructConnectSuccessResponse(server, route.getPath(), request.getId(), session.getId(), sessionConnectIntervalMilliseconds)};
        }

        if (session.getState().lt(SessionState.CONNECTED)) {
          session.completeConnection();
        }

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, messages);
      }
    }

    /**
     * Extracts the timeout supplied in the connect advice, if any.
     *
     * @param request connect message carrying optional advice
     * @param <V> value type
     * @return client requested timeout in milliseconds, or {@code -1} when unspecified
     */
    private <V extends Value<V>> long getLongPollTimeoutMilliseconds (Message<V> request) {

      ObjectValue<V> adviceValue;

      if ((adviceValue = request.getAdvice()) != null) {

        Value<V> clientTimeoutValue;

        if (((clientTimeoutValue = adviceValue.get(Advice.TIMEOUT.getField())) != null) && ValueType.NUMBER.equals(clientTimeoutValue.getType())) {

          return ((NumberValue<V>)clientTimeoutValue).asLong();
        }
      }

      return -1;
    }

    /**
     * Builds a successful connect response including interval advice.
     *
     * @param server owning server
     * @param path connect channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param sessionConnectIntervalMilliseconds interval before the next connect
     * @param <V> value type
     * @return connect response message
     */
    private <V extends Value<V>> Message<V> constructConnectSuccessResponse (Server<V> server, String path, String id, String sessionId, long sessionConnectIntervalMilliseconds) {

      Message<V> response;

      return (Message<V>)(response = constructSuccessResponse(server, path, id, sessionId, null)).put(Message.ADVICE, response.getFactory().objectValue().put(Advice.INTERVAL.getField(), sessionConnectIntervalMilliseconds));
    }

    /**
     * Builds an error response for a failed connect request.
     *
     * @param server owning server
     * @param path connect channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param error error description
     * @param reconnect reconnect advice to include
     * @param <V> value type
     * @return connect error response message
     */
    private <V extends Value<V>> Message<V> constructConnectErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      return constructErrorResponse(server, path, id, sessionId, error, reconnect);
    }

    /**
     * Validates that the requested connection type is recognized by the transport protocol.
     *
     * @param protocol protocol handling the request
     * @param request connect message
     * @param <V> value type
     * @return {@code true} if the transport supports the requested type
     */
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
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request) {

      session.completeDisconnect();

      return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructDisconnectSuccessResponse(server, route.getPath(), request.getId(), request.getSessionId()));
    }

    /**
     * Builds the standard successful disconnect response.
     *
     * @param server owning server
     * @param path disconnect channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param <V> value type
     * @return disconnect response message
     */
    private <V extends Value<V>> Message<V> constructDisconnectSuccessResponse (Server<V> server, String path, String id, String sessionId) {

      return constructSuccessResponse(server, path, id, sessionId, Reconnect.NONE);
    }
  }, SUBSCRIBE(DefaultRoute.SUBSCRIBE_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request) {

      String subscription;

      if ((subscription = getSubscription(request)) == null) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Missing subscription", null, null));
      } else if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Handshake required", subscription, Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED) && (!server.allowsImplicitConnection())) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Connection required", subscription, Reconnect.RETRY));
      } else if (subscription.startsWith("/meta/")) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Attempted subscription to a meta channel", subscription, null));
      } else {

        SecurityPolicy<V> securityPolicy = server.getSecurityPolicy();
        SecurityRejection rejection;
        Channel<V> channel;

        // If we're here without a connection, it's because we're explicitly allowing implicit connections, which makes no sense in terms of the bayeux protocol,
        // but cometd does it anyway, and the client expects it...
        if (session.getState().lt(SessionState.CONNECTED)) {
          session.completeConnection();
        }

        try {
          if ((channel = server.findChannel(subscription)) == null) {

            if ((securityPolicy != null) && ((rejection = securityPolicy.canCreate(session, subscription, request)) != null)) {

              return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), rejection.hasReason() ? "Unauthorized: " + rejection.getReason() : "Unauthorized", subscription, Reconnect.NONE));
            } else {
              channel = server.requireChannel(subscription);
            }
          }
        } catch (InvalidPathException invalidPathException) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), invalidPathException.getMessage(), subscription, null));
        }

        if ((securityPolicy != null) && ((rejection = securityPolicy.canSubscribe(session, channel, request)) != null)) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), rejection.hasReason() ? "Unauthorized: " + rejection.getReason() : "Unauthorized", subscription, Reconnect.NONE));
        } else if (!channel.subscribe(session)) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Attempted subscription to a closed channel", subscription, null));
        } else {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructSubscribeSuccessResponse(server, route.getPath(), request.getId(), request.getSessionId(), subscription));
        }
      }
    }

    /**
     * Builds a successful subscribe response including the requested channel.
     *
     * @param server owning server
     * @param path subscribe channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param subscription subscribed channel path
     * @param <V> value type
     * @return subscribe response message
     */
    private <V extends Value<V>> Message<V> constructSubscribeSuccessResponse (Server<V> server, String path, String id, String sessionId, String subscription) {

      return (Message<V>)constructSuccessResponse(server, path, id, sessionId, null).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Builds an error response for a failed subscribe attempt.
     *
     * @param server owning server
     * @param path subscribe channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param error human-readable error
     * @param subscription requested subscription channel
     * @param reconnect reconnect advice, if applicable
     * @param <V> value type
     * @return subscribe error response
     */
    private <V extends Value<V>> Message<V> constructSubscribeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, String subscription, Reconnect reconnect) {

      return (Message<V>)constructErrorResponse(server, path, id, sessionId, error, reconnect).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Retrieves the subscription channel from the incoming message.
     *
     * @param request incoming subscribe message
     * @param <V> value type
     * @return subscription path, or {@code null} if missing or of the wrong type
     */
    private <V extends Value<V>> String getSubscription (Message<V> request) {

      Value<V> subscriptionValue;

      return (((subscriptionValue = request.get(Message.SUBSCRIPTION)) != null) && ValueType.STRING.equals(subscriptionValue.getType())) ? ((StringValue<V>)subscriptionValue).asText() : null;
    }
  }, UNSUBSCRIBE(DefaultRoute.UNSUBSCRIBE_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request)
      throws InvalidPathException {

      String subscription;

      if ((subscription = getSubscription(request)) == null) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructUnsubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Missing subscription", null, null));
      } else if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructUnsubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Handshake required", subscription, Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructUnsubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Connection required", subscription, Reconnect.RETRY));
      } else if (subscription.startsWith("/meta/")) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructUnsubscribeErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Attempted subscription to a meta channel", subscription, null));
      } else {

        Channel<V> channel;

        if ((channel = server.findChannel(subscription)) != null) {
          channel.unsubscribe(session);
        }

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructUnsubscribeSuccessResponse(server, route.getPath(), request.getId(), request.getSessionId(), subscription));
      }
    }

    /**
     * Builds a successful unsubscribe response including the channel that was removed.
     *
     * @param server owning server
     * @param path unsubscribe channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param subscription subscription that was removed
     * @param <V> value type
     * @return unsubscribe response message
     */
    private <V extends Value<V>> Message<V> constructUnsubscribeSuccessResponse (Server<V> server, String path, String id, String sessionId, String subscription) {

      return (Message<V>)constructSuccessResponse(server, path, id, sessionId, null).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Builds an error response for a failed unsubscribe request.
     *
     * @param server owning server
     * @param path unsubscribe channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param error explanation of the failure
     * @param subscription target subscription channel
     * @param reconnect reconnect advice if applicable
     * @param <V> value type
     * @return unsubscribe error response
     */
    private <V extends Value<V>> Message<V> constructUnsubscribeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, String subscription, Reconnect reconnect) {

      return (Message<V>)constructErrorResponse(server, path, id, sessionId, error, reconnect).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Extracts the subscription path from the unsubscribe message.
     *
     * @param request incoming unsubscribe message
     * @param <V> value type
     * @return subscription channel, or {@code null} if missing or malformed
     */
    private <V extends Value<V>> String getSubscription (Message<V> request) {

      Value<V> subscriptionValue;

      return (((subscriptionValue = request.get(Message.SUBSCRIPTION)) != null) && ValueType.STRING.equals(subscriptionValue.getType())) ? ((StringValue<V>)subscriptionValue).asText() : null;
    }
  }, PUBLISH(null) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request) {

      if ((!session.getId().equals(request.getSessionId())) || session.getState().lt(SessionState.HANDSHOOK)) {

        return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Handshake required", Reconnect.HANDSHAKE));
      } else if (session.getState().lt(SessionState.CONNECTED)) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Connection required", Reconnect.RETRY));
      } else if (route.getPath().startsWith("/meta/")) {

        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Attempted to publish to a meta channel", null));
      } else {

        SecurityPolicy<V> securityPolicy = server.getSecurityPolicy();
        SecurityRejection rejection;
        Channel<V> channel;

        try {
          if ((channel = server.findChannel(route.getPath())) == null) {
            if ((securityPolicy != null) && ((rejection = securityPolicy.canCreate(session, route.getPath(), request)) != null)) {

              return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), rejection.hasReason() ? "Unauthorized: " + rejection.getReason() : "Unauthorized", Reconnect.NONE));
            } else {
              channel = server.requireChannel(route.getPath());
            }
          }
        } catch (InvalidPathException invalidPathException) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), invalidPathException.getMessage(), null));
        }

        if ((securityPolicy != null) && ((rejection = securityPolicy.canPublish(session, channel, request)) != null)) {

          return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), rejection.hasReason() ? "Unauthorized: " + rejection.getReason() : "Unauthorized", Reconnect.NONE));
        } else {
          try {

            Message<V> deliveryMessage = constructDeliveryMessage(server, route.getPath(), request.getId(), request.get(Message.DATA));

            ((AbstractProtocol<V>)protocol).onPublish(request, deliveryMessage);
            server.deliver(session, new Packet<>(PacketType.DELIVERY, session.getId(), route, deliveryMessage), true);

            if (getEchoFlag(request)) {
              return new Packet<V>(PacketType.RESPONSE, session.getId(), route, new Message[] {constructPublishSuccessResponse(server, route.getPath(), request.getId(), session.getId()), request});
            } else {
              return new Packet<V>(PacketType.RESPONSE, session.getId(), route, constructPublishSuccessResponse(server, route.getPath(), request.getId(), session.getId()));
            }
          } catch (Exception exception) {
            LoggerManager.getLogger(Meta.class).error(exception);

            return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructPublishErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), exception.getMessage(), null));
          }
        }
      }
    }

    /**
     * Indicates whether the client requested an echo of its published message.
     *
     * @param request publish message that may contain an oumuamua echo flag
     * @param <V> value type
     * @return {@code true} when the client expects an echo or did not specify a preference
     */
    private <V extends Value<V>> boolean getEchoFlag (Message<V> request) {

      ObjectValue<V> ext;
      ObjectValue<V> oumuamua;
      BooleanValue<V> echo;

      // cometd echoes by default, so that's what we do here
      return ((echo = ((oumuamua = ((ext = request.getExt()) == null) ? null : (ObjectValue<V>)ext.get("oumuamua")) == null) ? null : (BooleanValue<V>)oumuamua.get("echo")) == null) || echo.asBoolean();
    }

    /**
     * Constructs the delivery message sent to subscribers for a publish request.
     *
     * @param server owning server
     * @param path destination channel path
     * @param id request id
     * @param data payload supplied by the publisher
     * @param <V> value type
     * @return delivery message
     */
    private <V extends Value<V>> Message<V> constructDeliveryMessage (Server<V> server, String path, String id, Value<V> data) {

      return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.DATA, data);
    }

    /**
     * Builds a successful publish response message.
     *
     * @param server owning server
     * @param path publish channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param <V> value type
     * @return publish success response
     */
    private <V extends Value<V>> Message<V> constructPublishSuccessResponse (Server<V> server, String path, String id, String sessionId) {

      return constructSuccessResponse(server, path, id, sessionId, null);
    }

    /**
     * Builds an error response for a failed publish request.
     *
     * @param server owning server
     * @param path publish channel path
     * @param id request id
     * @param sessionId client session identifier
     * @param error error detail
     * @param reconnect reconnect advice to include, if any
     * @param <V> value type
     * @return publish error response
     */
    private <V extends Value<V>> Message<V> constructPublishErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      return constructErrorResponse(server, path, id, sessionId, error, reconnect);
    }
  },
  SERVICE(null) {
    @Override
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request) {

      BayeuxService<V> service;

      if ((service = server.getService(route)) == null) {
        return new Packet<>(PacketType.RESPONSE, session.getId(), route, constructErrorResponse(server, route.getPath(), request.getId(), request.getSessionId(), "Unknown service", null));
      } else {

        return service.process(protocol, route, server, session, request);
      }
    }
  };

  private static final Meta[] COMMANDS = new Meta[] {HANDSHAKE, CONNECT, DISCONNECT, SUBSCRIBE, UNSUBSCRIBE};
  private final Route route;

  /**
   * Creates a meta command bound to the given route.
   *
   * @param route meta route handled by this command, or {@code null} when not bound
   */
  Meta (Route route) {

    this.route = route;
  }

  /**
   * Constructs a successful meta response with optional reconnect advice.
   *
   * @param server    owning server
   * @param path      channel path
   * @param id        message id
   * @param sessionId session identifier
   * @param reconnect reconnect advice (optional)
   * @param <V>       value type
   * @return response message
   */
  private static <V extends Value<V>> Message<V> constructSuccessResponse (Server<V> server, String path, String id, String sessionId, Reconnect reconnect) {

    Message<V> response = constructResponse(server, path, id, sessionId);

    response.put(Message.SUCCESSFUL, true);

    if (reconnect != null) {
      response.put(Message.ADVICE, response.getFactory().objectValue().put(Advice.RECONNECT.getField(), reconnect.getCode()));
    }

    return response;
  }

  /**
   * Creates a meta error response including optional reconnect advice.
   *
   * @param server    owning server
   * @param path      channel path being processed
   * @param id        message id from the request
   * @param sessionId client session identifier
   * @param error     description of the error condition
   * @param reconnect reconnect advice, if any
   * @param <V>       value type
   * @return populated error response
   */
  public static <V extends Value<V>> Message<V> constructErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

    Message<V> response = constructResponse(server, path, id, sessionId);

    response.put(Message.SUCCESSFUL, false).put(Message.ERROR, error);

    if (reconnect != null) {
      response.put(Message.ADVICE, response.getFactory().objectValue().put(Advice.RECONNECT.getField(), reconnect.getCode()));
    }

    return response;
  }

  /**
   * Constructs the base response skeleton shared by meta responses.
   *
   * @param server    owning server
   * @param path      channel path
   * @param id        request id
   * @param sessionId client session identifier
   * @param <V>       value type
   * @return response message seeded with channel, id, and session
   */
  private static <V extends Value<V>> Message<V> constructResponse (Server<V> server, String path, String id, String sessionId) {

    return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.SESSION_ID, sessionId);
  }

  /**
   * Determines the meta command for a channel path.
   *
   * @param path channel path from the request
   * @return matching meta command or {@link #PUBLISH}/{@link #SERVICE}
   * @throws MetaProcessingException if the path targets meta channels incorrectly
   */
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

      if (path.startsWith("/meta/")) {
        throw new MetaProcessingException("Attempt to publish to a meta channel");
      } else if (path.startsWith("/service/")) {

        return SERVICE;
      } else {

        return PUBLISH;
      }
    }
  }

  /**
   * @return route handled by this meta command, or {@code null} if none
   */
  public Route getRoute () {

    return route;
  }

  /**
   * Processes an incoming meta message.
   *
   * @param protocol protocol handling the request
   * @param route    resolved route
   * @param server   owning server
   * @param session  session issuing the request
   * @param request  incoming message
   * @param <V>      value type
   * @return response packet
   * @throws InterruptedException if processing is interrupted
   * @throws InvalidPathException if the request uses an invalid path
   */
  public abstract <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws InterruptedException, InvalidPathException;
}
