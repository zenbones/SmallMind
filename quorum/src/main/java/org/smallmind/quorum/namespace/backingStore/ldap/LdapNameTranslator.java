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
package org.smallmind.quorum.namespace.backingStore.ldap;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import org.smallmind.quorum.namespace.JavaName;
import org.smallmind.quorum.namespace.backingStore.ContextCreator;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

/**
 * {@link NameTranslator} that maps between the {@code java:} namespace's internal path components
 * and LDAP distinguished names using the {@code cn=} (common name) attribute type.
 * <p>
 * Every internal path component {@code foo} is translated to an LDAP node {@code cn=foo}, and
 * the resulting components are written in reverse order (most specific first) when rendered as
 * a string, matching the LDAP convention for distinguished names.
 */
public class LdapNameTranslator extends NameTranslator {

  private static final String LDAP_NODE_PREFIX = "cn=";

  /**
   * Creates a translator backed by the supplied context creator.
   *
   * @param contextCreator the factory used to open initial LDAP directory contexts
   */
  public LdapNameTranslator (ContextCreator contextCreator) {

    super(contextCreator);
  }

  /**
   * Converts an internal {@link Name} into an LDAP-style {@link JavaName} by prefixing each
   * component with {@code cn=}.
   * <p>
   * The components are added in the same order as the internal name; reversal happens only when
   * the name is rendered to a string via {@link #fromExternalNameToExternalString}.
   *
   * @param internalName the internal name whose components are to be translated
   * @return a {@link JavaName} whose components are {@code cn=<component>} for each internal component
   * @throws InvalidNameException if the {@link JavaName} constructor or {@code add} rejects a component
   */
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

  /**
   * Renders an LDAP-style {@link JavaName} as a comma-separated distinguished name string.
   * <p>
   * Components are written in reverse order (index {@code size-1} down to {@code 0}), which
   * yields the standard LDAP DN format where the most specific component appears first.
   *
   * @param externalName the {@link JavaName} whose LDAP components are to be serialised
   * @return a comma-separated DN string with components in most-specific-first order
   */
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

  /**
   * Converts a fully qualified LDAP distinguished name — one that ends with the configured root DN
   * — into the equivalent internal {@code java:}-rooted path string.
   * <p>
   * The root DN is stripped from the end of {@code externalName} before the remaining components
   * are translated. Throws if {@code externalName} equals the root exactly (no sub-path) or does
   * not end with the root.
   *
   * @param externalName the absolute LDAP DN to convert, which must end with the configured root DN
   * @return an internal path string beginning with {@code java:}
   * @throws InvalidNameException if {@code externalName} equals the root DN, does not end with it,
   *                              or contains a component that lacks an {@code =} separator
   */
  public String fromAbsoluteExternalStringToInternalString (String externalName)
    throws InvalidNameException {

    return getInternalString(externalName, true);
  }

  /**
   * Converts a relative LDAP distinguished name into the equivalent internal path string.
   * <p>
   * Unlike {@link #fromAbsoluteExternalStringToInternalString}, no root DN is expected or stripped;
   * each component is decoded using its {@code =} separator and joined with {@code /}.
   *
   * @param externalName the relative LDAP DN to convert
   * @return an internal path string without a {@code java:} prefix
   * @throws InvalidNameException if any component of {@code externalName} lacks an {@code =} separator
   */
  public String fromExternalStringToInternalString (String externalName)
    throws InvalidNameException {

    return getInternalString(externalName, false);
  }

  /**
   * Shared implementation for both absolute and relative DN-to-internal-string conversion.
   * <p>
   * When {@code absolute} is {@code true} the method expects {@code externalName} to end with the
   * configured root DN, prepends {@code java:} to the result, and adjusts the insert position
   * accordingly. Components are split on commas and decoded by extracting the value after the first
   * {@code =} character in each component.
   *
   * @param externalName the LDAP DN string to convert
   * @param absolute     {@code true} to require and strip the root DN suffix and prepend {@code java:}
   * @return the internal path string
   * @throws InvalidNameException if the string does not conform to the expected LDAP DN format
   */
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
