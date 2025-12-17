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

import java.util.Hashtable;
import javax.naming.InvalidNameException;
import javax.naming.directory.DirContext;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

/**
 * Helper methods for converting naming enumeration elements between internal and external forms.
 */
public class NamingEnumerationUtility {

  /**
   * Converts an external string name to its internal representation using the translator.
   *
   * @param name           external string form
   * @param nameTranslator translator to apply
   * @return internal string form
   * @throws InvalidNameException if the name cannot be converted
   */
  protected static String convertName (String name, NameTranslator nameTranslator)
    throws InvalidNameException {

    return nameTranslator.fromExternalStringToInternalString(name);
  }

  /**
   * Converts a class name, swapping internal directory context types for the external {@link JavaContext}.
   *
   * @param className               class name to convert
   * @param internalDirContextClass backing directory context class
   * @return converted class name
   */
  protected static String convertClassName (String className, Class internalDirContextClass) {

    if (className != null) {
      if (className.equals(internalDirContextClass.getName())) {

        return JavaContext.class.getName();
      }
    }

    return className;
  }

  /**
   * Converts a bound object, wrapping directory contexts in {@link JavaContext} instances.
   *
   * @param boundObject             object bound in the naming enumeration
   * @param internalDirContextClass internal directory context class
   * @param environment             JNDI environment
   * @param nameTranslator          translator for names
   * @param nameParser              parser for names
   * @param modifiable              whether the context is modifiable
   * @return converted object
   */
  protected static Object convertObject (Object boundObject, Class internalDirContextClass, Hashtable<String, Object> environment, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

    if (boundObject != null) {
      if (boundObject.getClass().equals(internalDirContextClass)) {

        return new JavaContext(environment, (DirContext)boundObject, nameTranslator, nameParser, modifiable);
      }
    }
    return boundObject;
  }
}
