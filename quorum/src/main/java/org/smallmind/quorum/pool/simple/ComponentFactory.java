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
package org.smallmind.quorum.pool.simple;

/**
 * Strategy for creating new {@link PooledComponent} instances on behalf of a {@link ComponentPool}.
 * <p>
 * The pool calls {@link #createComponent()} whenever it needs a fresh instance — either because
 * the pool is empty and has not yet reached its capacity cap, or when it has been configured as
 * unbounded.
 *
 * @param <T> the type of {@link PooledComponent} produced by this factory
 */
public interface ComponentFactory<T extends PooledComponent> {

  /**
   * Creates and returns a new pooled component instance.
   *
   * @return a freshly constructed component ready for use
   * @throws Exception if the component cannot be created for any reason; the caller will wrap
   *                   this in a {@link org.smallmind.quorum.pool.ComponentPoolException}
   */
  T createComponent ()
    throws Exception;
}
