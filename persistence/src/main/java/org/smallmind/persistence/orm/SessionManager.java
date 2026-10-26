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
package org.smallmind.persistence.orm;

import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Registry of {@link ProxySession} instances that bridges {@link SessionScopeManager} and
 * {@link SessionContextManager}.
 * <p>
 * {@code register} writes to every per-application registry bound on the calling thread: the scope flavor when a
 * {@link PerApplicationScope} is bound, and the context flavor when a {@link PerApplicationContext} is attached, so a
 * registration made from a thread that has both is visible to threads that later carry only one of them. Lookups read
 * the scope flavor first and fall back to the context flavor when the scope holds nothing and a context is attached,
 * so a thread bound to a scope that was never written still finds what was registered through the context. A thread
 * with neither bound delegates to the context flavor and fails with {@code MissingPerApplicationContextException}.
 * Nothing is ever written to a registry whose binding is absent.
 */
public class SessionManager {

  /**
   * Registers a proxy session in every per-application registry bound on the current thread.
   *
   * @param sessionSourceKey the key that identifies the session; {@code null} registers the default unnamed source, which the transaction boundaries match only by {@code null}
   * @param proxySession     the session to register
   * @throws IllegalArgumentException if the key is the empty string, which is reserved as the stored key of the default unnamed source
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void register (String sessionSourceKey, ProxySession proxySession) {

    if (PerApplicationScope.isBound()) {
      SessionScopeManager.register(sessionSourceKey, proxySession);
      if (PerApplicationContext.isBound()) {
        SessionContextManager.register(sessionSourceKey, proxySession);
      }
    } else {
      SessionContextManager.register(sessionSourceKey, proxySession);
    }
  }

  /**
   * Retrieves the default proxy session.
   *
   * @return the default proxy session
   * @throws ORMInitializationException if no default session is registered in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static ProxySession getSession () {

    return getSession(null);
  }

  /**
   * Retrieves the default proxy session.
   *
   * @return the default proxy session
   * @throws ORMInitializationException if no default session is registered in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static ProxySession getDefaultSession () {

    return getSession(null);
  }

  /**
   * Retrieves a proxy session by key, scope flavor first, then context flavor.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source (the empty string, its stored key, resolves to it as well, but is refused by {@code register})
   * @return the matching proxy session
   * @throws ORMInitializationException                                          if no session is registered for the key in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static ProxySession getSession (String sessionSourceKey) {

    ProxySession proxySession;

    if ((proxySession = findSession(sessionSourceKey)) == null) {
      throw new ORMInitializationException("No ProxySession was mapped to the data source value(%s)", sessionSourceKey);
    }

    return proxySession;
  }

  /**
   * Looks up a proxy session by key, scope flavor first, then context flavor, without failing when none is registered.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source (the empty string, its stored key, resolves to it as well, but is refused by {@code register})
   * @return the matching proxy session, or {@code null} if none is registered for the key in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the current thread; only an unregistered key yields {@code null}
   */
  public static ProxySession findSession (String sessionSourceKey) {

    if (PerApplicationScope.isBound()) {

      ProxySession proxySession;

      if ((proxySession = SessionScopeManager.findSession(sessionSourceKey)) != null) {

        return proxySession;
      }
      if (!PerApplicationContext.isBound()) {

        return null;
      }
    }

    return SessionContextManager.findSession(sessionSourceKey);
  }

  /**
   * Closes the default proxy session.
   *
   * @throws ORMInitializationException if no default session is registered in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void closeSession () {

    closeSession(null);
  }

  /**
   * Closes the proxy session registered under the given key.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source (the empty string, its stored key, resolves to it as well, but is refused by {@code register})
   * @throws ORMInitializationException if no session is registered for the key in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void closeSession (String sessionSourceKey) {

    getSession(sessionSourceKey).close();
  }
}
