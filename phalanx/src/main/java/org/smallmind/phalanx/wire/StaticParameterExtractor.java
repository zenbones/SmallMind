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
package org.smallmind.phalanx.wire;

import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.phalanx.wire.signal.WireContext;

/**
 * {@link ParameterExtractor} that always returns a fixed, pre-configured constant value,
 * regardless of the invoked method or its arguments.
 *
 * <p>Use this when a routing or context parameter is known at configuration time and does
 * not depend on runtime invocation details, such as a static tenant identifier or a
 * hard-coded routing key.</p>
 *
 * @param <T> the type of the constant parameter value
 */
public class StaticParameterExtractor<T> implements ParameterExtractor<T> {

  private final T parameter;

  /**
   * Constructs an extractor that always returns the given constant value.
   *
   * @param parameter the value returned by every
   *                  {@link #getParameter(Method, HashMap, WireContext...)} call;
   *                  may be {@code null}
   */
  public StaticParameterExtractor (T parameter) {

    this.parameter = parameter;
  }

  /**
   * Returns the constant value supplied at construction time.
   *
   * @param method       ignored
   * @param argumentMap  ignored
   * @param wireContexts ignored
   * @return the constant parameter value; may be {@code null}
   * @throws MissingInstanceIdException never thrown by this implementation
   */
  @Override
  public T getParameter (Method method, HashMap<String, Object> argumentMap, WireContext... wireContexts)
    throws MissingInstanceIdException {

    return parameter;
  }
}
