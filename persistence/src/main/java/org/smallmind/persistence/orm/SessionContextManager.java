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
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;

/**
 * Application-scoped registry for {@link ProxySession} instances keyed by a session source.
 */
public class SessionContextManager implements PerApplicationDataManager {

  private static final String DEFAULT_SESSION_SOURCE_KEY = "";

  /**
   * Registers a proxy session for the given key.
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
      sessionMap = PerApplicationContext.setPerApplicationDataIfAbsent(SessionContextManager.class, new ConcurrentHashMap<>());
    }
    sessionMap.put(toMapKey(sessionSourceKey), proxySession);
  }

  /**
   * Retrieves the default proxy session.
   *
   * @return proxy session registered under the default key
   * @throws ORMInitializationException if no default session is registered
   */
  public static ProxySession getSession () {

    return getSession(null);
  }

  /**
   * Retrieves the registered proxy session for the default key.
   *
   * @return default proxy session
   * @throws ORMInitializationException if no session is registered
   */
  public static ProxySession getDefaultSession () {

    return getSession(null);
  }

  /**
   * Retrieves a proxy session by key.
   *
   * @param sessionSourceKey the key used during registration; may be {@code null} for the default unnamed source (the empty string, its stored key, resolves to it as well, but is refused by {@code register})
   * @return the matching proxy session
   * @throws ORMInitializationException if no session is registered for the key
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
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if no per-application context is attached to the current thread; only an unregistered key yields {@code null}
   */
  public static ProxySession findSession (String sessionSourceKey) {

    ConcurrentHashMap<String, ProxySession> sessionMap;

    return ((sessionMap = getSessionMap()) == null) ? null : sessionMap.get(toMapKey(sessionSourceKey));
  }

  /**
   * Closes the default proxy session.
   */
  public static void closeSession () {

    closeSession(null);
  }

  /**
   * Closes the session registered for the provided key.
   *
   * @param sessionSourceKey the key identifying the session to close; may be {@code null} for the default unnamed source (the empty string, its stored key, resolves to it as well, but is refused by {@code register})
   */
  public static void closeSession (String sessionSourceKey) {

    getSession(sessionSourceKey).close();
  }

  private static String toMapKey (String sessionSourceKey) {

    return (sessionSourceKey == null) ? DEFAULT_SESSION_SOURCE_KEY : sessionSourceKey;
  }

  private static ConcurrentHashMap<String, ProxySession> getSessionMap () {

    return PerApplicationContext.getPerApplicationData(SessionContextManager.class, ConcurrentHashMap.class);
  }
}
