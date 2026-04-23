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
 * Strategy interface responsible for constructing {@link ComponentInstance} objects and
 * participating in the pool's lifecycle.
 * <p>
 * The pool calls the four lifecycle methods in order during its own startup and shutdown
 * sequences: {@link #initialize()} then {@link #startup()} on the way up, and
 * {@link #shutdown()} then {@link #deconstruct()} on the way down. {@link #createInstance}
 * may be called any time between startup and shutdown.
 * <p>
 * {@link AbstractComponentInstanceFactory} provides no-op implementations of all lifecycle
 * methods for implementations that do not need them.
 *
 * @param <C> the type of component that instances produced by this factory will manage
 */
public interface ComponentInstanceFactory<C> {

  /**
   * Creates a new {@link ComponentInstance} associated with the given pool.
   * <p>
   * Called by the pool whenever it needs to add a component — at startup when pre-warming,
   * when a caller requests a component and the free queue is empty and the size cap allows
   * growth, or when replacing a terminated instance.
   *
   * @param componentPool the pool on whose behalf the instance is being created
   * @return a freshly constructed {@link ComponentInstance} ready for the pool
   * @throws Exception if construction of the component or its wrapper fails
   */
  ComponentInstance<C> createInstance (ComponentPool<C> componentPool)
    throws Exception;

  /**
   * Called once before {@link #startup()} to allow the factory to establish any external
   * connections or one-time resources required for subsequent {@link #createInstance} calls.
   *
   * @throws Exception if initialization fails; the pool will propagate this as a
   *                   {@link org.smallmind.quorum.pool.ComponentPoolException}
   */
  void initialize ()
    throws Exception;

  /**
   * Called after {@link #initialize()} and before the pool begins serving components.
   * May be used to warm connections, register listeners, or perform any other startup work.
   *
   * @throws Exception if startup fails; the pool will propagate this as a
   *                   {@link org.smallmind.quorum.pool.ComponentPoolException}
   */
  void startup ()
    throws Exception;

  /**
   * Called when the pool begins its shutdown sequence, before {@link #deconstruct()}.
   * Implementations should stop accepting new requests and quiesce any background activity.
   *
   * @throws Exception if shutdown fails; the pool will log and continue shutting down
   */
  void shutdown ()
    throws Exception;

  /**
   * Called after all component instances have been closed and the pool has fully stopped,
   * to release any resources held by the factory itself.
   *
   * @throws Exception if cleanup fails; the pool will log and continue
   */
  void deconstruct ()
    throws Exception;
}
