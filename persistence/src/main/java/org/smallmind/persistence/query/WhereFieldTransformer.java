/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.query;

import java.lang.reflect.Field;
import java.util.HashMap;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.orm.ORMOperationException;

public abstract class WhereFieldTransformer<R, T> {

  private HashMap<String, JoinedType<?>> typeMap = new HashMap<>();
  private HashMap<String, WhereFieldTransform<R, T>> transformMap = new HashMap<>();
  private WhereFieldTransform<R, T> defaultTransform;

  public WhereFieldTransformer () {

  }

  public WhereFieldTransformer (WhereFieldTransform<R, T> defaultTransform) {

    this.defaultTransform = defaultTransform;
  }

  public abstract <D extends Durable<?>> WherePath<R, T> createWherePath (Class<D> durableClass, R root, String name);

  public synchronized <D extends Durable<?>> WhereFieldTransformer<R, T> addRoot (Class<D> durableClass, R root) {

    typeMap.put(durableClass.getSimpleName(), new JoinedType<>(durableClass, root));

    return this;
  }

  public synchronized WhereFieldTransformer<R, T> add (WhereFieldTransform<R, T> transform, String... names) {

    if ((names != null) && (names.length > 0)) {
      for (String name : names) {
        transformMap.put(name, transform);
      }
    }

    return this;
  }

  public synchronized WherePath<R, T> transform (String entity, String name) {

    JoinedType<?> joinedType;

    if ((entity != null) && (!entity.isEmpty())) {
      if ((joinedType = typeMap.get(entity)) != null) {

        return createWherePath(joinedType.getDurableClass(), joinedType.getRoot(), name);
      } else {
        throw new ORMOperationException("Unknown entity(%s)", entity);
      }
    } else {

      WhereFieldTransform<R, T> transform;

      if ((transform = transformMap.get(name)) != null) {

        return transform.apply(entity, name);
      } else if ((joinedType = deduceJoinedType(name)) != null) {

        return createWherePath(joinedType.getDurableClass(), joinedType.getRoot(), name);
      } else if (defaultTransform != null) {

        return defaultTransform.apply(entity, name);
      } else {
        throw new ORMOperationException("Could not deduce the entity for the field(%s)", name);
      }
    }
  }

  private JoinedType<?> deduceJoinedType (String name) {

    for (JoinedType<?> joinedType : typeMap.values()) {
      for (Field field : FieldUtility.getFields(joinedType.getDurableClass())) {
        if (field.getName().equals(name)) {

          return joinedType;
        }
      }
    }

    return null;
  }

  private class JoinedType<D extends Durable<?>> {

    private Class<D> durableClass;
    private R root;

    private JoinedType (Class<D> durableClass, R root) {

      this.durableClass = durableClass;
      this.root = root;
    }

    public Class<D> getDurableClass () {

      return durableClass;
    }

    public R getRoot () {

      return root;
    }
  }
}
