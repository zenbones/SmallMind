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

import java.lang.reflect.InvocationTargetException;
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
 * Generic codec that encodes and decodes objects based on {@link ThrongProperties} metadata.
 *
 * @param <T> type being encoded/decoded
 */
public class ThrongPropertiesCodec<T> implements Codec<T>, IndexProvider {

  private final ThrongProperties<T> throngProperties;

  /**
   * @param throngProperties metadata describing fields and codecs for the type
   */
  public ThrongPropertiesCodec (ThrongProperties<T> throngProperties) {

    this.throngProperties = throngProperties;
  }

  @Override
  /**
   * {@inheritDoc}
   */
  public Class<T> getEncoderClass () {

    return throngProperties.getEntityClass();
  }

  /**
   * @return whether null values should be stored when encoding
   */
  public boolean isStoreNulls () {

    return throngProperties.isStoreNulls();
  }

  @Override
  /**
   * {@inheritDoc}
   */
  public ThrongIndexes provideIndexes () {

    return throngProperties.provideIndexes();
  }

  @Override
  /**
   * Decodes a BSON document into an instance of the mapped class using configured property codecs.
   *
   * @throws ThrongRuntimeException if the instance cannot be constructed or fields cannot be set
   */
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
      reader.readNull();

      return null;
    } else {
      try {

        T instance = throngProperties.getEntityClass().getConstructor().newInstance();

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

          ThrongProperty throngProperty;

          if ((throngProperty = throngProperties.getByPropertyName(reader.readName())) != null) {
            if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
              reader.readNull();
              throngProperty.getFieldAccessor().set(instance, null);
            } else {
              throngProperty.getFieldAccessor().set(instance, throngProperty.getCodec().decode(reader, decoderContext));
            }
          }
        }

        return instance;
      } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
        throw new ThrongRuntimeException(exception);
      }
    }
  }

  @Override
  /**
   * Encodes an object by iterating its mapped properties and writing names/values to the writer.
   *
   * @throws ThrongRuntimeException if property values cannot be accessed
   */
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    for (ThrongProperty throngProperty : throngProperties.values()) {
      try {

        Object propertyValue;

        if (((propertyValue = throngProperty.getFieldAccessor().get(value)) != null) || throngProperties.isStoreNulls()) {
          writer.writeName(throngProperty.getName());

          if (propertyValue == null) {
            writer.writeNull();
          } else {
            reEncode(writer, throngProperty.getCodec(), propertyValue, encoderContext);
          }
        }
      } catch (IllegalAccessException | InvocationTargetException exception) {
        throw new ThrongRuntimeException(exception);
      }
    }
  }

  // Due to the fact that object is not of type 'capture of ?'

  /**
   * Performs encoding of a value with the given codec, avoiding wildcard capture issues.
   *
   * @param writer         destination writer
   * @param codec          codec to use for encoding
   * @param stuff          value to encode
   * @param encoderContext encoder context
   * @param <U>            value type
   */
  protected <U> void reEncode (BsonWriter writer, Codec<U> codec, Object stuff, EncoderContext encoderContext) {

    codec.encode(writer, (U)stuff, encoderContext);
  }
}
