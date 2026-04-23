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
package org.smallmind.sleuth.runner;

import java.lang.reflect.Method;

/**
 * Checked exception wrapping a failure that occurred while invoking a reflected test method.
 * <p>
 * Carries both the method reference and the elapsed execution time at the point of failure so that
 * callers can emit accurate Sleuth events without needing to re-compute timing.
 */
public class MethodInvocationException extends Exception {

  private final Method method;
  private final long elapsed;

  /**
   * Constructs an invocation exception.
   *
   * @param method  reflected method that was being invoked when the failure occurred; must not be {@code null}
   * @param elapsed elapsed execution time in milliseconds measured from invocation start to failure
   * @param cause   root cause of the failure; must not be {@code null}
   */
  public MethodInvocationException (Method method, long elapsed, Throwable cause) {

    super(cause);

    this.method = method;
    this.elapsed = elapsed;
  }

  /**
   * @return the method that was being invoked when the failure occurred; never {@code null}
   */
  public Method getMethod () {

    return method;
  }

  /**
   * @return elapsed execution time in milliseconds at the moment the failure was caught
   */
  public long getElapsed () {

    return elapsed;
  }
}
