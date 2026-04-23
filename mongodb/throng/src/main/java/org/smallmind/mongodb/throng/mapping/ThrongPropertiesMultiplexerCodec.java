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
package org.smallmind.mongodb.throng.mapping;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.smallmind.mongodb.throng.ThrongRuntimeException;
import org.smallmind.mongodb.throng.index.IndexProvider;
import org.smallmind.mongodb.throng.index.ThrongIndexes;

/**
 * Codec that encodes/decodes polymorphic types using a discriminator key and a {@link ThrongPropertiesMultiplexer}.
 *
 * @param <T> base type of the polymorphic hierarchy
 */
public class ThrongPropertiesMultiplexerCodec<T> implements Codec<T>, IndexProvider {

  private final ThrongPropertiesMultiplexer<T> throngPropertiesMultiplexer;

  /**
   * Constructs the codec from the given polymorphic multiplexer.
   *
   * @param throngPropertiesMultiplexer the multiplexer that maps discriminator values to subtype codecs
   */
  public ThrongPropertiesMultiplexerCodec (ThrongPropertiesMultiplexer<T> throngPropertiesMultiplexer) {

    this.throngPropertiesMultiplexer = throngPropertiesMultiplexer;
  }

  /**
   * Returns the base class of the polymorphic hierarchy.
   *
   * @return the base entity class handled by this codec
   */
  @Override
  public Class<T> getEncoderClass () {

    return throngPropertiesMultiplexer.getEntityClass();
  }

  /**
   * Returns whether null values are written as BSON null rather than omitted during encoding.
   *
   * @return {@code true} if null values are persisted
   */
  public boolean isStoreNulls () {

    return throngPropertiesMultiplexer.isStoreNulls();
  }

  /**
   * Returns the combined index definitions from all registered subtype codecs.
   *
   * @return merged index definitions for the polymorphic hierarchy
   */
  @Override
  public ThrongIndexes provideIndexes () {

    return throngPropertiesMultiplexer.provideIndexes();
  }

  /**
   * Decodes a polymorphic value by reading the discriminator key field and delegating to the matching subtype codec.
   *
   * @param reader         BSON reader positioned within the document
   * @param decoderContext decoder context
   * @return decoded instance cast to the base type, or {@code null} when the value is null
   * @throws ThrongRuntimeException if the discriminator field name does not match the expected key, or no codec is
   *                                registered for the discriminator value
   */
  @Override
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
      reader.readNull();

      return null;
    } else {

      String polymorphicKey;

      if (!throngPropertiesMultiplexer.getKey().equals(polymorphicKey = reader.readName())) {
        throw new ThrongRuntimeException("The expected polymorphic key field(%s) does not match the actual(%s)", throngPropertiesMultiplexer.getKey(), polymorphicKey);
      } else {

        T instance;

        instance = throngPropertiesMultiplexer.getEntityClass().cast(throngPropertiesMultiplexer.getCodec(reader.readString()).decode(reader, decoderContext));

        return instance;
      }
    }
  }

  /**
   * Encodes a polymorphic value by writing the discriminator key followed by the subtype properties.
   *
   * @param writer         destination BSON writer
   * @param value          polymorphic instance to encode
   * @param encoderContext encoder context
   */
  @Override
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    if ((value != null) || throngPropertiesMultiplexer.isStoreNulls()) {
      if (value == null) {
        writer.writeNull();
      } else {
        writer.writeName(throngPropertiesMultiplexer.getKey());
        writer.writeString(value.getClass().getName());

        reEncode(writer, throngPropertiesMultiplexer.getCodec(value.getClass().getName()), value, encoderContext);
      }
    }
  }

  // Due to the fact that object is not of type 'capture of ?'

  /**
   * Re-encodes a value with the provided codec, avoiding wildcard capture issues.
   *
   * @param writer         destination writer
   * @param codec          codec to use
   * @param stuff          value to encode
   * @param encoderContext encoder context
   * @param <U>            value type
   */
  protected <U> void reEncode (BsonWriter writer, Codec<U> codec, Object stuff, EncoderContext encoderContext) {

    codec.encode(writer, (U)stuff, encoderContext);
  }
}
