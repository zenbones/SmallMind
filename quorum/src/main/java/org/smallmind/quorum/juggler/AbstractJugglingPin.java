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
 * Skeletal {@link JugglingPin} implementation that provides no-op lifecycle bodies
 * and convenience zero-argument overloads.
 * <p>
 * Subclasses override only the methods relevant to their resource type. The no-argument
 * overloads ({@link #start()}, {@link #stop()}, {@link #close()}) delegate to their
 * reflective counterparts with a {@code null} method reference, allowing callers to
 * trigger the lifecycle without supplying a hook method.
 *
 * @param <R> the type of resource wrapped by this pin
 */
public abstract class AbstractJugglingPin<R> implements JugglingPin<R> {

  /**
   * Starts the resource without invoking any additional lifecycle hook.
   * Delegates to {@link #start(Method, Object...) start(null)}.
   */
  public final void start () {

    start(null);
  }

  /**
   * No-op start implementation. Subclasses override this to perform resource initialisation
   * and optionally invoke {@code method} on the resource.
   *
   * @param method lifecycle hook to call after starting, or {@code null} to skip
   * @param args   arguments forwarded to {@code method}; ignored when {@code method} is {@code null}
   */
  @Override
  public void start (Method method, Object... args) {

  }

  /**
   * Stops the resource without invoking any additional lifecycle hook.
   * Delegates to {@link #stop(Method, Object...) stop(null)}.
   */
  public final void stop () {

    stop(null);
  }

  /**
   * No-op stop implementation. Subclasses override this to quiesce the resource
   * and optionally invoke {@code method} on it before halting.
   *
   * @param method lifecycle hook to call before stopping, or {@code null} to skip
   * @param args   arguments forwarded to {@code method}; ignored when {@code method} is {@code null}
   */
  @Override
  public void stop (Method method, Object... args) {

  }

  /**
   * Closes the resource without invoking any additional lifecycle hook.
   * Delegates to {@link #close(Method, Object...) close(null)}.
   */
  public final void close () {

    close(null);
  }

  /**
   * No-op close implementation. Subclasses override this to release all resources held
   * by the pin and optionally invoke {@code method} on the resource prior to disposal.
   *
   * @param method lifecycle hook to call before closing, or {@code null} to skip
   * @param args   arguments forwarded to {@code method}; ignored when {@code method} is {@code null}
   */
  @Override
  public void close (Method method, Object... args) {

  }
}
