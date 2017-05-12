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
package org.smallmind.persistence.orm.querydsl.hibernate;

import java.io.Serializable;
import java.util.List;
import com.querydsl.jpa.hibernate.HibernateQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.hibernate.HibernateDurable;

public class QHibernateDao<I extends Serializable & Comparable<I>, D extends HibernateDurable<I, D>> extends ORMDao<I, D, SessionFactory, Session> {

  public QHibernateDao (ProxySession<SessionFactory, Session> proxySession) {

    super(proxySession, null);
  }

  public QHibernateDao (ProxySession<SessionFactory, Session> proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);
  }

  @Override
  public D acquire (Class<D> durableClass, I id) {

    return null;
  }

  @Override
  public D get (Class<D> persistentClass, I id) {

    return null;
  }

  @Override
  public D persist (Class<D> durableClass, D durable) {

    return null;
  }

  @Override
  public void delete (Class<D> persistentClass, D persistent) {

  }

  @Override
  public D detach (D durable) {

    return null;
  }

  @Override
  public long size () {

    return 0;
  }

  @Override
  public List<D> list () {

    return null;
  }

  @Override
  public List<D> list (int fetchSize) {

    return null;
  }

  @Override
  public List<D> list (I greaterThan, int fetchSize) {

    return null;
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return null;
  }

  @Override
  public Iterable<D> scrollById (I greaterThan, int fetchSize) {

    return null;
  }

  @Override
  public Iterable<D> scroll () {

    return null;
  }

  public D findByQuery (HibernateQueryDetails queryDetails) {

    return getManagedClass().cast(constructQuery(queryDetails).fetchOne());
  }

  public <T> T findByQuery (Class<T> returnType, HibernateQueryDetails queryDetails) {

    return returnType.cast(constructQuery(queryDetails).fetchOne());
  }

  public HibernateQuery<?> constructQuery (HibernateQueryDetails queryDetails) {

    HibernateQuery<?> query = new HibernateQuery<>(getSession().getNativeSession());

    return queryDetails.completeQuery(query);
  }
}
