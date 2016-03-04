/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.orm.morphia;

import org.mongodb.morphia.Datastore;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;

public class MorphiaProxySession extends ProxySession<DatastoreFactory, Datastore> {

  private final DatastoreFactory datastoreFactory;
  private final MorphiaProxyTransaction proxyTransaction;

  public MorphiaProxySession (String dataSourceType, String sessionSourceKey, DatastoreFactory datastoreFactory, boolean boundaryEnforced, boolean cacheEnabled) {

    super(dataSourceType, sessionSourceKey, boundaryEnforced, cacheEnabled);

    this.datastoreFactory = datastoreFactory;

    proxyTransaction = new MorphiaProxyTransaction(this);
  }

  @Override
  public ProxyTransaction currentTransaction () {

    return proxyTransaction;
  }

  @Override
  public ProxyTransaction beginTransaction () {

    return proxyTransaction;
  }

  @Override
  public void flush () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void clear () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void close () {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isClosed () {

    throw new UnsupportedOperationException();
  }

  @Override
  public DatastoreFactory getNativeSessionFactory () {

    return datastoreFactory;
  }

  @Override
  public Datastore getNativeSession () {

    return datastoreFactory.get();
  }
}
