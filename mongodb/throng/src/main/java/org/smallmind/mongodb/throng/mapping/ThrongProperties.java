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
import java.util.HashMap;
import java.util.TreeMap;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.mapping.annotation.Codec;
import org.smallmind.mongodb.throng.mapping.annotation.Embedded;
import org.smallmind.mongodb.throng.index.annotation.Indexed;
import org.smallmind.mongodb.throng.index.annotation.Indexes;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.index.IndexProvider;
import org.smallmind.mongodb.throng.index.ThrongIndexes;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;

public class ThrongProperties<T> extends TreeMap<String, ThrongProperty> implements IndexProvider {

  private final Class<T> entityClass;
  private final ThrongIndexes throngIndexes = new ThrongIndexes();
  private final HashMap<String, String> propertyNameMap = new HashMap<>();
  private final boolean storeNulls;

  public ThrongProperties (Class<T> entityClass, CodecRegistry codecRegistry, EmbeddedReferences embeddedReferences, boolean storeNulls)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    this.entityClass = entityClass;
    this.storeNulls = storeNulls;

    throngIndexes.addIndexes(entityClass.getAnnotationsByType(Indexes.class));

    for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(entityClass)) {

      Property propertyAnnotation;

      if ((propertyAnnotation = fieldAccessor.getField().getAnnotation(Property.class)) != null) {

        String propertyName = propertyAnnotation.value().isEmpty() ? fieldAccessor.getName() : propertyAnnotation.value();

        if (propertyNameMap.containsKey(propertyName)) {
          throw new ThrongMappingException("The property(%s) in entity(%s) must be unique", propertyName, entityClass.getName());
        } else {

          Indexed indexedAnnotation;
          Codec codecAnnotation;
          org.bson.codecs.Codec<?> codec;

          if ((indexedAnnotation = fieldAccessor.getField().getAnnotation(Indexed.class)) != null) {
            throngIndexes.addIndexed(propertyName, indexedAnnotation);
          }

          if ((codecAnnotation = fieldAccessor.getField().getAnnotation(Codec.class)) != null) {
            codec = codecAnnotation.value().getConstructor().newInstance();
          } else {

            Embedded embedded;

            if ((embedded = fieldAccessor.getType().getAnnotation(Embedded.class)) != null) {
              codec = ThrongEmbeddedUtility.generateEmbeddedCodec(fieldAccessor.getType(), embedded, codecRegistry, embeddedReferences, storeNulls);
              throngIndexes.accumulate(propertyName, ((IndexProvider)codec).provideIndexes());
            } else {
              try {
                codec = CodecRegistryUtility.getReifiedCodec(codecRegistry, entityClass, fieldAccessor);
              } catch (CodecConfigurationException codecConfigurationException) {
                throw new ThrongMappingException("No known codec for field(%s) of type(%s) in entity(%s)", fieldAccessor.getName(), fieldAccessor.getType().getName(), entityClass.getName());
              }
            }
          }

          propertyNameMap.put(propertyName, fieldAccessor.getName());
          put(fieldAccessor.getName(), new ThrongProperty(fieldAccessor, codec, propertyName));
        }
      }
    }
  }

  public Class<T> getEntityClass () {

    return entityClass;
  }

  public boolean isStoreNulls () {

    return storeNulls;
  }

  public ThrongProperty getByPropertyName (String propertyName) {

    String fieldName;

    return ((fieldName = propertyNameMap.get(propertyName)) != null) ? get(fieldName) : null;
  }

  @Override
  public ThrongIndexes provideIndexes () {

    return throngIndexes;
  }
}
