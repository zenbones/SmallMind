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
package org.smallmind.quorum.namespace;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import org.smallmind.nutsnbolts.util.StringUtility;
import org.smallmind.quorum.namespace.backingStore.ContextCreator;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;
import org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails;

/**
 * JNDI {@link ObjectFactory} registered for the {@code java:} URL scheme that constructs
 * {@link JavaContext} instances configured from the JNDI environment.
 * <p>
 * The factory is invoked by the JNDI framework when an initial context with a {@code java:} URL
 * is requested. It reads the following environment keys from the supplied {@code environment}:
 * <ul>
 *   <li>{@link JavaContext#CONTEXT_STORE} — the short backing-store identifier (e.g., {@code ldap});
 *       used to locate the concrete {@link ContextCreator} and {@link NameTranslator} classes by
 *       convention: {@code <package>.<store>.<Store>ContextCreator} and
 *       {@code <package>.<store>.<Store>NameTranslator}.</li>
 *   <li>{@link JavaContext#CONNECTION_DETAILS} — a {@link NamingConnectionDetails} instance passed
 *       to the {@link ContextCreator} constructor.</li>
 *   <li>{@link JavaContext#CONTEXT_MODIFIABLE} — {@code "true"} to allow mutations; defaults to
 *       read-only.</li>
 *   <li>{@link JavaContext#POOLED_CONNECTION} — {@code "true"} to return pooled child contexts;
 *       defaults to unpooled.</li>
 * </ul>
 * When {@code obj} is non-null the factory returns {@code null} immediately, deferring to the
 * existing object.
 */
public class javaURLContextFactory implements ObjectFactory {

  private static final Class[] CONTEXT_CREATOR_SIGNATURE = new Class[] {NamingConnectionDetails.class};
  private static final Class[] NAME_TRANSLATOR_SIGNATURE = new Class[] {ContextCreator.class};

  /**
   * Creates and returns a new {@link JavaContext} when {@code obj} is {@code null}, or returns
   * {@code null} when an existing object is provided.
   * <p>
   * The {@link ContextCreator} and {@link NameTranslator} implementations are located by convention
   * using the {@code CONTEXT_STORE} environment key: the class names are constructed as
   * {@code <parentPackage>.<store>.<Store>ContextCreator} and
   * {@code <parentPackage>.<store>.<Store>NameTranslator}. Both are instantiated via reflection
   * using their single-argument constructors.
   *
   * @param obj         an existing object to re-use; when non-null the factory returns {@code null}
   * @param name        the JNDI name of the object (not used by this factory)
   * @param nameCtx     the context in which {@code name} is bound (not used by this factory)
   * @param environment the JNDI environment containing the keys listed in the class description
   * @return a new {@link JavaContext} if {@code obj} is {@code null}, otherwise {@code null}
   * @throws Exception if any reflective class lookup, constructor resolution, or instantiation fails
   */
  public Object getObjectInstance (Object obj, Name name, Context nameCtx, Hashtable environment)
    throws Exception {

    ContextCreator contextCreator;
    NameTranslator nameTranslator;
    Class contextCreatorClass;
    Class nameTranslatorClass;
    Constructor contextCreatorConstructor;
    Constructor nameTranslatorConstructor;
    String backingStore;
    boolean modifiable = false;
    boolean pooled = false;

    if (obj == null) {
      if (environment.containsKey(JavaContext.CONTEXT_MODIFIABLE)) {
        if ((environment.get(JavaContext.CONTEXT_MODIFIABLE)).equals("true")) {
          modifiable = true;
        }
      }
      if (environment.containsKey(JavaContext.POOLED_CONNECTION)) {
        if ((environment.get(JavaContext.POOLED_CONNECTION)).equals("true")) {
          pooled = true;
        }
      }

      backingStore = (String)environment.get(JavaContext.CONTEXT_STORE);

      contextCreatorClass = Class.forName(ContextCreator.class.getPackage().getName() + '.' + backingStore + '.' + StringUtility.toDisplayCase(backingStore) + ContextCreator.class.getSimpleName());
      contextCreatorConstructor = contextCreatorClass.getConstructor(CONTEXT_CREATOR_SIGNATURE);
      contextCreator = (ContextCreator)contextCreatorConstructor.newInstance(environment.get(JavaContext.CONNECTION_DETAILS));

      nameTranslatorClass = Class.forName(NameTranslator.class.getPackage().getName() + '.' + backingStore + '.' + StringUtility.toDisplayCase(backingStore) + NameTranslator.class.getSimpleName());
      nameTranslatorConstructor = nameTranslatorClass.getConstructor(NAME_TRANSLATOR_SIGNATURE);
      nameTranslator = (NameTranslator)nameTranslatorConstructor.newInstance(contextCreator);

      return new JavaContext(nameTranslator, (Hashtable<String, Object>)environment, modifiable, pooled);
    }

    return null;
  }
}
