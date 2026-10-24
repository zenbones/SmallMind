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
package org.smallmind.claxon.registry;

import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Stores and retrieves the per-application {@link ClaxonRegistry} through a {@link PerApplicationScope}. This is the
 * scope flavor of {@link ClaxonRegistryContextManager}: the calling thread must be inside the scope's {@code run} /
 * {@code call}, an {@code ApplicationCarrier}, or a thread created by {@code PerApplicationScope.wrapThreadFactory},
 * otherwise {@code MissingPerApplicationScopeException} is thrown. {@link Instrument} resolves the registry through
 * the context flavor until this module is cut over.
 */
public class ClaxonRegistryScopeManager implements PerApplicationDataManager {

  /**
   * Installs {@code registry} as the {@link ClaxonRegistry} of the per-application scope bound to the current thread.
   *
   * @param registry the registry to install for the current application scope
   */
  public static void register (ClaxonRegistry registry) {

    PerApplicationScope.setPerApplicationData(ClaxonRegistryScopeManager.class, registry);
  }

  /**
   * Returns the {@link ClaxonRegistry} installed in the per-application scope bound to the current thread, or
   * {@code null} if none has been installed.
   *
   * @return the installed {@link ClaxonRegistry}, or {@code null}
   */
  public static ClaxonRegistry getRegistry () {

    return PerApplicationScope.getPerApplicationData(ClaxonRegistryScopeManager.class, ClaxonRegistry.class);
  }
}
