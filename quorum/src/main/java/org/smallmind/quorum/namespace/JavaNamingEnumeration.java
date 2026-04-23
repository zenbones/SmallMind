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
import java.util.NoSuchElementException;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

/**
 * Translating wrapper around a backing-store {@link NamingEnumeration} that converts each element
 * into the external form expected by {@link JavaContext} callers.
 * <p>
 * On every {@link #next()} call the wrapper:
 * <ol>
 *   <li>Translates the element's name string via
 *       {@link NamingEnumerationUtility#convertName(String, NameTranslator)}.</li>
 *   <li>Replaces the backing-store context class name with {@code JavaContext} via
 *       {@link NamingEnumerationUtility#convertClassName(String, Class)}.</li>
 *   <li>Wraps any bound {@link javax.naming.directory.DirContext} in a new {@link JavaContext} via
 *       {@link NamingEnumerationUtility#convertObject}.</li>
 * </ol>
 * The converted element is then reconstructed via reflection using the known constructor signatures
 * for {@link SearchResult}, {@link Binding}, and {@link NameClassPair}.
 *
 * @param <T> the enumeration element type ({@link NameClassPair}, {@link Binding}, or {@link SearchResult})
 */
public class JavaNamingEnumeration<T> implements NamingEnumeration<T> {

  private static final Class[] SEARCH_RESULT_SIGNATURE = new Class[] {String.class, String.class, Object.class, Attributes.class, Boolean.class};
  private static final Class[] BINDING_SIGNATURE = new Class[] {String.class, String.class, Object.class, Boolean.class};
  private static final Class[] NAME_CLASS_PAIR_SIGNATURE = new Class[] {String.class, String.class, Boolean.class};

  private final NamingEnumeration internalEnumeration;
  private final Class<T> typeClass;
  private final Class internalDirContextClass;
  private final Hashtable<String, Object> environment;
  private final NameTranslator nameTranslator;
  private final JavaNameParser nameParser;
  private final boolean modifiable;

  /**
   * Creates an enumeration that wraps and translates the given backing-store enumeration.
   *
   * @param typeClass               the runtime class of the elements to produce
   * @param internalEnumeration     the backing-store enumeration to wrap
   * @param internalDirContextClass the runtime class of the backing-store directory context, used
   *                                to detect and wrap nested contexts
   * @param environment             the JNDI environment passed to any {@link JavaContext} created
   *                                while wrapping nested contexts
   * @param nameTranslator          the translator used to convert name strings to internal form
   * @param nameParser              the name parser passed to any {@link JavaContext} created while
   *                                wrapping nested contexts
   * @param modifiable              whether new {@link JavaContext} instances for nested contexts
   *                                should allow mutations
   */
  public JavaNamingEnumeration (Class<T> typeClass, NamingEnumeration<T> internalEnumeration, Class internalDirContextClass, Hashtable<String, Object> environment, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

    this.typeClass = typeClass;
    this.internalEnumeration = internalEnumeration;
    this.internalDirContextClass = internalDirContextClass;
    this.environment = environment;
    this.nameTranslator = nameTranslator;
    this.nameParser = nameParser;
    this.modifiable = modifiable;
  }

  /**
   * Returns {@code true} if the enumeration has more elements, swallowing any
   * {@link NamingException} as {@code false}.
   *
   * @return {@code true} if {@link #next()} would return an element without throwing
   */
  public boolean hasMoreElements () {

    try {
      return hasMore();
    } catch (NamingException n) {
      return false;
    }
  }

  /**
   * Returns the next translated element, converting any {@link NamingException} into a
   * {@link NoSuchElementException}.
   *
   * @return the next element in translated form
   * @throws NoSuchElementException if there are no more elements or if the underlying
   *                                enumeration throws {@link NamingException}
   */
  public T nextElement () {

    try {
      return next();
    } catch (NamingException n) {
      throw new NoSuchElementException("There are no more items within this Enumeration");
    }
  }

  /**
   * Retrieves and translates the next element from the backing-store enumeration.
   * <p>
   * The element's name, class name, and bound object are each converted using
   * {@link NamingEnumerationUtility}. The translated values are used to construct a new element
   * of the appropriate type via reflection. If the element type is not {@link SearchResult},
   * {@link Binding}, or {@link NameClassPair}, the original element is returned cast to {@code T}.
   *
   * @return the next element in translated form
   * @throws NamingException if the underlying enumeration fails, if name translation fails, or if
   *                         reflective construction of the translated element fails
   */
  public T next ()
    throws NamingException {

    NamingException namingException;
    Constructor<T> typeConstructor;
    Object enumObject;
    Object boundObject;
    Attributes resultAttributes;
    String name;
    String className;
    boolean isRelative;

    enumObject = internalEnumeration.next();
    isRelative = ((NameClassPair)enumObject).isRelative();
    name = NamingEnumerationUtility.convertName(((NameClassPair)enumObject).getName(), nameTranslator);
    className = NamingEnumerationUtility.convertClassName(((NameClassPair)enumObject).getClassName(), internalDirContextClass);

    try {
      if (enumObject instanceof SearchResult) {
        boundObject = NamingEnumerationUtility.convertObject(((SearchResult)enumObject).getObject(), internalDirContextClass, environment, nameTranslator, nameParser, modifiable);
        resultAttributes = ((SearchResult)enumObject).getAttributes();

        typeConstructor = typeClass.getConstructor(SEARCH_RESULT_SIGNATURE);

        return typeConstructor.newInstance(name, className, boundObject, resultAttributes, isRelative);
      }
      if (enumObject instanceof Binding) {
        boundObject = NamingEnumerationUtility.convertObject(((Binding)enumObject).getObject(), internalDirContextClass, environment, nameTranslator, nameParser, modifiable);

        typeConstructor = typeClass.getConstructor(BINDING_SIGNATURE);

        return typeConstructor.newInstance(name, className, boundObject, isRelative);
      } else if (enumObject instanceof NameClassPair) {
        typeConstructor = typeClass.getConstructor(NAME_CLASS_PAIR_SIGNATURE);

        return typeConstructor.newInstance(name, className, isRelative);
      }
    } catch (Exception e) {
      namingException = new NamingException(e.getMessage());
      namingException.setRootCause(e);
      throw namingException;
    }

    return typeClass.cast(enumObject);
  }

  /**
   * Returns {@code true} if the underlying enumeration has more elements.
   *
   * @return {@code true} if another element is available
   * @throws NamingException if the underlying enumeration throws while checking for more elements
   */
  public boolean hasMore ()
    throws NamingException {

    return internalEnumeration.hasMore();
  }

  /**
   * Closes the underlying backing-store enumeration and releases any associated resources.
   *
   * @throws NamingException if the underlying enumeration throws while closing
   */
  public void close ()
    throws NamingException {

    internalEnumeration.close();
  }
}
