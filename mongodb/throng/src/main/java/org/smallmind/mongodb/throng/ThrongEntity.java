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
package org.smallmind.mongodb.throng;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;

public class ThrongEntity extends ThrongProperties {

  private final String collection;
  private ThrongProperty idProperty;

  public ThrongEntity (Class<?> entityClass, CodecRegistry codecRegistry, HashMap<String, ThrongEmbeddedCodec<?>> embeddedReferenceMap)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    super(entityClass, codecRegistry, embeddedReferenceMap);

    Entity entityAnnotation;

    if ((entityAnnotation = entityClass.getAnnotation(Entity.class)) == null) {
      throw new ThrongMappingException("The entity class(%s) is not @Entity annotated", entityClass.getName());
    } else {

      collection = entityAnnotation.value();

      for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(entityClass)) {

        Id idAnnotation;

        if ((idAnnotation = fieldAccessor.getField().getAnnotation(Id.class)) != null) {
          if (containsKey(fieldAccessor.getName())) {
            throw new ThrongMappingException("The field(%s) in entity(%s) can't be both an 'id' and a 'property'", fieldAccessor.getName(), fieldAccessor.getType().getName(), entityClass.getName());
          } else if (idProperty != null) {
            throw new ThrongMappingException("The entity(%s) has multiple 'id' fields defined", entityClass.getName());
          } else {
            try {
              idProperty = new ThrongProperty(fieldAccessor, CodecRegistryUtility.getReifiedCodec(codecRegistry, entityClass, fieldAccessor), idAnnotation.value());
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
  }

  public String getCollection () {

    return collection;
  }

  public ThrongProperty getIdProperty () {

    return idProperty;
  }
}
