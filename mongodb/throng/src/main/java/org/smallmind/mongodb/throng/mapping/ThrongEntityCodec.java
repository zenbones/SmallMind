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

import java.lang.reflect.InvocationTargetException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.smallmind.mongodb.throng.ThrongRuntimeException;
import org.smallmind.mongodb.throng.lifecycle.ThrongLifecycle;

public class ThrongEntityCodec<T> extends ThrongPropertiesCodec<T> {

  private final ThrongLifecycle<T> lifecycle;
  private final Class<T> entityClass;
  private final ThrongProperty idProperty;
  private final String collection;

  public ThrongEntityCodec (ThrongEntity<T> throngEntity) {

    super(throngEntity);

    entityClass = throngEntity.getEntityClass();
    idProperty = throngEntity.getIdProperty();
    collection = throngEntity.getCollection();
    lifecycle = throngEntity.getLifecycle();
  }

  public Class<T> getEntityClass () {

    return entityClass;
  }

  public String getCollection () {

    return collection;
  }

  public ThrongLifecycle<T> getLifecycle () {

    return lifecycle;
  }

  @Override
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    T instance;
    String idName;

    reader.readStartDocument();

    if (!idProperty.getName().equals(idName = reader.readName())) {
      throw new ThrongRuntimeException("The expected 'id' field(%s) does not match the actual(%s)", idProperty.getName(), idName);
    } else {

      Object idValue = idProperty.getCodec().decode(reader, decoderContext);
      ThrongLifecycle<T> events;

      instance = super.decode(reader, decoderContext);
      reader.readEndDocument();

      try {
        idProperty.getFieldAccessor().set(instance, idValue);
      } catch (IllegalAccessException | InvocationTargetException exception) {
        throw new ThrongRuntimeException(exception);
      }

      return instance;
    }
  }

  @Override
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    if (value != null) {
      writer.writeStartDocument();

      try {

        Object idValue;

        if ((idValue = idProperty.getFieldAccessor().get(value)) != null) {
          writer.writeName(idProperty.getName());
          reEncode(writer, idProperty.getCodec(), idValue, encoderContext);
        }
      } catch (IllegalAccessException | InvocationTargetException exception) {
        throw new ThrongRuntimeException(exception);
      }

      super.encode(writer, value, encoderContext);
      writer.writeEndDocument();
    }
  }
}
