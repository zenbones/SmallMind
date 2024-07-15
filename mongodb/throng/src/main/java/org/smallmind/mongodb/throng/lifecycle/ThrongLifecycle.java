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
package org.smallmind.mongodb.throng.lifecycle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import org.bson.BsonDocument;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.ThrongRuntimeException;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostPersist;
import org.smallmind.mongodb.throng.lifecycle.annotation.PreLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PrePersist;

public class ThrongLifecycle<T> {

  private final LinkedList<Method> preLoadMethods = new LinkedList<>();
  private final LinkedList<Method> postLoadMethods = new LinkedList<>();
  private final LinkedList<Method> prePersistMethods = new LinkedList<>();
  private final LinkedList<Method> postPersistMethods = new LinkedList<>();

  public ThrongLifecycle (Class<T> entityClass)
    throws ThrongMappingException {

    for (Method method : entityClass.getMethods()) {
      if (method.getAnnotation(PreLoad.class) != null) {
        if ((!Modifier.isStatic(method.getModifiers()))) {
          throw new ThrongMappingException("The @PreLoad method(%s) of entity type(%s) must be decalred as 'static'", method.getName(), entityClass.getName());
        } else {
          preLoadMethods.add(method);
        }
      }
      if (method.getAnnotation(PostLoad.class) != null) {
        if ((Modifier.isStatic(method.getModifiers()))) {
          throw new ThrongMappingException("The @PostLoad method(%s) of entity type(%s) must not be decalred as 'static'", method.getName(), entityClass.getName());
        } else {
          postLoadMethods.add(method);
        }
      }
      if (method.getAnnotation(PrePersist.class) != null) {
        if ((Modifier.isStatic(method.getModifiers()))) {
          throw new ThrongMappingException("The @PrePersist method(%s) of entity type(%s) must not be decalred as 'static'", method.getName(), entityClass.getName());
        } else {
          prePersistMethods.add(method);
        }
      }
      if (method.getAnnotation(PostPersist.class) != null) {
        if ((Modifier.isStatic(method.getModifiers()))) {
          throw new ThrongMappingException("The @PostPersist method(%s) of entity type(%s) must not be decalred as 'static'", method.getName(), entityClass.getName());
        } else {
          postPersistMethods.add(method);
        }
      }
    }
  }

  public void executePreLoad (Class<T> entityClass, BsonDocument bsonDocument) {

    for (Method method : preLoadMethods) {
      try {
        method.invoke(entityClass, bsonDocument);
      } catch (InvocationTargetException | IllegalAccessException exception) {
        throw new ThrongRuntimeException(exception);
      }
    }
  }

  public void executePostLoad (T value) {

    for (Method method : postLoadMethods) {
      try {
        method.invoke(value);
      } catch (InvocationTargetException | IllegalAccessException exception) {
        throw new ThrongRuntimeException(exception);
      }
    }
  }

  public void executePrePersist (T value) {

    for (Method method : prePersistMethods) {
      try {
        method.invoke(value);
      } catch (InvocationTargetException | IllegalAccessException exception) {
        throw new ThrongRuntimeException(exception);
      }
    }
  }

  public void executePostPersist (T value, BsonDocument bsonDocument) {

    for (Method method : postPersistMethods) {
      try {
        method.invoke(value, bsonDocument);
      } catch (InvocationTargetException | IllegalAccessException exception) {
        throw new ThrongRuntimeException(exception);
      }
    }
  }
}
