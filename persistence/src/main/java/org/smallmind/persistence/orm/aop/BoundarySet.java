/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * A {@link HashSet} that represents a single AOP boundary scope and controls which session source keys
 * are permitted to participate in it.
 *
 * @param <T> the resource type (session or transaction) tracked within this boundary
 */
public class BoundarySet<T> extends HashSet<T> {

  private final String[] sessionSourceKeys;
  private final boolean implicit;

  /**
   * Constructs a boundary set for the given session source constraints.
   *
   * @param sessionSourceKeys the session source keys permitted in this boundary; empty means all sources are allowed
   * @param implicit          whether this boundary was established implicitly by a class-level annotation
   */
  public BoundarySet (String[] sessionSourceKeys, boolean implicit) {

    super();

    this.sessionSourceKeys = sessionSourceKeys;
    this.implicit = implicit;
  }

  /**
   * Returns {@code true} when this boundary is both implicit and unconstrained by any explicit session source keys.
   *
   * @return {@code true} if the boundary is implicit and no source keys are listed
   */
  public boolean isImplicit () {

    return implicit && (sessionSourceKeys.length == 0);
  }

  /**
   * Returns {@code true} if the given session's source key is permitted by this boundary.
   *
   * @param proxySession the session whose source key is checked
   * @return {@code true} if the session is allowed
   */
  public boolean allows (ProxySession<?, ?> proxySession) {

    return allows(proxySession.getSessionSourceKey());
  }

  /**
   * Returns {@code true} if the given session source key is permitted by this boundary.
   *
   * @param sessionSourceKey the session source key to test; {@code null} is accepted when no keys are listed
   * @return {@code true} if the key is allowed
   * @throws IllegalArgumentException if the boundary is marked implicit but also lists explicit source keys
   */
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
