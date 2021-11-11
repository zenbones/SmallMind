/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.persistence.orm.aop;

import java.util.HashSet;
import org.smallmind.persistence.orm.ProxySession;

public class BoundarySet<T> extends HashSet<T> {

  private final String[] sessionSourceKeys;
  private final boolean implicit;

  public BoundarySet (String[] sessionSourceKeys, boolean implicit) {

    super();

    this.sessionSourceKeys = sessionSourceKeys;
    this.implicit = implicit;
  }

  public boolean isImplicit () {

    return implicit && (sessionSourceKeys.length == 0);
  }

  public boolean allows (ProxySession proxySession) {

    return allows(proxySession.getSessionSourceKey());
  }

  public boolean allows (String sessionSourceKey) {

    if (sessionSourceKeys.length == 0) {
      return isImplicit() || (sessionSourceKey == null);
    } else if (isImplicit()) {
      throw new IllegalArgumentException("Boundary annotation (@NonTransaction or @Transactional) is marked as implicit, but explicitly lists data sources");
    } else if (sessionSourceKey != null) {
      for (String boundarySource : sessionSourceKeys) {
        if (sessionSourceKey.equals(boundarySource)) {
          return true;
        }
      }
    }

    return false;
  }
}