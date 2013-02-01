/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.cloud.namespace.java;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.smallmind.cloud.namespace.java.backingStore.NameTranslator;

public class JavaNamingEnumeration<T> implements NamingEnumeration<T> {

  private static final Class[] SEARCH_RESULT_SIGNATURE = new Class[] {String.class, String.class, Object.class, Attributes.class, Boolean.class};
  private static final Class[] BINDING_SIGNATURE = new Class[] {String.class, String.class, Object.class, Boolean.class};
  private static final Class[] NAME_CLASS_PAIR_SIGNATURE = new Class[] {String.class, String.class, Boolean.class};

  private NamingEnumeration internalEnumeration;
  private Class<T> typeClass;
  private Class internalDirContextClass;
  private Hashtable<String, Object> environment;
  private NameTranslator nameTranslator;
  private JavaNameParser nameParser;
  private boolean modifiable;

  public JavaNamingEnumeration (Class<T> typeClass, NamingEnumeration<T> internalEnumeration, Class internalDirContextClass, Hashtable<String, Object> environment, NameTranslator nameTranslator, JavaNameParser nameParser, boolean modifiable) {

    this.typeClass = typeClass;
    this.internalEnumeration = internalEnumeration;
    this.internalDirContextClass = internalDirContextClass;
    this.environment = environment;
    this.nameTranslator = nameTranslator;
    this.nameParser = nameParser;
    this.modifiable = modifiable;
  }

  public boolean hasMoreElements () {

    try {
      return hasMore();
    }
    catch (NamingException n) {
      return false;
    }
  }

  public T nextElement () {

    try {
      return next();
    }
    catch (NamingException n) {
      throw new NoSuchElementException("There are no more items within this Enumeration");
    }
  }

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
    name = NamingEnumerationUtilities.convertName(((NameClassPair)enumObject).getName(), nameTranslator);
    className = NamingEnumerationUtilities.convertClassName(((NameClassPair)enumObject).getClassName(), internalDirContextClass);

    try {
      if (enumObject instanceof SearchResult) {
        boundObject = NamingEnumerationUtilities.convertObject(((SearchResult)enumObject).getObject(), internalDirContextClass, environment, nameTranslator, nameParser, modifiable);
        resultAttributes = ((SearchResult)enumObject).getAttributes();

        typeConstructor = typeClass.getConstructor(SEARCH_RESULT_SIGNATURE);

        return typeConstructor.newInstance(name, className, boundObject, resultAttributes, isRelative);
      }
      if (enumObject instanceof Binding) {
        boundObject = NamingEnumerationUtilities.convertObject(((Binding)enumObject).getObject(), internalDirContextClass, environment, nameTranslator, nameParser, modifiable);

        typeConstructor = typeClass.getConstructor(BINDING_SIGNATURE);

        return typeConstructor.newInstance(name, className, boundObject, isRelative);
      }
      else if (enumObject instanceof NameClassPair) {
        typeConstructor = typeClass.getConstructor(NAME_CLASS_PAIR_SIGNATURE);

        return typeConstructor.newInstance(name, className, isRelative);
      }
    }
    catch (Exception e) {
      namingException = new NamingException(e.getMessage());
      namingException.setRootCause(e);
      throw namingException;
    }

    return typeClass.cast(enumObject);
  }

  public boolean hasMore ()
    throws NamingException {

    return internalEnumeration.hasMore();
  }

  public void close ()
    throws NamingException {

    internalEnumeration.close();
  }

}
