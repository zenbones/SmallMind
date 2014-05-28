/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.nosql;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.reflection.type.TypeInference;
import org.smallmind.persistence.AbstractWideVectorAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.WideDurableDao;
import org.smallmind.persistence.cache.WideVectoredDao;

public abstract class NoSqlDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractWideVectorAwareManagedDao<W, I, D> implements WideDurableDao<W, I, D> {

  private final TypeInference parentIdTypeInference = new TypeInference();
  private boolean cacheEnabled;

  public NoSqlDao (String metricSource, WideVectoredDao<W, I, D> wideVectoredDao, boolean cacheEnabled) {

    super(metricSource, wideVectoredDao);

    this.cacheEnabled = cacheEnabled;

    List<Class<?>> typeArguments = GenericUtility.getTypeArguments(NoSqlDao.class, this.getClass());

    if (typeArguments.size() == 3) {
      if (typeArguments.get(0) != null) {
        parentIdTypeInference.addPossibility(typeArguments.get(0));
      }
    }
  }

  public Class<I> getParentIdClass () {

    return parentIdTypeInference.getInference();
  }

  @Override
  public boolean isCacheEnabled () {

    return cacheEnabled;
  }

  @Override
  public void remove (String context, Class<? extends Durable<W>> parentClass, W parentId) {

    remove(context, parentClass, parentId, getManagedClass());
  }

  @Override
  public List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId) {

    return get(context, parentClass, parentId, getManagedClass());
  }

  @Override
  public List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables) {

    return persist(context, parentClass, parentId, getManagedClass(), durables);
  }

  @Override
  public List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables) {

    return persist(context, parentClass, parentId, getManagedClass(), durables);
  }

  @Override
  public List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables) {

    return persist(context, parentClass, parentId, durableClass, Arrays.asList(durables));
  }

  @Override
  public void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables) {

    delete(context, parentClass, parentId, getManagedClass(), durables);
  }

  @Override
  public void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables) {

    delete(context, parentClass, parentId, getManagedClass(), durables);
  }

  @Override
  public void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables) {

    delete(context, parentClass, parentId, durableClass, Arrays.asList(durables));
  }
}
