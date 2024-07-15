/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class LongPollingConnection<V extends Value<V>> implements OumuamuaConnection<V> {

  private final LongPollingTransport<V> longPollingTransport;
  private final OumuamuaServer<V> server;
  private final String connectionId;

  public LongPollingConnection (LongPollingTransport<V> longPollingTransport, OumuamuaServer<V> server) {

    this.longPollingTransport = longPollingTransport;
    this.server = server;

    connectionId = SnowflakeId.newInstance().generateHexEncoding();
  }

  @Override
  public String getId () {

    return connectionId;
  }

  @Override
  public Transport<V> getTransport () {

    return longPollingTransport;
  }

  @Override
  public void deliver (Packet<V> packet) {

    throw new UnsupportedOperationException();
  }

  private void emit (AsyncContext asyncContext, Packet<V> packet)
    throws IOException {

    String encodedPacket = PacketUtility.encode(packet);

    LoggerManager.getLogger(LongPollingConnection.class).debug(() -> "=>" + encodedPacket);

    asyncContext.getResponse().getOutputStream().print(encodedPacket);
    asyncContext.getResponse().flushBuffer();
  }

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

  @Override
  public void onCleanUp () {

  }
}
