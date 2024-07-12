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
package org.smallmind.mongodb.throng.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.ThrongRuntimeException;
import org.smallmind.mongodb.throng.mapping.annotation.Polymorphic;
import org.smallmind.mongodb.throng.index.IndexProvider;
import org.smallmind.mongodb.throng.index.ThrongIndexes;

public class ThrongPropertiesMultiplexer<T> implements IndexProvider {

  private final HashMap<String, ThrongPropertiesCodec<?>> polymorphicCodecMap = new HashMap<>();
  private final Class<T> entityClass;
  private final String key;
  private final boolean storeNulls;

  public ThrongPropertiesMultiplexer (Class<T> entityClass, Polymorphic polymorphic, CodecRegistry codecRegistry, EmbeddedReferences embeddedReferences, boolean storeNulls)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    this.entityClass = entityClass;
    this.storeNulls = storeNulls;

    key = polymorphic.key().isEmpty() ? "java/object" : polymorphic.key();

    for (Class<?> polymorphicClass : polymorphic.value()) {
      if (!polymorphicClass.isAssignableFrom(entityClass)) {
        throw new ThrongMappingException("The declared polymorphic class(%s) is not assignable from the declaring type(%s)", polymorphicClass.getName(), entityClass.getName());
      } else {
        polymorphicCodecMap.put(polymorphicClass.getName(), new ThrongPropertiesCodec<>(new ThrongProperties<>(polymorphicClass, codecRegistry, embeddedReferences, storeNulls)));
      }
    }
  }

  public Class<T> getEntityClass () {

    return entityClass;
  }

  public boolean isStoreNulls () {

    return storeNulls;
  }

  public String getKey () {

    return key;
  }

  public Codec<?> getCodec (String polymorphicClassName) {

    Codec<?> polymorphicCodec;

    if ((polymorphicCodec = polymorphicCodecMap.get(polymorphicClassName)) == null) {
      throw new ThrongRuntimeException("No known codec for polymorphic key(%s) of entity type(%s)", polymorphicClassName, entityClass.getName());
    } else {

      return polymorphicCodec;
    }
  }

  @Override
  public ThrongIndexes provideIndexes () {

    ThrongIndexes throngIndexes = new ThrongIndexes();

    for (ThrongPropertiesCodec<?> polymorphicCodec : polymorphicCodecMap.values()) {
      throngIndexes.add(polymorphicCodec.provideIndexes());
    }

    return throngIndexes;
  }
}
