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
package org.smallmind.phalanx.worker;

import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Logs a WARN for the start-time condition that most often signals a forgotten scope: the owner was given no
 * {@link PerApplicationScope}, yet the starting thread is itself bound to a scope (so the application is running
 * scope-only) and has no {@link PerApplicationContext} attached for the threads to inherit. On such threads Claxon
 * metrics are dropped and, on an owner that receives invocations, inbound wire context or scope elements fail to
 * decode. Nothing is logged when the starting thread carries a context (the threads inherit it) or when a scope was
 * given. A start on a thread with neither binding is deliberately not warned about: that is the legacy unwrapped
 * case, and the throwing lookups there (for example {@code WireContextManager}, {@code WireScopeManager},
 * {@code PoolManager}, the persistence and property-accessor bridges, and every {@code register}) fail loudly at
 * first use, although {@code Instrument.with} degrades to an unplugged instrumentation, so metrics alone are lost
 * silently. The check also cannot tell whether an inherited context's registry is empty or whether a
 * scope that was given was ever written to.
 */
public class BindingAdvisory {

  /**
   * Emits the WARN described above when the condition holds.
   *
   * @param loggingClass        the class under whose logger the warning is recorded
   * @param owner               a description of the owner for the message, such as {@code Work manager(…)}
   * @param perApplicationScope the scope the owner was given, or {@code null}
   */
  public static void warnIfUnbound (Class<?> loggingClass, String owner, PerApplicationScope perApplicationScope) {

    if ((perApplicationScope == null) && PerApplicationScope.isBound() && (!PerApplicationContext.isBound())) {
      LoggerManager.getLogger(loggingClass).warn("%s was started on a thread bound to a PerApplicationScope without being given that scope, and no PerApplicationContext is attached; the threads it starts will carry no per-application binding, so Claxon metrics recorded from them will be dropped and, on a transport that receives invocations, inbound WireContext or WireScope elements will fail to decode", owner);
    }
  }
}
