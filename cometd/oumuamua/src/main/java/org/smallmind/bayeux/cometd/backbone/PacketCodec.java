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
package org.smallmind.bayeux.cometd.backbone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.ChannelId;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.bayeux.cometd.channel.ChannelIdCache;
import org.smallmind.bayeux.cometd.message.MapLike;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.bayeux.cometd.session.OumuamuaServerSession;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class PacketCodec {

  public static byte[] encode (OumuamuaServerSession sender, OumuamuaPacket packet)
    throws IOException {

    ByteArrayOutputStream byteArrayOutputStream;

    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream = new ByteArrayOutputStream())) {

      MapLike[] messages;

      objectOutputStream.writeUTF(sender.getId());
      objectOutputStream.writeUTF(sender.getServerTransport().getName());

      objectOutputStream.writeUTF(packet.getChannelId().getId());

      if ((messages = packet.getMessages()) == null) {
        objectOutputStream.writeInt(0);
      } else {
        objectOutputStream.writeInt(messages.length);

        for (MapLike message : messages) {
          objectOutputStream.writeUTF(message.encode());
        }
      }
    }

    return byteArrayOutputStream.toByteArray();
  }

  public static OumuamuaPacket decode (OumuamuaServer oumuamuaServer, byte[] buffer)
    throws IOException {

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

    MapLike[] messaages;
    ChannelId channelId;
    String sessionId;
    String transportName;
    int messageCount = objectInputStream.readInt();

    sessionId = objectInputStream.readUTF();
    transportName = objectInputStream.readUTF();

    channelId = ChannelIdCache.generate(objectInputStream.readUTF());

    messaages = new MapLike[messageCount];

    for (int index = 0; index < messageCount; index++) {
      messaages[index++] = new MapLike((ObjectNode)JsonCodec.readAsJsonNode(objectInputStream.readUTF()));
    }

    return new OumuamuaPacket(new ClusteredServerSession(oumuamuaServer, transportName, sessionId), channelId, messaages);
  }
}
