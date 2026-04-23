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
 * Package-private utility methods shared by {@link JavaNamingEnumeration} for converting
 * naming enumeration elements from backing-store-specific representations into the forms
 * expected by {@link JavaContext} callers.
 * <p>
 * Three conversions are provided: translating the element's name string, replacing the internal
 * directory context class name with the {@link JavaContext} class name, and wrapping a bound
 * {@link DirContext} object in a new {@link JavaContext} instance.
 */
public class NamingEnumerationUtility {

  /**
   * Converts a backing-store name string into its internal representation.
   *
   * @param name           the external name string returned by the backing store
   * @param nameTranslator the translator used to map external names to internal form
   * @return the internal path string for {@code name}
   * @throws InvalidNameException if {@code name} cannot be mapped to a valid internal form
   */
  protected static String convertName (String name, NameTranslator nameTranslator)
    throws InvalidNameException {

    return nameTranslator.fromExternalStringToInternalString(name);
  }

  /**
   * Replaces the backing-store context class name with the {@link JavaContext} class name.
   * <p>
   * When a naming enumeration element reports a class name that equals the runtime class of the
   * internal directory context, callers should see {@link JavaContext} instead. All other class
   * names are returned unchanged.
   *
   * @param className               the class name reported by the enumeration element
   * @param internalDirContextClass the runtime class of the backing-store's directory context
   * @return {@code JavaContext.class.getName()} if {@code className} matches the internal
   * context class, otherwise {@code className} unchanged
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
   * Wraps a bound object in a {@link JavaContext} if it is an instance of the internal
   * directory context class; otherwise returns the object unchanged.
   *
   * @param boundObject             the object bound to the name in the backing store
   * @param internalDirContextClass the runtime class of the backing-store's directory context
   * @param environment             the JNDI environment to pass to the new {@link JavaContext}
   * @param nameTranslator          the name translator to pass to the new {@link JavaContext}
   * @param nameParser              the name parser to pass to the new {@link JavaContext}
   * @param modifiable              whether the new {@link JavaContext} should allow mutations
   * @return a new {@link JavaContext} wrapping {@code boundObject} if it is a directory context,
   * or {@code boundObject} itself if it is not
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
