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
package org.smallmind.bayeux.oumuamua.server.spi.backbone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.nutsnbolts.util.Bytes;

/**
 * Serialization helpers for encoding and decoding {@link Packet} instances transmitted over
 * a backbone transport; each record includes the originating node name, the channel route path,
 * and the JSON-encoded messages, each preceded by a 4-byte big-endian length prefix.
 */
public class RecordUtility {

  /**
   * Encodes a packet into a self-delimiting byte array suitable for backbone transmission.
   *
   * <p>The layout is: {@code [4-byte nodeNameLen][nodeName][4-byte pathLen][path][4-byte payloadLen][payload]}.
   *
   * @param nodeName unique identifier of the cluster node emitting the packet
   * @param packet   packet to encode; its route's path and messages are both included
   * @param <V>      value type carried in the packet
   * @return byte array containing the length-prefixed record
   * @throws IOException if message encoding or stream writing fails
   */
  public static <V extends Value<V>> byte[] serialize (String nodeName, Packet<V> packet)
    throws IOException {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    String encodedPacket = PacketUtility.encode(packet);
    byte[] nodeNameBytes = nodeName.getBytes(StandardCharsets.UTF_8);
    byte[] pathBytes = packet.getRoute().getPath().getBytes(StandardCharsets.UTF_8);
    byte[] encodedPacketBytes = encodedPacket.getBytes(StandardCharsets.UTF_8);

    byteArrayOutputStream.write(Bytes.getBytes(nodeNameBytes.length));
    byteArrayOutputStream.write(nodeNameBytes);
    byteArrayOutputStream.write(Bytes.getBytes(pathBytes.length));
    byteArrayOutputStream.write(pathBytes);
    byteArrayOutputStream.write(Bytes.getBytes(encodedPacketBytes.length));
    byteArrayOutputStream.write(encodedPacketBytes);

    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Decodes a byte array produced by {@link #serialize(String, Packet)} back into a
   * {@link DebonedPacket}, annotating each decoded message with a {@code backbone} ext field
   * indicating the record originated from a remote Kafka node.
   *
   * @param codec  codec used to decode the JSON message payload
   * @param buffer serialized record bytes in the format written by {@link #serialize}
   * @param <V>    value type carried in the packet
   * @return a {@link DebonedPacket} containing the source node name and the reconstructed packet
   * @throws IOException          if stream reading or message decoding fails
   * @throws InvalidPathException if the path encoded in the record is not a valid channel path
   */
  public static <V extends Value<V>> DebonedPacket<V> deserialize (Codec<V> codec, byte[] buffer)
    throws IOException, InvalidPathException {

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    byte[] lengthBuffer = new byte[Integer.BYTES];
    String nodeName = new String(readRecordBuffer(byteArrayInputStream, lengthBuffer), StandardCharsets.UTF_8);
    String path = new String(readRecordBuffer(byteArrayInputStream, lengthBuffer), StandardCharsets.UTF_8);
    byte[] encodedPacketBuffer = readRecordBuffer(byteArrayInputStream, lengthBuffer);
    Message<V>[] decodedMessages = codec.from(encodedPacketBuffer);

    for (Message<V> decodedMessage : decodedMessages) {

      ObjectValue<V> backboneValue = decodedMessage.getFactory().objectValue();

      backboneValue.put("remote", true);
      backboneValue.put("type", "kafka");

      decodedMessage.getExt(true).put("backbone", backboneValue);
    }

    return new DebonedPacket<>(nodeName, new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(path), decodedMessages));
  }

  /**
   * Reads one length-prefixed field from {@code byteArrayInputStream}, reusing {@code lengthBuffer}
   * to avoid allocating a new 4-byte array on each call.
   *
   * @param byteArrayInputStream source stream positioned at the start of a length prefix
   * @param lengthBuffer         4-byte scratch buffer used to read the big-endian length value
   * @return the content bytes that followed the length prefix
   */
  private static byte[] readRecordBuffer (ByteArrayInputStream byteArrayInputStream, byte[] lengthBuffer) {

    byte[] contentBuffer;
    int length;

    byteArrayInputStream.readNBytes(lengthBuffer, 0, Integer.BYTES);
    length = Bytes.getInt(lengthBuffer);
    contentBuffer = new byte[length];
    byteArrayInputStream.readNBytes(contentBuffer, 0, length);

    return contentBuffer;
  }
}
