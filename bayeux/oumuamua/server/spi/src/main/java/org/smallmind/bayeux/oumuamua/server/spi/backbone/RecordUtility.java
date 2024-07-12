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
package org.smallmind.bayeux.oumuamua.server.spi.backbone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;

public class RecordUtility {

  public static <V extends Value<V>> byte[] serialize (String nodeName, Packet<V> packet)
    throws IOException {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeUTF(nodeName);
      objectOutputStream.writeUTF(packet.getRoute().getPath());
      objectOutputStream.writeUTF(PacketUtility.encode(packet));
    }

    return byteArrayOutputStream.toByteArray();
  }

  public static <V extends Value<V>> DebonedPacket<V> deserialize (Codec<V> codec, byte[] buffer)
    throws IOException, InvalidPathException {

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);

    try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

      return new DebonedPacket<>(objectInputStream.readUTF(), new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(objectInputStream.readUTF()), codec.from(objectInputStream.readUTF().getBytes())));
    }
  }
}
