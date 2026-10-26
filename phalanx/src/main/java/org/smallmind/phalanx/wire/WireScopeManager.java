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
import org.smallmind.phalanx.wire.signal.WireScope;

/**
 * Per-application registry mapping wire handles to {@link WireScope} classes, stored in the
 * {@link PerApplicationScope} bound to the calling thread. This is the scope counterpart of
 * {@link WireContextManager}, which keeps the same kind of registry for {@code WireContext} classes in
 * {@code PerApplicationContext}.
 * <p>
 * Both methods resolve the registry through {@code PerApplicationScope.getPerApplicationData}, so the calling thread
 * must be inside the scope's {@code run} / {@code call}, an {@code ApplicationCarrier}, a task handed through
 * {@code wrapExecutor} / {@code boundExecutor}, or a thread created by {@code wrapThreadFactory} /
 * {@code boundThreadFactory}; on an unbound thread they throw {@code MissingPerApplicationScopeException}. Each
 * scope instance owns its own registry, so registrations made in one scope are invisible in another.
 * {@link org.smallmind.phalanx.wire.signal.WireScopeXmlAdapter} resolves wire tags through this registry when it
 * unmarshals an invocation.
 */
public class WireScopeManager implements PerApplicationDataManager {

  /**
   * Registers a wire scope class under the given handle in the per-application scope bound to the current thread,
   * creating the registry on first use. Registering a handle again replaces the earlier class.
   *
   * @param handle     the handle that identifies the scope class on the wire
   * @param scopeClass the scope class to register
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static void register (String handle, Class<? extends WireScope> scopeClass) {

    ConcurrentHashMap<String, Class<? extends WireScope>> wireScopeMap;

    if ((wireScopeMap = PerApplicationScope.getPerApplicationData(WireScopeManager.class, ConcurrentHashMap.class)) == null) {
      wireScopeMap = PerApplicationScope.setPerApplicationDataIfAbsent(WireScopeManager.class, new ConcurrentHashMap<>());
    }
    wireScopeMap.put(handle, scopeClass);
  }

  /**
   * Returns the wire scope class registered under the given handle in the per-application scope bound to the
   * current thread.
   *
   * @param handle the handle that identifies the scope class on the wire
   * @return the registered class, or {@code null} if the handle is unknown or nothing has been registered in this scope
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static Class<? extends WireScope> getScopeClass (String handle) {

    ConcurrentHashMap<String, Class<? extends WireScope>> wireScopeMap;

    if ((wireScopeMap = PerApplicationScope.getPerApplicationData(WireScopeManager.class, ConcurrentHashMap.class)) != null) {

      return wireScopeMap.get(handle);
    }

    return null;
  }
}
