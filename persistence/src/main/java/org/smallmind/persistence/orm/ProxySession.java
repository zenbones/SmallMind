/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.persistence.orm;

public abstract class ProxySession<N> {

  private final ThreadLocal<Boolean> boundaryEnforcedThreadLocal = new ThreadLocal<Boolean>() {

    protected Boolean initialValue () {

      return boundaryEnforced;
    }
  };
  private final ThreadLocal<Boolean> cacheEnabledThreadLocal = new ThreadLocal<Boolean>() {

    protected Boolean initialValue () {

      return cacheEnabled;
    }
  };

  private String database;
  private String dataSource;
  private boolean boundaryEnforced;
  private boolean cacheEnabled;

  public ProxySession (String database, String dataSource, boolean boundaryEnforced, boolean cacheEnabled) {

    this.database = database;
    this.dataSource = dataSource;
    this.boundaryEnforced = boundaryEnforced;
    this.cacheEnabled = cacheEnabled;
  }

  public void register () {

    SessionManager.register(dataSource, this);
  }

  public String getDatabase () {

    return database;
  }

  public String getDataSource () {

    return dataSource;
  }

  public boolean isBoundaryEnforced () {

    return boundaryEnforcedThreadLocal.get();
  }

  public void overrideBoundaryEnforced (boolean boundaryEnforced) {

    boundaryEnforcedThreadLocal.set(boundaryEnforced);
  }

  public boolean isCacheEnabled () {

    return cacheEnabledThreadLocal.get();
  }

  public void overrideCacheEnabled (boolean cacheEnabled) {

    cacheEnabledThreadLocal.set(cacheEnabled);
  }

  public abstract N getNativeSession ();

  public abstract ProxyTransaction beginTransaction ();

  public abstract ProxyTransaction currentTransaction ();

  public abstract void flush ();

  public abstract void clear ();

  public abstract boolean isClosed ();

  public abstract void close ();
}
