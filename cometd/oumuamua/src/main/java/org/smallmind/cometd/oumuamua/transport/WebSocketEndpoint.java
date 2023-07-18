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
package org.smallmind.cometd.oumuamua.transport;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.context.OumuamuaWebsocketContext;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.message.OumuamuaServerMessage;
import org.smallmind.cometd.oumuamua.meta.ConnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.DisconnectMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.HandshakeMessage;
import org.smallmind.cometd.oumuamua.meta.HandshakeMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.PublishMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.SubscribeMessage;
import org.smallmind.cometd.oumuamua.meta.SubscribeMessageRequestInView;
import org.smallmind.cometd.oumuamua.meta.UnsubscribeMessageRequestInView;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class WebSocketEndpoint extends Endpoint implements MessageHandler.Whole<String>, OumuamuaCarrier {

  private static final String[] ACTUAL_TRANSPORTS = new String[] {"websocket"};

  private OumuamuaServer oumuamuaServer;
  private WebSocketTransport websocketTransport;
  private Session websocketSession;
  private OumuamuaServerSession serverSession;
  private OumuamuaWebsocketContext context;
  private boolean connected = false;

  @Override
  public void onOpen (Session websocketSession, EndpointConfig config) {

    this.websocketSession = websocketSession;

    oumuamuaServer = (OumuamuaServer)config.getUserProperties().get(BayeuxServer.ATTRIBUTE);
    websocketTransport = (WebSocketTransport)config.getUserProperties().get(WebSocketTransport.ATTRIBUTE);

    if (websocketTransport.getMaxInterval() >= 0) {
      websocketSession.getContainer().setDefaultMaxSessionIdleTimeout(websocketTransport.getMaxInterval());
    }
    if (websocketTransport.getMaximumTextMessageBufferSize() > 0) {
      websocketSession.getContainer().setDefaultMaxTextMessageBufferSize(websocketTransport.getMaximumTextMessageBufferSize());
    }

    websocketSession.addMessageHandler(this);
  }

  public WebSocketEndpoint setStoredHandshakeRequest (StoredHandshakeRequest storedHandshakeRequest) {

    context = new OumuamuaWebsocketContext(storedHandshakeRequest);

    return this;
  }

  @Override
  public String getUserAgent () {

    return null;
  }

  @Override
  public synchronized void send (ServerSession receiver, OumuamuaPacket packet)
    throws IOException, InterruptedException, ExecutionException, TimeoutException {

    StringBuilder sendBuilder = new StringBuilder("[");
    boolean first = true;

    for (MapLike mapLike : packet.getMessages()) {
      if (!first) {
        sendBuilder.append(',');
      }

      sendBuilder.append(mapLike.encode());
      first = false;
    }
    sendBuilder.append(']');

    if (websocketTransport.getAsyncSendTimeoutMilliseconds() > 0) {
      websocketSession.getAsyncRemote().sendText(sendBuilder.toString()).get(websocketTransport.getAsyncSendTimeoutMilliseconds(), TimeUnit.MILLISECONDS);
    } else {
      websocketSession.getBasicRemote().sendText(sendBuilder.toString());
    }

    System.out.println("out:" + sendBuilder.toString());
  }

  @Override
  public synchronized void onMessage (String data) {

    System.out.println("in:" + data);
    try {
      for (JsonNode messageNode : JsonCodec.readAsJsonNode(data)) {
        send(serverSession, new OumuamuaPacket(serverSession, respond(messageNode)));
      }

      // handle disconnect
      if (!connected) {
        websocketSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client disconnect"));
      }
    } catch (Exception ioException) {
      LoggerManager.getLogger(WebSocketEndpoint.class).error(ioException);
    } finally {
      // Keep our threads clean and tidy
      ChannelIdCache.clear();
    }
  }

  private MapLike[] respond (JsonNode messageNode)
    throws JsonProcessingException {

    if (messageNode.has("channel")) {

      String channel;

      switch (channel = messageNode.get("channel").asText()) {
        case "/meta/handshake":

          HandshakeMessageRequestInView handshakeView;

          if (serverSession == null) {
            serverSession = new OumuamuaServerSession(websocketTransport, this);
            connected = true;
          }

          return (handshakeView = JsonCodec.read(messageNode, HandshakeMessageRequestInView.class)).factory().process(oumuamuaServer, ACTUAL_TRANSPORTS, serverSession, new OumuamuaServerMessage(websocketTransport, context, null, HandshakeMessage.CHANNEL_ID, handshakeView.getId(), null, false, (ObjectNode)messageNode));
        case "/meta/connect":

          return JsonCodec.read(messageNode, ConnectMessageRequestInView.class).factory().process(oumuamuaServer, serverSession);
        case "/meta/disconnect":
          // disconnect will happen after the response hs been sent
          connected = false;

          return JsonCodec.read(messageNode, DisconnectMessageRequestInView.class).factory().process();
        case "/meta/subscribe":

          SubscribeMessageRequestInView subscribeView;

          return (subscribeView = JsonCodec.read(messageNode, SubscribeMessageRequestInView.class)).factory().process(oumuamuaServer, serverSession, new OumuamuaServerMessage(websocketTransport, context, null, SubscribeMessage.CHANNEL_ID, subscribeView.getId(), serverSession.getId(), false, (ObjectNode)messageNode));
        case "/meta/unsubscribe":
          return JsonCodec.read(messageNode, UnsubscribeMessageRequestInView.class).factory().process(oumuamuaServer, serverSession);
        default:
          if (channel.startsWith("/meta/")) {

            ObjectNode errorNode = JsonNodeFactory.instance.objectNode();

            errorNode.put("successful", false);
            errorNode.put("channel", channel);
            errorNode.put("error", "Unknown meta channel");

            if (messageNode.has("id")) {
              errorNode.set("id", messageNode.get("id"));
            }
            if (serverSession != null) {
              errorNode.put("clientId", serverSession.getId());
            }

            return JsonCodec.writeAsString(errorNode);
          } else if (channel.startsWith("/service/")) {
            // TODO: service
          } else {

            PublishMessageRequestInView publishView;

            return (publishView = JsonCodec.read(messageNode, PublishMessageRequestInView.class)).factory().process(oumuamuaServer, serverSession, new OumuamuaServerMessage(websocketTransport, context, null, ChannelIdCache.generate(channel), publishView.getId(), serverSession.getId(), false, (ObjectNode)messageNode));
          }
      }
    }

    return null;
  }

  @Override
  public synchronized void onClose (Session wsSession, CloseReason closeReason) {

    connected = false;
    //TODO: destroy the server session!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    serverSession = null;
  }

  @Override
  public synchronized void onError (Session wsSession, Throwable failure) {

    LoggerManager.getLogger(WebSocketEndpoint.class).error(failure);
  }
}
