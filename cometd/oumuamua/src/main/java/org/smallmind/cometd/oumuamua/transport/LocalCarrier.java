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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.OumuamuaServer;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.context.OumuamuaLocalContext;
import org.smallmind.cometd.oumuamua.extension.ExtensionNotifier;
import org.smallmind.cometd.oumuamua.logging.DataRecord;
import org.smallmind.cometd.oumuamua.logging.NodeRecord;
import org.smallmind.cometd.oumuamua.logging.PacketRecord;
import org.smallmind.cometd.oumuamua.message.NodeMessageGenerator;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.session.OumuamuaLocalSession;
import org.smallmind.cometd.oumuamua.session.VeridicalServerSession;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class LocalCarrier extends AbstractExpiringCarrier {

  private static final OumuamuaLocalContext LOCAL_CONTEXT = new OumuamuaLocalContext();
  private static final String[] ACTUAL_TRANSPORTS = new String[] {"local"};

  private final OumuamuaServer oumuamuaServer;
  private final LocalTransport localTransport;
  private VeridicalServerSession serverSession;
  private boolean connected;

  public LocalCarrier (OumuamuaServer oumuamuaServer, LocalTransport localTransport, String idHint) {

    super(localTransport.getMaxInterval(), localTransport.getIdleCheckCycleMilliseconds(), 300000);

    this.oumuamuaServer = oumuamuaServer;
    this.localTransport = localTransport;

    oumuamuaServer.addSession(serverSession = new VeridicalServerSession(oumuamuaServer, localTransport, this, true, idHint, oumuamuaServer.getConfiguration().getMaximumMessageQueueSize(), oumuamuaServer.getConfiguration().getMaximumUndeliveredLazyMessageCount()));
    setConnected(true);
  }

  public OumuamuaLocalSession getLocalSession () {

    return (OumuamuaLocalSession)serverSession.getLocalSession();
  }

  @Override
  public CarrierType getType () {

    return CarrierType.LOCAL;
  }

  @Override
  public BayeuxContext getContext () {

    return LOCAL_CONTEXT;
  }

  @Override
  public String[] getActualTransports () {

    return ACTUAL_TRANSPORTS;
  }

  @Override
  public String getUserAgent () {

    return null;
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
    throws IOException {

    if (serverSession != null) {

      String text;

      if ((text = asText(packets)) != null) {
        System.out.println("=>" + text);
        LoggerManager.getLogger(LocalCarrier.class).debug(new DataRecord(text, false));

        ((OumuamuaLocalSession)serverSession.getLocalSession()).receive(text);
      }
    }
  }

  public synchronized OumuamuaPacket[] inject (ObjectNode messageNode)
    throws JsonProcessingException {

    System.out.println("<=" + JsonCodec.writeAsString(messageNode));
    LoggerManager.getLogger(LocalCarrier.class).debug(new NodeRecord(messageNode, true));

    if ((serverSession == null) || (!isConnected()) || (!messageNode.has(Message.CHANNEL_FIELD))) {

      return null;
    } else {
      try {

        String channel = messageNode.get(Message.CHANNEL_FIELD).asText();
        ChannelId channelId = ChannelIdCache.generate(channel);

        updateLastContact();

        if (ExtensionNotifier.incoming(oumuamuaServer, serverSession, new NodeMessageGenerator(LOCAL_CONTEXT, localTransport, channelId, messageNode, false))) {

          OumuamuaPacket[] packets = respond(oumuamuaServer, LOCAL_CONTEXT, localTransport, serverSession, channelId, channelId.getId(), messageNode);

          if (!isConnected()) {
            close();
          }

          System.out.println(new PacketRecord(packets, false));
          LoggerManager.getLogger(LocalCarrier.class).debug(new PacketRecord(packets, false));

          return packets;
        } else {

          return createErrorPacket(serverSession, channelId, channel, messageNode, "Processing was denied");
        }
      } finally {
        // Keep our threads clean and tidy
        ChannelIdCache.clear();
      }
    }
  }

  @Override
  public void finishClosing () {

    if (serverSession != null) {

      OumuamuaLocalSession localSession;

      oumuamuaServer.removeSession(serverSession);
      if ((localSession = (OumuamuaLocalSession)serverSession.getLocalSession()) != null) {
        localSession.stop();
      }

      if (isConnected()) {
        setConnected(false);
        oumuamuaServer.onSessionDisconnected(serverSession, null, true);
      }

      serverSession = null;
    }
  }
}
