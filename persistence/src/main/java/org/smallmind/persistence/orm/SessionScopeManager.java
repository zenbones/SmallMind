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

import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Application-scoped registry for {@link ProxySession} instances keyed by a session source, resolved through a
 * {@link PerApplicationScope}. This is the scope flavor of {@link SessionContextManager}: the calling thread must be
 * inside the scope's {@code run} / {@code call}, an {@code ApplicationCarrier}, or a thread created by
 * {@code PerApplicationScope.wrapThreadFactory} or the scope's own {@code boundThreadFactory} / {@code boundExecutor},
 * otherwise {@code MissingPerApplicationScopeException} is thrown.
 * Library code in this module ({@code ProxySession.register()}) resolves through {@link SessionManager}, which writes
 * here whenever a scope is bound on the calling thread and reads here first.
 */
public class SessionScopeManager implements PerApplicationDataManager {

  private static final String DEFAULT_SESSION_SOURCE_KEY = "";

  /**
   * Registers a proxy session for the given key in the per-application scope bound to the current thread.
   *
   * @param sessionSourceKey the key that identifies the session; {@code null} registers the default unnamed source, which the transaction boundaries match only by {@code null}
   * @param proxySession     the session instance
   * @throws IllegalArgumentException if the key is the empty string, which is reserved as the stored key of the default unnamed source
   */
  public static void register (String sessionSourceKey, ProxySession proxySession) {

    ConcurrentHashMap<String, ProxySession> sessionMap;

    if (DEFAULT_SESSION_SOURCE_KEY.equals(sessionSourceKey)) {
      throw new IllegalArgumentException("The empty string is reserved as the stored key of the default unnamed source; register that source with a null key");
    }
    if ((sessionMap = getSessionMap()) == null) {
      sessionMap = PerApplicationScope.setPerApplicationDataIfAbsent(SessionScopeManager.class, new ConcurrentHashMap<>());
    }
    sessionMap.put(toMapKey(sessionSourceKey), proxySession);
  }

  /**
   * Returns the session registered as the default unnamed source.
   *
   * @return the default proxy session
   * @throws ORMInitializationException if no default session is registered
   */
  public static ProxySession getSession () {

    return getSession(null);
  }

  /**
   * Returns the session registered as the default unnamed source.
   *
   * @return the default proxy session
   * @throws ORMInitializationException if no default session is registered
   */
  public static ProxySession getDefaultSession () {

    return getSession(null);
  }

  /**
   * Returns the session registered under the given key.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source
   * @return the registered proxy session
   * @throws ORMInitializationException if no session is registered under the key
   */
  public static ProxySession getSession (String sessionSourceKey) {

    ProxySession proxySession;

    if ((proxySession = findSession(sessionSourceKey)) == null) {
      throw new ORMInitializationException("No ProxySession was mapped to the data source value(%s)", sessionSourceKey);
    }

    return proxySession;
  }

  /**
   * Looks up a proxy session by key without failing when none is registered.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source (the empty string, its stored key, resolves to it as well, but is refused by {@code register})
   * @return the matching proxy session, or {@code null} if none is registered for the key
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread; only an unregistered key yields {@code null}
   */
  public static ProxySession findSession (String sessionSourceKey) {

    ConcurrentHashMap<String, ProxySession> sessionMap;

    return ((sessionMap = getSessionMap()) == null) ? null : sessionMap.get(toMapKey(sessionSourceKey));
  }

  /**
   * Closes the session registered as the default unnamed source.
   */
  public static void closeSession () {

    closeSession(null);
  }

  /**
   * Closes the session registered under the given key.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source
   */
  public static void closeSession (String sessionSourceKey) {

    getSession(sessionSourceKey).close();
  }

  private static String toMapKey (String sessionSourceKey) {

    return (sessionSourceKey == null) ? DEFAULT_SESSION_SOURCE_KEY : sessionSourceKey;
  }

  private static ConcurrentHashMap<String, ProxySession> getSessionMap () {

    return PerApplicationScope.getPerApplicationData(SessionScopeManager.class, ConcurrentHashMap.class);
  }
}
