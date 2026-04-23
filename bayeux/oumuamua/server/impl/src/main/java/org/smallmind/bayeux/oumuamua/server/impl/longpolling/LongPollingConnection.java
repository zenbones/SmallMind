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
package org.smallmind.bayeux.oumuamua.server.impl.longpolling;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import jakarta.servlet.AsyncContext;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaConnection;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaServer;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link OumuamuaConnection} for the long-polling transport that writes Bayeux responses
 * directly to servlet {@link AsyncContext} instances rather than maintaining a persistent
 * socket.
 *
 * @param <V> the concrete {@link Value} type used by the server's JSON codec
 */
public class LongPollingConnection<V extends Value<V>> implements OumuamuaConnection<V> {

  private final LongPollingTransport<V> longPollingTransport;
  private final OumuamuaServer<V> server;
  private final String connectionId;

  /**
   * Constructs a connection associated with the given transport and server, generating a
   * unique snowflake-encoded connection identifier.
   *
   * @param longPollingTransport the {@link LongPollingTransport} that owns this connection
   * @param server               the hosting {@link OumuamuaServer}
   */
  public LongPollingConnection (LongPollingTransport<V> longPollingTransport, OumuamuaServer<V> server) {

    this.longPollingTransport = longPollingTransport;
    this.server = server;

    connectionId = SnowflakeId.newInstance().generateHexEncoding();
  }

  /**
   * Returns the unique hex-encoded snowflake identifier assigned at construction.
   *
   * @return the connection's unique identifier
   */
  @Override
  public String getId () {

    return connectionId;
  }

  /**
   * Returns the {@link LongPollingTransport} that owns this connection.
   *
   * @return the owning long-polling transport
   */
  @Override
  public Transport<V> getTransport () {

    return longPollingTransport;
  }

  /**
   * Not supported; long-polling connections write responses through {@link AsyncContext}
   * instances rather than via a persistent channel.
   *
   * @param packet the packet that cannot be delivered through this path
   * @throws UnsupportedOperationException always
   */
  @Override
  public void deliver (Packet<V> packet) {

    throw new UnsupportedOperationException();
  }

  /**
   * Encodes a packet, writes it to the response output stream of the given async context,
   * flushes the buffer, and notifies the protocol of the delivery.
   *
   * @param asyncContext the servlet {@link AsyncContext} whose response receives the packet
   * @param packet       the {@link Packet} to encode and write
   * @throws IOException if writing to or flushing the response output stream fails
   */
  private void emit (AsyncContext asyncContext, Packet<V> packet)
    throws IOException {

    String encodedPacket = PacketUtility.encode(packet);

    LoggerManager.getLogger(LongPollingConnection.class).log(server.getMessageLogLevel(), () -> "=>" + encodedPacket);

    asyncContext.getResponse().getOutputStream().print(encodedPacket);
    asyncContext.getResponse().flushBuffer();

    ((ServletProtocol<V>)longPollingTransport.getProtocol()).onDelivery(packet);
  }

  /**
   * Processes an array of decoded inbound Bayeux messages and writes the corresponding
   * response(s) to the async context. Single-message requests are emitted individually;
   * multi-message batches are collected and emitted as a single response packet. The async
   * context is always completed in the finally block regardless of outcome.
   *
   * @param asyncContext the servlet {@link AsyncContext} for the current poll request
   * @param messages     the decoded inbound {@link Message} array; may be {@code null} or empty
   */
  public void onMessages (AsyncContext asyncContext, Message<V>[] messages) {

    try {
      if ((messages != null) && (messages.length > 0)) {
        if (messages.length == 1) {
          process(server, (session, packet) -> {
            try {
              emit(asyncContext, packet);
            } catch (IOException ioException) {
              LoggerManager.getLogger(LongPollingConnection.class).error(ioException);
            }
          }, messages);
        } else {

          LinkedList<Message<V>> batchList = new LinkedList<>();

          process(server, (session, packet) -> batchList.addAll(Arrays.asList(packet.getMessages())), messages);

          try {
            emit(asyncContext, new Packet<>(PacketType.RESPONSE, null, null, batchList.toArray(new Message[0])));
          } catch (IOException ioException) {
            LoggerManager.getLogger(LongPollingConnection.class).error(ioException);
          }
        }
      }
    } finally {
      asyncContext.complete();
    }
  }

  /**
   * No-op implementation; long-polling connections hold no persistent resources requiring
   * cleanup.
   */
  @Override
  public void onCleanup () {

  }
}
