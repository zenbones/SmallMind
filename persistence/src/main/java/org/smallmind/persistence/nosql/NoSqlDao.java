/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
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
import java.util.List;
import org.smallmind.persistence.AbstractCacheAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.WideDurableDao;
import org.smallmind.persistence.cache.VectoredDao;

public abstract class NoSqlDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheAwareManagedDao<I, D> implements WideDurableDao<I, D> {

  private boolean cacheEnabled;

  public NoSqlDao (String metricSource, VectoredDao<I, D> vectoredDao, boolean cacheEnabled) {

    super(metricSource, vectoredDao);

    this.cacheEnabled = cacheEnabled;
  }

  @Override
  public boolean isCacheEnabled () {

    return cacheEnabled;
  }

  @Override
  public List<D> get (I id) {

    return get(getManagedClass(), id);
  }

  @Override
  public D[] persist (I id, D... durables) {

    return persist(id, getManagedClass(), durables);
  }

  @Override
  public void delete (I id, D... durables) {

    delete(id, getManagedClass(), durables);
  }
}
