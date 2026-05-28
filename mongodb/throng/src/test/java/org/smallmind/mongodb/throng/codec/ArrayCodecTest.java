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
package org.smallmind.mongodb.throng.codec;

import com.mongodb.MongoClientSettings;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ArrayCodecTest {

  private static final CodecRegistry REGISTRY = MongoClientSettings.getDefaultCodecRegistry();

  public void testGetEncoderClassReturnsConfiguredArrayClass () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), false);

    Assert.assertEquals(codec.getEncoderClass(), String[].class);
  }

  public void testEncodeStringArrayProducesBsonArrayWithMatchingElements () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), false);
    BsonDocument backing = new BsonDocument();
    BsonDocumentWriter writer = new BsonDocumentWriter(backing);

    writer.writeStartDocument();
    writer.writeName("items");
    codec.encode(writer, new String[] {"alpha", "beta", "gamma"}, EncoderContext.builder().build());
    writer.writeEndDocument();

    BsonArray array = backing.getArray("items");

    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(((BsonString)array.get(0)).getValue(), "alpha");
    Assert.assertEquals(((BsonString)array.get(2)).getValue(), "gamma");
  }

  public void testEncodeNullArrayWithStoreNullsFalseWritesNothing () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), false);
    BsonDocument backing = new BsonDocument();
    BsonDocumentWriter writer = new BsonDocumentWriter(backing);

    writer.writeStartDocument();
    codec.encode(writer, null, EncoderContext.builder().build());
    writer.writeEndDocument();

    Assert.assertTrue(backing.isEmpty());
  }

  public void testEncodeNullArrayWithStoreNullsTrueWritesBsonNull () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), true);
    BsonDocument backing = new BsonDocument();
    BsonDocumentWriter writer = new BsonDocumentWriter(backing);

    writer.writeStartDocument();
    writer.writeName("items");
    codec.encode(writer, null, EncoderContext.builder().build());
    writer.writeEndDocument();

    Assert.assertTrue(backing.isNull("items"));
  }

  public void testDecodeBsonArrayReturnsTypedArrayWithElements () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), false);
    BsonArray array = new BsonArray();

    array.add(new BsonString("one"));
    array.add(new BsonString("two"));

    BsonDocument backing = new BsonDocument().append("items", array);
    BsonDocumentReader reader = new BsonDocumentReader(backing);

    reader.readStartDocument();
    reader.readName();
    Assert.assertEquals(reader.getCurrentBsonType(), BsonType.ARRAY);

    String[] result = codec.decode(reader, DecoderContext.builder().build());

    Assert.assertEquals(result.length, 2);
    Assert.assertEquals(result[0], "one");
    Assert.assertEquals(result[1], "two");
  }

  public void testDecodeBsonNullReturnsNullArray () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), false);
    BsonDocument backing = new BsonDocument().append("items", BsonNull.VALUE);
    BsonDocumentReader reader = new BsonDocumentReader(backing);

    reader.readStartDocument();
    reader.readName();

    String[] result = codec.decode(reader, DecoderContext.builder().build());

    Assert.assertNull(result);
  }

  public void testEncodeDecodeRoundTripPreservesElements () {

    ArrayCodec<String[]> codec = new ArrayCodec<>(String[].class, String.class, REGISTRY.get(String.class), false);
    BsonDocument backing = new BsonDocument();
    BsonDocumentWriter writer = new BsonDocumentWriter(backing);

    writer.writeStartDocument();
    writer.writeName("items");
    codec.encode(writer, new String[] {"red", "green", "blue"}, EncoderContext.builder().build());
    writer.writeEndDocument();

    BsonDocumentReader reader = new BsonDocumentReader(backing);

    reader.readStartDocument();
    reader.readName();

    String[] result = codec.decode(reader, DecoderContext.builder().build());

    Assert.assertEquals(result, new String[] {"red", "green", "blue"});
  }
}
