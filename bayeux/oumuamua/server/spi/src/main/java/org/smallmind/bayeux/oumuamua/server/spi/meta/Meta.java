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
 * Enum-based dispatch table for all Bayeux meta-channel operations; each constant owns one
 * meta-channel path and implements the full server-side state-machine logic for that operation,
 * including security checks, session state transitions, and response construction.
 */
public enum Meta {

  /**
   * Handles {@code /meta/handshake}: validates the session state and proposed connection type,
   * delegates to the security policy, transitions the session to {@code HANDSHOOK} on success,
   * and returns a response containing Bayeux version and supported transport lists.
   */
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
     * Builds a successful handshake response populated with the Bayeux version, minimum version,
     * and the transport names supported by the responding protocol.
     *
     * @param protocol  protocol that accepted the handshake, whose transport names are advertised
     * @param server    server supplying version strings and codec
     * @param path      {@code /meta/handshake} channel path written to the response
     * @param id        request message id echoed in the response
     * @param sessionId client session identifier written to the response
     * @param <V>       value type
     * @return fully populated success response message
     */
    private <V extends Value<V>> Message<V> constructHandshakeSuccessResponse (Protocol<V> protocol, Server<V> server, String path, String id, String sessionId) {

      Message<V> response;

      return (Message<V>)(response = constructSuccessResponse(server, path, id, sessionId, null)).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(protocol.getTransportNames(), text -> response.getFactory().textValue(text))));
    }

    /**
     * Builds a failed handshake response that includes the server's Bayeux version, minimum version,
     * and the full set of supported transports across all protocols, plus reconnect advice.
     *
     * @param server    server supplying version strings and the full transport name list
     * @param path      {@code /meta/handshake} channel path written to the response
     * @param id        request message id echoed in the response
     * @param sessionId client session identifier written to the response
     * @param error     human-readable description of why the handshake was rejected
     * @param reconnect reconnect advice the client should follow after this failure
     * @param <V>       value type
     * @return fully populated error response message
     */
    private <V extends Value<V>> Message<V> constructHandshakeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      Message<V> response;

      return (Message<V>)(response = constructErrorResponse(server, path, id, sessionId, error, reconnect)).put(Message.VERSION, server.getBayeuxVersion()).put(Message.MINIMUM_VERSION, server.getMinimumBayeuxVersion()).put(Message.SUPPORTED_CONNECTION_TYPES, response.getFactory().arrayValue().addAll(MutationUtility.toList(TransportUtility.accumulateSupportedTransportNames(server), text -> response.getFactory().textValue(text))));
    }

    /**
     * Checks whether the {@code supportedConnectionTypes} array in the handshake request contains
     * at least one transport name recognized by {@code protocol}.
     *
     * @param protocol protocol whose transport names are the accepted set
     * @param request  incoming handshake message whose {@code supportedConnectionTypes} field is inspected
     * @param <V>      value type
     * @return {@code true} when the intersection of client-proposed and protocol-supported types is non-empty
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
  },

  /**
   * Handles {@code /meta/connect}: verifies handshake state and transport compatibility, transitions
   * the session to {@code CONNECTED} on first connect, and performs a long-poll loop when the
   * transport supports it — blocking until queued messages arrive or the timeout elapses.
   */
  CONNECT(DefaultRoute.CONNECT_ROUTE) {
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
     * Reads the {@code advice.timeout} value from the connect request, representing the client's
     * preferred long-poll duration.
     *
     * @param request the connect message that may carry an {@code advice} object with a {@code timeout} field
     * @param <V>     value type
     * @return the client-specified long-poll timeout in milliseconds, or {@code -1} when absent or malformed
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
     * Builds a successful connect response that includes an {@code advice.interval} directing the
     * client when to send the next connect.
     *
     * @param server                             server supplying the codec
     * @param path                               {@code /meta/connect} channel path written to the response
     * @param id                                 request message id echoed in the response
     * @param sessionId                          client session identifier written to the response
     * @param sessionConnectIntervalMilliseconds milliseconds the client should wait before the next connect
     * @param <V>                                value type
     * @return fully populated success response message with interval advice
     */
    private <V extends Value<V>> Message<V> constructConnectSuccessResponse (Server<V> server, String path, String id, String sessionId, long sessionConnectIntervalMilliseconds) {

      Message<V> response;

      return (Message<V>)(response = constructSuccessResponse(server, path, id, sessionId, null)).put(Message.ADVICE, response.getFactory().objectValue().put(Advice.INTERVAL.getField(), sessionConnectIntervalMilliseconds));
    }

    /**
     * Builds an error response for a rejected connect request, delegating to the shared error builder.
     *
     * @param server    server supplying the codec
     * @param path      {@code /meta/connect} channel path written to the response
     * @param id        request message id echoed in the response
     * @param sessionId client session identifier written to the response
     * @param error     human-readable description of why the connect was rejected
     * @param reconnect reconnect advice the client should follow
     * @param <V>       value type
     * @return fully populated error response message
     */
    private <V extends Value<V>> Message<V> constructConnectErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      return constructErrorResponse(server, path, id, sessionId, error, reconnect);
    }

    /**
     * Verifies that the {@code connectionType} field in the connect request matches one of the
     * transport names supported by {@code protocol}.
     *
     * @param protocol protocol whose transport names form the accepted set
     * @param request  the connect message whose {@code connectionType} field is checked
     * @param <V>      value type
     * @return {@code true} when the requested connection type is among the protocol's supported transports
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
  },

  /**
   * Handles {@code /meta/disconnect}: immediately transitions the session to the disconnected state
   * and returns a success response with {@link Reconnect#NONE} advice.
   */
  DISCONNECT(DefaultRoute.DISCONNECT_ROUTE) {
    public <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request) {

      session.completeDisconnect();

      return new Packet<>(PacketType.RESPONSE, request.getSessionId(), route, constructDisconnectSuccessResponse(server, route.getPath(), request.getId(), request.getSessionId()));
    }

    /**
     * Builds the disconnect success response with {@link Reconnect#NONE} advice, signaling the client
     * that no further reconnection is expected.
     *
     * @param server    server supplying the codec
     * @param path      {@code /meta/disconnect} channel path written to the response
     * @param id        request message id echoed in the response
     * @param sessionId client session identifier written to the response
     * @param <V>       value type
     * @return success response message with {@code reconnect: none} advice
     */
    private <V extends Value<V>> Message<V> constructDisconnectSuccessResponse (Server<V> server, String path, String id, String sessionId) {

      return constructSuccessResponse(server, path, id, sessionId, Reconnect.NONE);
    }
  },

  /**
   * Handles {@code /meta/subscribe}: validates session state, rejects subscriptions to meta channels,
   * enforces security policy for channel creation and subscription, creates the channel on demand when
   * permitted, and adds the session as a subscriber.  Supports implicit connection when the server allows it.
   */
  SUBSCRIBE(DefaultRoute.SUBSCRIBE_ROUTE) {
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
     * Builds a subscribe success response that echoes the {@code subscription} field back to the client.
     *
     * @param server       server supplying the codec
     * @param path         {@code /meta/subscribe} channel path written to the response
     * @param id           request message id echoed in the response
     * @param sessionId    client session identifier written to the response
     * @param subscription the channel path that was successfully subscribed
     * @param <V>          value type
     * @return success response message containing the confirmed subscription path
     */
    private <V extends Value<V>> Message<V> constructSubscribeSuccessResponse (Server<V> server, String path, String id, String sessionId, String subscription) {

      return (Message<V>)constructSuccessResponse(server, path, id, sessionId, null).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Builds a subscribe error response that includes both the error description and the
     * target subscription path so the client can correlate failures to requests.
     *
     * @param server       server supplying the codec
     * @param path         {@code /meta/subscribe} channel path written to the response
     * @param id           request message id echoed in the response
     * @param sessionId    client session identifier written to the response
     * @param error        human-readable description of why the subscription was rejected
     * @param subscription the channel path the client attempted to subscribe to, may be {@code null}
     * @param reconnect    reconnect advice to include, or {@code null} for none
     * @param <V>          value type
     * @return error response message containing the subscription path and optional reconnect advice
     */
    private <V extends Value<V>> Message<V> constructSubscribeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, String subscription, Reconnect reconnect) {

      return (Message<V>)constructErrorResponse(server, path, id, sessionId, error, reconnect).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Extracts the subscription channel path from the subscribe request.
     *
     * @param request the incoming subscribe message from which the {@code subscription} field is read
     * @param <V>     value type
     * @return the subscription channel path string, or {@code null} if the field is absent or not a string
     */
    private <V extends Value<V>> String getSubscription (Message<V> request) {

      Value<V> subscriptionValue;

      return (((subscriptionValue = request.get(Message.SUBSCRIPTION)) != null) && ValueType.STRING.equals(subscriptionValue.getType())) ? ((StringValue<V>)subscriptionValue).asText() : null;
    }
  },

  /**
   * Handles {@code /meta/unsubscribe}: validates session state, rejects unsubscription from meta
   * channels, removes the session from the target channel's subscriber list if it exists,
   * and always returns a success response (absent channels are silently ignored).
   */
  UNSUBSCRIBE(DefaultRoute.UNSUBSCRIBE_ROUTE) {
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
     * Builds an unsubscribe success response that echoes the subscription channel back to the client.
     *
     * @param server       server supplying the codec
     * @param path         {@code /meta/unsubscribe} channel path written to the response
     * @param id           request message id echoed in the response
     * @param sessionId    client session identifier written to the response
     * @param subscription the channel path from which the session was unsubscribed
     * @param <V>          value type
     * @return success response message containing the confirmed subscription path
     */
    private <V extends Value<V>> Message<V> constructUnsubscribeSuccessResponse (Server<V> server, String path, String id, String sessionId, String subscription) {

      return (Message<V>)constructSuccessResponse(server, path, id, sessionId, null).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Builds an unsubscribe error response that includes the subscription path so the client can
     * correlate the failure to its original request.
     *
     * @param server       server supplying the codec
     * @param path         {@code /meta/unsubscribe} channel path written to the response
     * @param id           request message id echoed in the response
     * @param sessionId    client session identifier written to the response
     * @param error        human-readable description of why the unsubscription was rejected
     * @param subscription the channel path the client attempted to unsubscribe from, may be {@code null}
     * @param reconnect    reconnect advice to include, or {@code null} for none
     * @param <V>          value type
     * @return error response message containing the subscription path and optional reconnect advice
     */
    private <V extends Value<V>> Message<V> constructUnsubscribeErrorResponse (Server<V> server, String path, String id, String sessionId, String error, String subscription, Reconnect reconnect) {

      return (Message<V>)constructErrorResponse(server, path, id, sessionId, error, reconnect).put(Message.SUBSCRIPTION, subscription);
    }

    /**
     * Extracts the subscription channel path from the unsubscribe request.
     *
     * @param request the incoming unsubscribe message from which the {@code subscription} field is read
     * @param <V>     value type
     * @return the subscription channel path string, or {@code null} if the field is absent or not a string
     */
    private <V extends Value<V>> String getSubscription (Message<V> request) {

      Value<V> subscriptionValue;

      return (((subscriptionValue = request.get(Message.SUBSCRIPTION)) != null) && ValueType.STRING.equals(subscriptionValue.getType())) ? ((StringValue<V>)subscriptionValue).asText() : null;
    }
  },

  /**
   * Handles publish requests to normal (non-meta, non-service) channels: validates session state,
   * enforces security policy for channel creation and publishing, delivers the message to subscribers,
   * and optionally echoes the original message back to the publisher based on the
   * {@code ext.oumuamua.echo} flag.
   */
  PUBLISH(null) {
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
     * Determines whether the publisher wants its own message echoed back in the response.
     * Reads {@code ext.oumuamua.echo}; defaults to {@code true} when the field is absent, matching
     * the CometD convention of echoing by default.
     *
     * @param request the publish message whose {@code ext.oumuamua.echo} field is inspected
     * @param <V>     value type
     * @return {@code true} when the publisher expects an echo or did not express a preference
     */
    private <V extends Value<V>> boolean getEchoFlag (Message<V> request) {

      ObjectValue<V> ext;
      ObjectValue<V> oumuamua;
      BooleanValue<V> echo;

      // cometd echoes by default, so that's what we do here
      return ((echo = ((oumuamua = ((ext = request.getExt()) == null) ? null : (ObjectValue<V>)ext.get("oumuamua")) == null) ? null : (BooleanValue<V>)oumuamua.get("echo")) == null) || echo.asBoolean();
    }

    /**
     * Constructs the delivery message that is broadcast to channel subscribers, containing only the
     * channel path, request id, and the publisher's payload data.
     *
     * @param server server supplying the codec for message creation
     * @param path   destination channel path written to the delivery message
     * @param id     request message id echoed in the delivery message
     * @param data   the publisher's payload value to broadcast
     * @param <V>    value type
     * @return delivery message ready for broadcasting to subscribers
     */
    private <V extends Value<V>> Message<V> constructDeliveryMessage (Server<V> server, String path, String id, Value<V> data) {

      return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.DATA, data);
    }

    /**
     * Builds the publish acknowledgment response sent to the publisher on successful delivery.
     *
     * @param server    server supplying the codec
     * @param path      channel path where the message was published
     * @param id        request message id echoed in the response
     * @param sessionId publisher's session identifier written to the response
     * @param <V>       value type
     * @return success response message confirming the publish
     */
    private <V extends Value<V>> Message<V> constructPublishSuccessResponse (Server<V> server, String path, String id, String sessionId) {

      return constructSuccessResponse(server, path, id, sessionId, null);
    }

    /**
     * Builds an error response for a rejected or failed publish attempt.
     *
     * @param server    server supplying the codec
     * @param path      the channel path to which publication was attempted
     * @param id        request message id echoed in the response
     * @param sessionId publisher's session identifier written to the response
     * @param error     human-readable explanation of why the publish failed
     * @param reconnect reconnect advice to include, or {@code null} for none
     * @param <V>       value type
     * @return error response message with optional reconnect advice
     */
    private <V extends Value<V>> Message<V> constructPublishErrorResponse (Server<V> server, String path, String id, String sessionId, String error, Reconnect reconnect) {

      return constructErrorResponse(server, path, id, sessionId, error, reconnect);
    }
  },
  /**
   * Handles requests to {@code /service/**} channels by locating the registered {@link BayeuxService}
   * for the route and delegating processing to it; returns an error if no service is registered.
   */
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
   * Binds the constant to the meta-channel {@link Route} it handles.
   *
   * @param route the fixed route for this meta command, or {@code null} for {@code PUBLISH} and {@code SERVICE}
   *              which operate on dynamic routes
   */
  Meta (Route route) {

    this.route = route;
  }

  /**
   * Builds a base success response with {@code successful: true} and optional reconnect advice,
   * shared by all meta-channel success paths.
   *
   * @param server    server supplying the codec for message creation
   * @param path      channel path written to the response
   * @param id        request message id echoed in the response
   * @param sessionId client session identifier written to the response
   * @param reconnect reconnect advice to embed in an {@code advice} object, or {@code null} to omit
   * @param <V>       value type
   * @return success response message with channel, id, session, and optional advice populated
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
   * Builds a base error response with {@code successful: false}, the given error string, and optional
   * reconnect advice; accessible to external callers such as service implementations.
   *
   * @param server    server supplying the codec for message creation
   * @param path      channel path written to the response
   * @param id        request message id echoed in the response
   * @param sessionId client session identifier written to the response
   * @param error     human-readable description of the error condition
   * @param reconnect reconnect advice to embed in an {@code advice} object, or {@code null} to omit
   * @param <V>       value type
   * @return error response message with channel, id, session, error, and optional advice populated
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
   * Creates the minimal response skeleton pre-populated with the channel path, message id, and
   * session id; all meta response builders call this method before adding type-specific fields.
   *
   * @param server    server supplying the codec used to allocate the new message
   * @param path      channel path to write into the {@code channel} field
   * @param id        request message id to echo into the {@code id} field
   * @param sessionId client session identifier to write into the {@code clientId} field
   * @param <V>       value type
   * @return bare response message with channel, id, and clientId set
   */
  private static <V extends Value<V>> Message<V> constructResponse (Server<V> server, String path, String id, String sessionId) {

    return (Message<V>)server.getCodec().create().put(Message.CHANNEL, path).put(Message.ID, id).put(Message.SESSION_ID, sessionId);
  }

  /**
   * Resolves the appropriate {@link Meta} constant for the given channel path.
   * Returns one of the five fixed meta commands for standard meta paths, {@link #SERVICE} for
   * {@code /service/**} paths, and {@link #PUBLISH} for all other channel paths.
   *
   * @param path the Bayeux channel path from the incoming message's {@code channel} field
   * @return the {@link Meta} constant responsible for handling this path
   * @throws MetaProcessingException if {@code path} is {@code null} or targets an unrecognized meta channel
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
   * Returns the fixed meta-channel route for this command.
   *
   * @return the {@link Route} bound at construction, or {@code null} for {@code PUBLISH} and {@code SERVICE}
   */
  public Route getRoute () {

    return route;
  }

  /**
   * Executes the full server-side handling logic for one incoming Bayeux message and returns
   * the response packet to send back to the client.
   *
   * @param protocol the protocol that received the message and provides transport context
   * @param route    the resolved channel route for the message
   * @param server   the server instance providing session registry, security policy, and codec
   * @param session  the client session that sent {@code request}
   * @param request  the incoming Bayeux message to process
   * @param <V>      value type
   * @return response packet containing one or more response messages
   * @throws InterruptedException if a long-poll wait inside {@link #CONNECT} is interrupted
   * @throws InvalidPathException if the message targets a channel path that violates path constraints
   */
  public abstract <V extends Value<V>> Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws InterruptedException, InvalidPathException;
}
