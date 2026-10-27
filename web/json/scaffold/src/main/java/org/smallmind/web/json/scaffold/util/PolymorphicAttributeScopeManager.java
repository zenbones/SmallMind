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
package org.smallmind.web.json.scaffold.util;

import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;

/**
 * Stores the polymorphic discriminator attribute name through a {@link PerApplicationScope}. This is the scope
 * flavor of {@link PolymorphicAttributeContextManager}: the calling thread must be inside the scope's {@code run} /
 * {@code call}, an {@code ApplicationCarrier}, or a thread created by {@code PerApplicationScope.wrapThreadFactory},
 * otherwise {@code MissingPerApplicationScopeException} is thrown.
 * <p>
 * Like the context flavor, this class has no static initializer; until {@link #setPolymorphicAttributeName}
 * has been called in a scope, {@link #getPolymorphicAttributeName} returns
 * {@link AttributedPolymorphicXmlAdapter#getDefaultPolymorphicAttributeName()} for that scope. Library code in this
 * module ({@code AttributedPolymorphicXmlAdapter}) resolves through {@link PolymorphicAttributeManager}, which writes
 * here whenever a scope is bound on the calling thread and reads here first.
 */
public class PolymorphicAttributeScopeManager implements PerApplicationDataManager {

  /**
   * Returns the attribute name stored in the per-application scope bound to the current thread, or the adapter's
   * default name if none has been stored.
   *
   * @return the polymorphic attribute name for the current scope
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static String getPolymorphicAttributeName () {

    String polymorphicAttributeName;

    if ((polymorphicAttributeName = findPolymorphicAttributeName()) == null) {

      return AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName();
    }

    return polymorphicAttributeName;
  }

  /**
   * Stores the attribute name in the per-application scope bound to the current thread.
   *
   * @param polymorphicAttributeName the attribute name to use for this scope
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static void setPolymorphicAttributeName (String polymorphicAttributeName) {

    PerApplicationScope.setPerApplicationData(PolymorphicAttributeScopeManager.class, polymorphicAttributeName);
  }

  /**
   * @return the polymorphic attribute name stored in the bound scope, or {@code null} when none has been stored
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread
   */
  public static String findPolymorphicAttributeName () {

    return PerApplicationScope.getPerApplicationData(PolymorphicAttributeScopeManager.class, String.class);
  }
}
