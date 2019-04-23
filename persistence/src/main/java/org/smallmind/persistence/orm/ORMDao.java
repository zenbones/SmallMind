/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.persistence.orm;

import java.io.Serializable;
import org.smallmind.persistence.AbstractVectorAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectoredDao;

public abstract class ORMDao<I extends Serializable & Comparable<I>, D extends Durable<I>, F, N> extends AbstractVectorAwareManagedDao<I, D> implements RelationalDao<I, D, F, N> {

  private ProxySession<F, N> proxySession;

  public ORMDao (ProxySession<F, N> proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession.getDataSourceType(), vectoredDao);

    this.proxySession = proxySession;
  }

  public void register () {

    OrmDaoManager.register(getManagedClass(), this);
  }

  @Override
  public String getSessionSourceKey () {

    return proxySession.getSessionSourceKey();
  }

  @Override
  public ProxySession<F, N> getSession () {

    return proxySession;
  }

  @Override
  public boolean isCacheEnabled () {

    return proxySession.isCacheEnabled();
  }

  // The acquire() method gets the managed object directly from the underlying data source (no vector, no cascade)
  public abstract D acquire (Class<D> durableClass, I id);

  @Override
  public D get (I id) {

    return get(getManagedClass(), id);
  }

  @Override
  public D persist (D durable) {

    return persist(getManagedClass(), durable);
  }

  @Override
  public void delete (D durable) {

    delete(getManagedClass(), durable);
  }
}
