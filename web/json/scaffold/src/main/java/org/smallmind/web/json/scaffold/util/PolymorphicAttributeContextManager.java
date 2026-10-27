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

import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;

/**
 * Manages the application-wide JSON attribute name used to carry the polymorphic type key during
 * marshaling and unmarshaling, stored in the {@link PerApplicationContext} attached to the calling thread. Until
 * {@link #setPolymorphicAttributeName} has been called in a context, {@link #getPolymorphicAttributeName} returns
 * {@link AttributedPolymorphicXmlAdapter#getDefaultPolymorphicAttributeName()} for that context.
 */
public class PolymorphicAttributeContextManager implements PerApplicationDataManager {

  /**
   * @return the polymorphic attribute name stored in the current context, or the default when none has been stored
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if no per-application context is attached to the current thread
   */
  public static String getPolymorphicAttributeName () {

    String polymorphicAttributeName;

    if ((polymorphicAttributeName = findPolymorphicAttributeName()) == null) {

      return AttributedPolymorphicXmlAdapter.getDefaultPolymorphicAttributeName();
    }

    return polymorphicAttributeName;
  }

  /**
   * Stores the polymorphic attribute name used during marshaling and unmarshaling in the per-application context
   * attached to the current thread.
   *
   * @param polymorphicAttributeName new attribute name
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if no per-application context is attached to the current thread
   */
  public static void setPolymorphicAttributeName (String polymorphicAttributeName) {

    PerApplicationContext.setPerApplicationData(PolymorphicAttributeContextManager.class, polymorphicAttributeName);
  }

  /**
   * @return the polymorphic attribute name stored in the current context, or {@code null} when none has been stored
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if no per-application context is attached to the current thread
   */
  public static String findPolymorphicAttributeName () {

    return PerApplicationContext.getPerApplicationData(PolymorphicAttributeContextManager.class, String.class);
  }
}
