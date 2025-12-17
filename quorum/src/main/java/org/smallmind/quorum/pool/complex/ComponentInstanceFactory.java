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
package org.smallmind.quorum.pool.complex;

/**
 * Factory for producing and lifecycle-managing component instances used in the complex pool.
 *
 * @param <C> component type
 */
public interface ComponentInstanceFactory<C> {

  /**
   * Creates a new component instance associated with the provided pool.
   *
   * @param componentPool owning pool
   * @return new component instance
   * @throws Exception if creation fails
   */
  ComponentInstance<C> createInstance (ComponentPool<C> componentPool)
    throws Exception;

  /**
   * Performs one-time factory initialization.
   *
   * @throws Exception if initialization fails
   */
  void initialize ()
    throws Exception;

  /**
   * Starts the factory after initialization.
   *
   * @throws Exception if startup fails
   */
  void startup ()
    throws Exception;

  /**
   * Shuts down the factory before deconstruction.
   *
   * @throws Exception if shutdown fails
   */
  void shutdown ()
    throws Exception;

  /**
   * Deconstructs any resources held by the factory.
   *
   * @throws Exception if cleanup fails
   */
  void deconstruct ()
    throws Exception;
}
