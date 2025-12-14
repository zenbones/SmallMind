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
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.lifecycle.ThrongLifecycle;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Id;
import org.smallmind.mongodb.throng.mapping.annotation.Polymorphic;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;

/**
 * Represents an entity mapped to a MongoDB collection, including lifecycle hooks and id metadata.
 *
 * @param <T> entity type
 */
public class ThrongEntity<T> extends ThrongProperties<T> {

  private final ThrongLifecycle<T> lifecycle;
  private final String collection;
  private ThrongProperty idProperty;

  /**
   * Builds metadata for an entity, registering its id property and lifecycle callbacks.
   *
   * @param entityClass        concrete entity class
   * @param entityAnnotation   entity annotation with collection information
   * @param codecRegistry      registry for resolving codecs
   * @param embeddedReferences cache of embedded codecs
   * @param storeNulls         whether null values should be stored
   * @throws ThrongMappingException    if id fields are missing or invalid, or annotations are misused
   * @throws NoSuchMethodException     if reflective codec construction fails
   * @throws InstantiationException    if codec construction fails
   * @throws IllegalAccessException    if reflection cannot access members
   * @throws InvocationTargetException if construction of codecs throws
   */
  public ThrongEntity (Class<T> entityClass, Entity entityAnnotation, CodecRegistry codecRegistry, EmbeddedReferences embeddedReferences, boolean storeNulls)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    super(entityClass, codecRegistry, embeddedReferences, storeNulls);

    // check for misuse of the polymorphic annotation
    if (entityClass.getAnnotation(Polymorphic.class) != null) {
      throw new ThrongMappingException("Only @Embedded classes may be marked as @Polymorphic");
    }

    collection = entityAnnotation.value();
    lifecycle = new ThrongLifecycle<>(entityClass);

    for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(entityClass)) {

      Id idAnnotation;

      if ((idAnnotation = fieldAccessor.getField().getAnnotation(Id.class)) != null) {
        if (containsKey(fieldAccessor.getName())) {
          throw new ThrongMappingException("The field(%s) in entity(%s) can't be both an 'id' and a 'property'", fieldAccessor.getName(), fieldAccessor.getType().getName(), entityClass.getName());
        } else if (idProperty != null) {
          throw new ThrongMappingException("The entity(%s) has multiple 'id' fields defined", entityClass.getName());
        } else {
          try {

            Codec<?> idCodec;

            if ((idCodec = codecRegistry.get(CodecUtility.getReifiedType(entityClass, fieldAccessor))) == null) {
              throw new ThrongMappingException("No known codec for id(%s) of type(%s) in entity(%s)", fieldAccessor.getName(), fieldAccessor.getType().getName(), entityClass.getName());
            } else {
              idProperty = new ThrongProperty(fieldAccessor, idCodec, idAnnotation.value());
            }
          } catch (CodecConfigurationException codecConfigurationException) {
            throw new ThrongMappingException("No known codec for id(%s) of type(%s) in entity(%s)", fieldAccessor.getName(), fieldAccessor.getType().getName(), entityClass.getName());
          }
        }
      }
    }

    if (idProperty == null) {
      throw new ThrongMappingException("The entity(%s) has no 'id' field defined", entityClass.getName());
    }
  }

  /**
   * @return MongoDB collection name for the entity
   */
  public String getCollection () {

    return collection;
  }

  /**
   * @return property representing the id field
   */
  public ThrongProperty getIdProperty () {

    return idProperty;
  }

  /**
   * @return lifecycle handler for the entity
   */
  public ThrongLifecycle<T> getLifecycle () {

    return lifecycle;
  }
}
