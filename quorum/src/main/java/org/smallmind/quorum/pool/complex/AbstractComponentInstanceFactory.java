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
 * Convenience base class for {@link ComponentInstanceFactory} implementations that do not need
 * lifecycle callbacks.
 * <p>
 * All four lifecycle methods — {@link #initialize()}, {@link #startup()}, {@link #shutdown()},
 * and {@link #deconstruct()} — are implemented as no-ops. Subclasses override only the methods
 * they need, and must implement {@link #createInstance(ComponentPool)}.
 *
 * @param <C> the type of component produced by the factory
 */
public abstract class AbstractComponentInstanceFactory<C> implements ComponentInstanceFactory<C> {

  /**
   * No-op; subclasses may override to perform one-time initialization before the pool starts.
   */
  @Override
  public void initialize () {

  }

  /**
   * No-op; subclasses may override to perform work after the pool has initialized and before
   * it begins serving components.
   */
  @Override
  public void startup () {

  }

  /**
   * No-op; subclasses may override to release resources when the pool begins its shutdown
   * sequence, before {@link #deconstruct()} is called.
   */
  @Override
  public void shutdown () {

  }

  /**
   * No-op; subclasses may override to perform final cleanup after the pool has fully stopped.
   */
  @Override
  public void deconstruct () {

  }
}
