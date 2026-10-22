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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Application-scoped registry for {@link ProxySession} instances keyed by a session source, resolved through a
 * {@link PerApplicationScope}. This is the scope flavor of {@link SessionContextManager}: the calling thread must be
 * inside the scope's {@code run} / {@code call}, an {@code ApplicationCarrier}, or a thread created by
 * {@code PerApplicationScope.wrapThreadFactory}, otherwise {@code MissingPerApplicationScopeException} is thrown.
 * Library code in this module ({@code ProxySession.register()} and the session boundaries) resolves through the
 * context flavor until this module is cut over.
 */
public class SessionScopeManager implements PerApplicationDataManager, ApplicationContextAware {

  /**
   * Registers a proxy session for the given key in the per-application scope bound to the current thread.
   *
   * @param sessionSourceKey the key that identifies the session
   * @param proxySession     the session instance
   */
  public static void register (String sessionSourceKey, ProxySession proxySession) {

    ConcurrentHashMap<String, ProxySession> sessionMap;

    if ((sessionMap = PerApplicationScope.getPerApplicationData(SessionScopeManager.class, ConcurrentHashMap.class)) == null) {
      PerApplicationScope.setPerApplicationData(SessionScopeManager.class, sessionMap = new ConcurrentHashMap<>());
    }
    sessionMap.put(sessionSourceKey, proxySession);
  }

  /**
   * Returns the session registered under the {@code null} key.
   *
   * @return the default proxy session
   * @throws ORMInitializationException if no session is registered under the {@code null} key
   */
  public static ProxySession getSession () {

    return getSession(null);
  }

  /**
   * Returns the session registered under the {@code null} key.
   *
   * @return the default proxy session
   * @throws ORMInitializationException if no session is registered under the {@code null} key
   */
  public static ProxySession getDefaultSession () {

    return getSession(null);
  }

  /**
   * Returns the session registered under the given key.
   *
   * @param sessionSourceKey the key that identifies the session
   * @return the registered proxy session
   * @throws ORMInitializationException if no session is registered under the key
   */
  public static ProxySession getSession (String sessionSourceKey) {

    ProxySession proxySession;

    if ((proxySession = (ProxySession)PerApplicationScope.getPerApplicationData(SessionScopeManager.class, ConcurrentHashMap.class).get(sessionSourceKey)) == null) {
      throw new ORMInitializationException("No ProxySession was mapped to the data source value(%s)", sessionSourceKey);
    }

    return proxySession;
  }

  /**
   * Closes the session registered under the {@code null} key.
   */
  public static void closeSession () {

    closeSession(null);
  }

  /**
   * Closes the session registered under the given key.
   *
   * @param sessionSourceKey the key that identifies the session
   */
  public static void closeSession (String sessionSourceKey) {

    getSession(sessionSourceKey).close();
  }

  /**
   * Replaces the session registry of the per-application scope bound to the current thread with an empty map.
   *
   * @param applicationContext the Spring application context (unused)
   * @throws BeansException never thrown by this implementation
   */
  @Override
  public void setApplicationContext (ApplicationContext applicationContext)
    throws BeansException {

    PerApplicationScope.setPerApplicationData(SessionScopeManager.class, new ConcurrentHashMap<String, ProxySession>());
  }
}
