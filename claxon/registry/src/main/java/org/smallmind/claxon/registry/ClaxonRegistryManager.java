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

import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Holder for the per-application {@link ClaxonRegistry} that bridges {@link ClaxonRegistryScopeManager} and
 * {@link ClaxonRegistryContextManager}.
 * <p>
 * {@code register} writes to every per-application registry bound on the calling thread: the scope flavor when a
 * {@link PerApplicationScope} is bound, and the context flavor when a {@link PerApplicationContext} is attached, so a
 * registration made from a thread that has both is visible to threads that later carry only one of them. Lookups read
 * the scope flavor first and fall back to the context flavor when the scope holds nothing and a context is attached,
 * so a thread bound to a scope that was never written still finds what was registered through the context. Unlike the
 * other bridging managers, a lookup on a thread with neither bound returns {@code null} rather than throwing, so that
 * {@link Instrument#with} degrades to an {@code UnpluggedInstrumentation} and metrics never fail the caller;
 * {@link #register} on such a thread still fails with {@code MissingPerApplicationContextException}. Nothing is ever
 * written to a registry whose binding is absent.
 */
public class ClaxonRegistryManager {

  /**
   * Installs {@code registry} in every per-application registry bound on the current thread.
   *
   * @param registry the registry to install
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void register (ClaxonRegistry registry) {

    if (PerApplicationScope.isBound()) {
      ClaxonRegistryScopeManager.register(registry);
      if (PerApplicationContext.isBound()) {
        ClaxonRegistryContextManager.register(registry);
      }
    } else {
      ClaxonRegistryContextManager.register(registry);
    }
  }

  /**
   * Returns the installed {@link ClaxonRegistry}, scope flavor first, then context flavor.
   *
   * @return the installed {@link ClaxonRegistry}, or {@code null} if none has been installed in any bound registry, or
   * if no per-application scope or context is bound on the current thread
   */
  public static ClaxonRegistry getRegistry () {

    if (!(PerApplicationScope.isBound() || PerApplicationContext.isBound())) {

      return null;
    }
    if (PerApplicationScope.isBound()) {

      ClaxonRegistry registry;

      if ((registry = ClaxonRegistryScopeManager.getRegistry()) != null) {

        return registry;
      }
      if (!PerApplicationContext.isBound()) {

        return null;
      }
    }

    return ClaxonRegistryContextManager.getRegistry();
  }
}
