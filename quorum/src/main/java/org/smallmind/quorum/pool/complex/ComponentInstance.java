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

import org.smallmind.nutsnbolts.lang.Existential;

/**
 * Contract for an object that wraps a pooled component and manages its health and lifecycle.
 * <p>
 * The complex pool stores {@code ComponentInstance} objects rather than raw components.
 * Each instance owns the underlying resource and is responsible for reporting its health via
 * {@link #validate()}, exposing the resource to callers via {@link #serve()}, and releasing
 * all resources when the pool is done with it via {@link #close()}.
 * <p>
 * Extends {@link Existential} so that the pool can optionally capture and expose a stack trace
 * identifying the thread that acquired the component.
 *
 * @param <C> the type of the underlying component managed by this instance
 */
public interface ComponentInstance<C> extends Existential {

  /**
   * Tests whether this instance is still healthy and fit for use.
   * <p>
   * Called by the pool before handing the instance to a caller when
   * {@link ComplexPoolConfig#isTestOnAcquire()} is enabled, and immediately after creation
   * when {@link ComplexPoolConfig#isTestOnCreate()} is enabled. A return value of {@code false}
   * causes the pool to discard the instance and attempt to obtain a different one.
   *
   * @return {@code true} if the instance is healthy; {@code false} if it should be discarded
   */
  boolean validate ();

  /**
   * Returns the underlying component, performing any per-use setup required by the
   * implementation (for example, capturing an existential stack trace).
   *
   * @return the component ready for use by the caller
   * @throws Exception if the component cannot be prepared for use
   */
  C serve ()
    throws Exception;

  /**
   * Permanently releases all resources held by this instance.
   * <p>
   * Called by the pool when the instance is being removed from service — whether due to
   * a failed validation check, a deconstruction fuse igniting, or the pool shutting down.
   * After this method returns the pool will never call any other method on this instance.
   *
   * @throws Exception if resource cleanup fails
   */
  void close ()
    throws Exception;
}
