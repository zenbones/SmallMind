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
package org.smallmind.scribe.pen;

import java.io.Serializable;

/**
 * Serializable snapshot of the call-site location at which a logging event was created, including the class,
 * method, file, and line number from the application's call stack.
 */
public interface LoggerContext extends Serializable {

  /**
   * Returns whether the context fields have already been populated by a prior call to {@link #fillIn()}.
   *
   * @return {@code true} if all available call-site fields have been resolved
   */
  boolean isFilled ();

  /**
   * Resolves and stores the call-site fields by walking the current thread's stack; this method is typically called
   * lazily the first time context information is needed.
   */
  void fillIn ();

  /**
   * Returns the fully-qualified name of the class at the logging call site.
   *
   * @return the class name, or {@code null} if it could not be determined
   */
  String getClassName ();

  /**
   * Returns the name of the method at the logging call site.
   *
   * @return the method name, or {@code null} if it could not be determined
   */
  String getMethodName ();

  /**
   * Returns the source file name at the logging call site.
   *
   * @return the file name, or {@code null} if it could not be determined
   */
  String getFileName ();

  /**
   * Returns whether the logging call site is located inside a native (JNI) method.
   *
   * @return {@code true} if the originating frame is a native method
   */
  boolean isNativeMethod ();

  /**
   * Returns the source line number at the logging call site.
   *
   * @return the line number, or {@code -1} if the information is not available
   */
  int getLineNumber ();
}
