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

import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.phalanx.wire.signal.WireContext;

/**
 * Per-application registry mapping wire handles to {@link WireContext} classes, resolved through a
 * {@link PerApplicationScope}. This is the scope flavor of {@link WireContextManager}: the calling thread must be
 * inside the scope's {@code run} / {@code call}, an {@code ApplicationCarrier}, or a thread created by
 * {@code PerApplicationScope.wrapThreadFactory}, otherwise {@code MissingPerApplicationScopeException} is thrown.
 * Library code in this module ({@code WireContextInitializingBean} and {@code WireContextXmlAdapter}) resolves
 * through the context flavor until this module is cut over.
 */
public class WireScopeManager implements PerApplicationDataManager {

  /**
   * Registers a wire context class under the given handle in the per-application scope bound to the current thread.
   *
   * @param handle       the handle that identifies the context class on the wire
   * @param contextClass the context class to register
   */
  public static void register (String handle, Class<? extends WireContext> contextClass) {

    ConcurrentHashMap<String, Class<? extends WireContext>> wireContextMap;

    if ((wireContextMap = PerApplicationScope.getPerApplicationData(WireScopeManager.class, ConcurrentHashMap.class)) == null) {
      PerApplicationScope.setPerApplicationData(WireScopeManager.class, wireContextMap = new ConcurrentHashMap<>());
    }
    wireContextMap.put(handle, contextClass);
  }

  /**
   * Returns the wire context class registered under the given handle.
   *
   * @param handle the handle that identifies the context class on the wire
   * @return the registered class, or {@code null} if the handle is unknown or nothing has been registered
   */
  public static Class<? extends WireContext> getContextClass (String handle) {

    ConcurrentHashMap<String, Class<? extends WireContext>> wireContextMap;

    if ((wireContextMap = PerApplicationScope.getPerApplicationData(WireScopeManager.class, ConcurrentHashMap.class)) != null) {

      return wireContextMap.get(handle);
    }

    return null;
  }
}
