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
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.CloseReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.OumuamuaServerSession;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.context.OumuamuaServletContext;
import org.smallmind.cometd.oumuamua.extension.ExtensionNotifier;
import org.smallmind.cometd.oumuamua.message.NodeMessageGenerator;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class LongPollingCarrier implements OumuamuaCarrier {

  private static final String[] ACTUAL_TRANSPORTS = new String[] {"long-polling"};
  private final OumuamuaServer oumuamuaServer;
  private final OumuamuaServletContext context;
  private final LongPollingTransport longPollingTransport;
  private final AsyncContext asyncContext;
  private OumuamuaServerSession serverSession;
  private boolean connected;

  public LongPollingCarrier (OumuamuaServer oumuamuaServer, LongPollingTransport longPollingTransport, AsyncContext asyncContext)
    throws IOException {

    this.oumuamuaServer = oumuamuaServer;
    this.longPollingTransport = longPollingTransport;
    this.asyncContext = asyncContext;

    context = new OumuamuaServletContext((HttpServletRequest)asyncContext.getRequest());
    asyncContext.getResponse().getOutputStream().setWriteListener();
  }

  @Override
  public CarrierType getType () {

    return CarrierType.LONG_POLLING;
  }

  @Override
  public String[] getActualTransports () {

    return ACTUAL_TRANSPORTS;
  }

  public void setServerSession (OumuamuaServerSession serverSession) {

    this.serverSession = serverSession;
    setConnected(true);
  }

  @Override
  public BayeuxContext getContext () {

    return context;
  }

  @Override
  public String getUserAgent () {

    return ((HttpServletRequest)asyncContext.getRequest()).getHeader("User-Agent");
  }

  @Override
  public void setMaxSessionIdleTimeout (long maxSessionIdleTimeout) {

    long adjustedIdleTimeout = (maxSessionIdleTimeout >= 0) ? maxSessionIdleTimeout : longPollingTransport.getMaxInterval();

    asyncContext.setTimeout(Math.max(adjustedIdleTimeout, 0));
  }

  @Override
  public synchronized boolean isConnected () {

    return connected;
  }

  @Override
  public synchronized void setConnected (boolean connected) {

    this.connected = connected;
  }

  @Override
  public synchronized void send (OumuamuaPacket... packets)
    throws Exception {

  }

  @Override
  public synchronized OumuamuaPacket[] inject (ObjectNode messageNode)
    throws JsonProcessingException {

    return new OumuamuaPacket[0];
  }

  public synchronized void onMessage (JsonNode messageConglomerate) {

    if ((serverSession != null) && isConnected()) {
      try {
        for (JsonNode messageNode : messageConglomerate) {
          if (JsonNodeType.OBJECT.equals(messageNode.getNodeType()) && messageNode.has(Message.CHANNEL_FIELD)) {

            String channel = messageNode.get(Message.CHANNEL_FIELD).asText();
            ChannelId channelId = ChannelIdCache.generate(channel);

            if (ExtensionNotifier.incoming(oumuamuaServer, serverSession, new NodeMessageGenerator(context, longPollingTransport, channelId, (ObjectNode)messageNode, false))) {

              OumuamuaPacket[] packets;

              if ((packets = respond(oumuamuaServer, context, longPollingTransport, serverSession, channelId, channelId.getId(), (ObjectNode)messageNode)) != null) {
                send(packets);
              }
            } else {
              send(createErrorPacket(serverSession, channelId, channel, messageNode, "Processing was denied"));
            }
          }
        }

        // handle the disconnect after sending the confirmation
        if (!isConnected()) {
          websocketSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client disconnect"));
        }
      } catch (Exception ioException) {
        LoggerManager.getLogger(WebSocketEndpoint.class).error(ioException);
      } finally {
        // Keep our threads clean and tidy
        ChannelIdCache.clear();
      }
    }
  }

  @Override
  public synchronized void close ()
    throws IOException {

  }
}
