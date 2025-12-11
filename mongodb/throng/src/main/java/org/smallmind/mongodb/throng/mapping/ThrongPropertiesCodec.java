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

public class ThrongPropertiesCodec<T> implements Codec<T>, IndexProvider {

  private final ThrongProperties<T> throngProperties;

  public ThrongPropertiesCodec (ThrongProperties<T> throngProperties) {

    this.throngProperties = throngProperties;
  }

  @Override
  public Class<T> getEncoderClass () {

    return throngProperties.getEntityClass();
  }

  public boolean isStoreNulls () {

    return throngProperties.isStoreNulls();
  }

  @Override
  public ThrongIndexes provideIndexes () {

    return throngProperties.provideIndexes();
  }

  @Override
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
  protected <U> void reEncode (BsonWriter writer, Codec<U> codec, Object stuff, EncoderContext encoderContext) {

    codec.encode(writer, (U)stuff, encoderContext);
  }
}
