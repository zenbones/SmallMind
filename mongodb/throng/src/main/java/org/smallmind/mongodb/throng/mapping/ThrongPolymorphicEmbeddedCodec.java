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
package org.smallmind.mongodb.throng.mapping;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.smallmind.mongodb.throng.ThrongRuntimeException;

public class ThrongPolymorphicEmbeddedCodec<T> implements Codec<T> {

  private final ThrongPolymorphicMultiplexer<T> polymorphicMultiplexer;

  public ThrongPolymorphicEmbeddedCodec (ThrongPolymorphicMultiplexer<T> polymorphicMultiplexer) {

    this.polymorphicMultiplexer = polymorphicMultiplexer;
  }

  @Override
  public Class<T> getEncoderClass () {

    return polymorphicMultiplexer.getEntityClass();
  }

  public boolean isStoreNulls () {

    return polymorphicMultiplexer.isStoreNulls();
  }

  @Override
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    String polymorphicKey;

    reader.readStartDocument();

    if (!polymorphicMultiplexer.getKey().equals(polymorphicKey = reader.readName())) {
      throw new ThrongRuntimeException("The expected polymorphic key field(%s) does not match the actual(%s)", polymorphicMultiplexer.getKey(), polymorphicKey);
    } else {

      T instance;

      instance = polymorphicMultiplexer.getEntityClass().cast(polymorphicMultiplexer.getCodec(reader.readString()).decode(reader, decoderContext));
      reader.readEndDocument();

      return instance;
    }
  }

  @Override
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    if (value != null) {
      writer.writeStartDocument();
      writer.writeName(polymorphicMultiplexer.getKey());
      writer.writeString(value.getClass().getName());
      reEncode(writer, polymorphicMultiplexer.getCodec(value.getClass().getName()), value, encoderContext);
      writer.writeEndDocument();
    } else if (isStoreNulls()) {
      writer.writeNull();
    }
  }

  // Due to the fact that object is not of type 'capture of ?'
  private <U> void reEncode (BsonWriter writer, Codec<U> codec, Object stuff, EncoderContext encoderContext) {

    codec.encode(writer, (U)stuff, encoderContext);
  }
}
