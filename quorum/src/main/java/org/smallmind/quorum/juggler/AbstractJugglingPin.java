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
package org.smallmind.quorum.juggler;

import java.lang.reflect.Method;

/**
 * Convenience base implementation that provides no-op lifecycle hooks and overloads without reflection arguments.
 *
 * @param <R> resource type managed by the pin
 */
public abstract class AbstractJugglingPin<R> implements JugglingPin<R> {

  /**
   * Starts the resource without invoking a specific lifecycle method.
   */
  public final void start () {

    start(null);
  }

  /**
   * Starts the resource invoking an optional lifecycle method. Subclasses should override to provide behavior.
   *
   * @param method lifecycle method to call on the resource, or {@code null} to ignore
   * @param args   arguments passed to the lifecycle method
   */
  @Override
  public void start (Method method, Object... args) {

  }

  /**
   * Stops the resource without invoking a specific lifecycle method.
   */
  public final void stop () {

    stop(null);
  }

  /**
   * Stops the resource invoking an optional lifecycle method. Subclasses should override to provide behavior.
   *
   * @param method lifecycle method to call on the resource, or {@code null} to ignore
   * @param args   arguments passed to the lifecycle method
   */
  @Override
  public void stop (Method method, Object... args) {

  }

  /**
   * Closes the resource without invoking a specific lifecycle method.
   */
  public final void close () {

    close(null);
  }

  /**
   * Closes the resource invoking an optional lifecycle method. Subclasses should override to provide behavior.
   *
   * @param method lifecycle method to call on the resource, or {@code null} to ignore
   * @param args   arguments passed to the lifecycle method
   */
  @Override
  public void close (Method method, Object... args) {

  }
}
