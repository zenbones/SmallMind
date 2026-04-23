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
 * Strategy for deriving a typed parameter value from an active service invocation context.
 *
 * <p>Implementations inspect the invoked method, its resolved argument map, and any attached
 * {@link WireContext} objects to produce a single value. Typical uses include extracting
 * routing keys, tenant identifiers, or other cross-cutting values that the transport layer
 * requires without coupling directly to the service contract.</p>
 *
 * @param <T> the type of parameter value this extractor produces
 */
public interface ParameterExtractor<T> {

  /**
   * Derives a parameter value from the given invocation context.
   *
   * @param method       the method being invoked on the service interface
   * @param argumentMap  map of logical argument names to their resolved values; may be empty
   *                     but never {@code null}
   * @param wireContexts zero or more {@link WireContext} objects attached to the current wire
   *                     operation that may supply additional metadata
   * @return the extracted parameter value; may be {@code null}
   * @throws Exception if extraction fails for any reason
   */
  T getParameter (Method method, HashMap<String, Object> argumentMap, WireContext... wireContexts)
    throws Exception;
}
