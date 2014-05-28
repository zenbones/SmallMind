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
package org.smallmind.persistence.orm.aop;

import java.util.HashSet;
import org.smallmind.persistence.orm.ProxySession;

public class BoundarySet<T> extends HashSet<T> {

  private String[] dataSourceKeys;
  private boolean implicit;

  public BoundarySet (String dataSourceKeys[], boolean implicit) {

    super();

    this.dataSourceKeys = dataSourceKeys;
    this.implicit = implicit;
  }

  public boolean isImplicit () {

    return implicit && (dataSourceKeys.length == 0);
  }

  public boolean allows (ProxySession proxySession) {

    return allows(proxySession.getDataSourceKey());
  }

  public boolean allows (String dataSourceKey) {

    if (dataSourceKeys.length == 0) {
      return isImplicit() || (dataSourceKey == null);
    }
    else if (isImplicit()) {
      throw new IllegalArgumentException("Boundary annotation (@NonTransaction or @Transactional) is marked as implicit, but explicitly lists data sources");
    }
    else if (dataSourceKey != null) {
      for (String boundarySource : dataSourceKeys) {
        if (dataSourceKey.equals(boundarySource)) {
          return true;
        }
      }
    }

    return false;
  }
}