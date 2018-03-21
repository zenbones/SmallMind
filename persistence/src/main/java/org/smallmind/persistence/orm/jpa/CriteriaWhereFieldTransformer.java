/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.orm.jpa;

import java.lang.reflect.Field;
import java.util.HashMap;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.orm.ORMOperationException;
import org.smallmind.persistence.query.AbstractWhereFieldTransformer;
import org.smallmind.persistence.query.WherePath;

public class CriteriaWhereFieldTransformer extends AbstractWhereFieldTransformer<Path<?>> {

  private HashMap<String, JoinedType<?>> typeMap = new HashMap<>();
  private Root<?> defaultRoot;

  public CriteriaWhereFieldTransformer () {

  }

  public CriteriaWhereFieldTransformer (Root<?> defaultRoot) {

    this.defaultRoot = defaultRoot;
  }

  public synchronized <D extends AbstractDurable<?, D>> CriteriaWhereFieldTransformer add (Class<? extends AbstractDurable<?, D>> durableClass, Root<D> root) {

    typeMap.put(durableClass.getSimpleName(), new JoinedType<>(durableClass, root));

    return this;
  }

  @Override
  public synchronized WherePath<Path<?>> getDefault (String entity, String name) {

    JoinedType<?> joinedType;

    if ((entity != null) && (!entity.isEmpty())) {
      if ((joinedType = typeMap.get(entity)) != null) {

        return new CriteriaWherePath(joinedType.getRoot().get(name));
      } else {
        throw new ORMOperationException("Unknown entity(%s)", entity);
      }
    } else if ((joinedType = deduceJoinedType(name)) != null) {

      return new CriteriaWherePath(joinedType.getRoot().get(name));
    } else if (defaultRoot != null) {
      return new CriteriaWherePath(defaultRoot.get(name));
    } else {
      throw new ORMOperationException("Could not deduce the entity for the field(%s)", name);
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

  private class JoinedType<D extends AbstractDurable<?, D>> {

    private Class<? extends AbstractDurable<?, D>> durableClass;
    private Root<D> root;

    public JoinedType (Class<? extends AbstractDurable<?, D>> durableClass, Root<D> root) {

      this.durableClass = durableClass;
      this.root = root;
    }

    public Class<? extends AbstractDurable<?, D>> getDurableClass () {

      return durableClass;
    }

    public Root<D> getRoot () {

      return root;
    }
  }
}
