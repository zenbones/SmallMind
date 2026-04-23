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
 * Handle to a provider-backed resource managed by a {@link Juggler}.
 * <p>
 * Each pin wraps one resource instance and exposes a uniform lifecycle —
 * start, stop, close — plus the ability to obtain the resource for use and
 * to attempt recovery after a failure. The optional {@code method} and
 * {@code args} parameters on lifecycle methods allow the {@link Juggler} to
 * invoke application-specific hooks on the resource without requiring the pin
 * to know the hook's signature in advance.
 *
 * @param <R> the type of resource wrapped by this pin
 */
public interface JugglingPin<R> {

  /**
   * Starts the underlying resource, optionally invoking {@code method} on it.
   *
   * @param method a reflective reference to the lifecycle method to call on the resource,
   *               or {@code null} to skip any extra invocation
   * @param args   arguments to pass to {@code method}; ignored when {@code method} is {@code null}
   * @throws JugglerResourceException if the resource cannot be started or the lifecycle call fails
   */
  void start (Method method, Object... args)
    throws JugglerResourceException;

  /**
   * Stops the underlying resource, optionally invoking {@code method} on it.
   *
   * @param method a reflective reference to the lifecycle method to call on the resource,
   *               or {@code null} to skip any extra invocation
   * @param args   arguments to pass to {@code method}; ignored when {@code method} is {@code null}
   * @throws JugglerResourceException if the resource cannot be stopped or the lifecycle call fails
   */
  void stop (Method method, Object... args)
    throws JugglerResourceException;

  /**
   * Closes the underlying resource and releases all associated state, optionally invoking {@code method} first.
   *
   * @param method a reflective reference to the lifecycle method to call before closing,
   *               or {@code null} to skip any extra invocation
   * @param args   arguments to pass to {@code method}; ignored when {@code method} is {@code null}
   * @throws JugglerResourceException if the resource cannot be closed or the lifecycle call fails
   */
  void close (Method method, Object... args)
    throws JugglerResourceException;

  /**
   * Obtains the resource for active use.
   *
   * @return the live resource instance
   * @throws JugglerResourceException if the resource is unavailable or fails to be obtained
   */
  R obtain ()
    throws JugglerResourceException;

  /**
   * Attempts to recover the resource after it has been blacklisted due to a failure.
   *
   * @return {@code true} if the resource is healthy and ready to return to service;
   * {@code false} if it remains unusable
   */
  boolean recover ();

  /**
   * Returns a human-readable label identifying this pin and its underlying provider,
   * used in log messages and diagnostics.
   *
   * @return description string; never {@code null}
   */
  String describe ();
}
