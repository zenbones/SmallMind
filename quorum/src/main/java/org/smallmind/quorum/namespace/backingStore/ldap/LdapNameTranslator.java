/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.quorum.namespace.backingStore.ldap;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import org.smallmind.quorum.namespace.JavaName;
import org.smallmind.quorum.namespace.backingStore.ContextCreator;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

public class LdapNameTranslator extends NameTranslator {

  private static final String LDAP_NODE_PREFIX = "cn=";

  public LdapNameTranslator (ContextCreator contextCreator) {

    super(contextCreator);
  }

  public JavaName fromInternalNameToExternalName (Name internalName)
    throws InvalidNameException {

    JavaName translatedName;
    int count;

    translatedName = new JavaName(this);
    for (count = 0; count < internalName.size(); count++) {
      translatedName.add(LDAP_NODE_PREFIX + internalName.get(count));
    }
    return translatedName;
  }

  public String fromExternalNameToExternalString (JavaName externalName) {

    StringBuilder externalBuilder;
    int count;

    externalBuilder = new StringBuilder();
    for (count = 0; count < externalName.size(); count++) {
      if (count > 0) {
        externalBuilder.insert(0, ',');
      }
      externalBuilder.insert(0, externalName.get(count));
    }
    return externalBuilder.toString();
  }

  public String fromAbsoluteExternalStringToInternalString (String externalName)
    throws InvalidNameException {

    return getInternalString(externalName, true);
  }

  public String fromExternalStringToInternalString (String externalName)
    throws InvalidNameException {

    return getInternalString(externalName, false);
  }

  private String getInternalString (String externalName, boolean absolute)
    throws InvalidNameException {

    StringBuilder internalBuilder;
    String[] parsedArray;
    String parsedName;
    String rootName;
    int insertPos = 0;
    int equalsPos;

    rootName = ((LdapContextCreator)getContextCreator()).getRoot();

    internalBuilder = new StringBuilder();
    if (absolute && externalName.equals(rootName)) {
      throw new InvalidNameException("Parameter (" + externalName + ") must designate a context below the Ldap root context (" + rootName + ")");
    } else if (absolute && externalName.endsWith(rootName)) {
      internalBuilder.append("java:");
      insertPos = 5;
      parsedArray = externalName.substring(0, externalName.length() - (rootName.length() + 1)).split(",", -1);
    } else {
      parsedArray = externalName.split(",", -1);
    }

    for (int count = 0; count < parsedArray.length; count++) {
      parsedName = parsedArray[count];
      if ((equalsPos = parsedName.indexOf('=')) >= 0) {
        if (count > 0) {
          internalBuilder.insert(insertPos, '/');
        }
        internalBuilder.insert(insertPos, parsedName.substring(equalsPos + 1).strip());
      } else {
        throw new InvalidNameException("Parameter (" + externalName + ") is not a proper distinguished Ldap name");
      }
    }

    return internalBuilder.toString();
  }
}
