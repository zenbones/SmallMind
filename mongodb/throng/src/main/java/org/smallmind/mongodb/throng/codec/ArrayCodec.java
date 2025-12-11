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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ArrayCodec<T> implements Codec<T> {

  private final Codec<?> itemCodec;
  private final Class<T> arrayClass;
  private final Class<?> componentClass;
  private final boolean storeNulls;

  public ArrayCodec (Class<T> arrayClass, Class<?> componentClass, Codec<?> itemCodec, boolean storeNulls) {

    this.arrayClass = arrayClass;
    this.componentClass = componentClass;
    this.itemCodec = itemCodec;
    this.storeNulls = storeNulls;
  }

  @Override
  public Class<T> getEncoderClass () {

    return arrayClass;
  }

  @Override
  public T decode (BsonReader reader, DecoderContext decoderContext) {

    if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
      reader.readNull();

      return null;
    } else {

      Object[] array;
      List<Object> list = new ArrayList<Object>();

      reader.readStartArray();

      while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
        list.add(itemCodec.decode(reader, decoderContext));
      }

      reader.readEndArray();

      array = (Object[])Array.newInstance(componentClass, list.size());
      list.toArray(array);

      return arrayClass.cast(array);
    }
  }

  @Override
  public void encode (BsonWriter writer, T value, EncoderContext encoderContext) {

    if (value != null) {

      writer.writeStartArray();

      for (Object item : (Object[])value) {
        reEncode(writer, itemCodec, item, encoderContext);
      }

      writer.writeEndArray();
    } else if (storeNulls) {
      writer.writeNull();
    }
  }

  // Due to the fact that object is not of type 'capture of ?'
  protected <U> void reEncode (BsonWriter writer, Codec<U> codec, Object stuff, EncoderContext encoderContext) {

    codec.encode(writer, (U)stuff, encoderContext);
  }
}
