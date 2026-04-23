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
package org.smallmind.quorum.namespace.backingStore;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.smallmind.quorum.namespace.ContextNamePair;
import org.smallmind.quorum.namespace.JavaName;

/**
 * Bridge between the {@code java:} namespace's internal name representation and the naming
 * conventions of a specific backing store (for example, LDAP distinguished names).
 * <p>
 * Implementations hold a {@link ContextCreator} that they use to open initial contexts and pair
 * them with translated names. The four abstract translation methods cover every direction that
 * {@link org.smallmind.quorum.namespace.JavaContext} requires:
 * <ul>
 *   <li>internal {@link Name} → external {@link JavaName} (for lookups)</li>
 *   <li>external {@link JavaName} → external string (for display and storage)</li>
 *   <li>absolute external string → internal string (when re-importing a fully qualified name)</li>
 *   <li>relative external string → internal string (when re-importing a partial name)</li>
 * </ul>
 */
public abstract class NameTranslator {

  private final ContextCreator contextCreator;

  /**
   * Creates a translator backed by the given context creator.
   *
   * @param contextCreator the factory used to obtain initial directory contexts
   */
  public NameTranslator (ContextCreator contextCreator) {

    this.contextCreator = contextCreator;
  }

  /**
   * Returns the context creator held by this translator.
   *
   * @return the {@link ContextCreator} supplied at construction time; never {@code null}
   */
  public ContextCreator getContextCreator () {

    return contextCreator;
  }

  /**
   * Resolves an internal name against an optional internal context and returns the corresponding
   * external context paired with the translated external name.
   * <p>
   * When {@code internalContext} is {@code null}, the name must start with {@code java:}; the
   * method strips that prefix and opens a new initial context via the embedded
   * {@link ContextCreator}. When {@code internalContext} is non-null, it is used directly and the
   * name is translated without removing any prefix.
   *
   * @param internalContext an already-open directory context to use as the resolution root,
   *                        or {@code null} to open a new initial context
   * @param internalName    the internal name to translate, which must begin with {@code java:}
   *                        when {@code internalContext} is {@code null}
   * @return a {@link ContextNamePair} holding the external context and translated name
   * @throws NamingException if the name does not start with {@code java:} when required, or if
   *                         opening the initial context or translating the name fails
   */
  public ContextNamePair fromInternalNameToExternalContext (DirContext internalContext, Name internalName)
    throws NamingException {

    if (internalContext == null) {
      if ((internalName.size() == 0) || (!internalName.get(0).equals("java:"))) {
        throw new NamingException("No starting context from which to resolve (" + internalName + ")");
      }

      try {
        return new ContextNamePair(contextCreator.getInitialContext(), fromInternalNameToExternalName(internalName.getSuffix(1)));
      } catch (NamingException namingException) {
        throw namingException;
      } catch (Exception e) {

        NamingException namingException;

        namingException = new NamingException(e.getMessage());
        namingException.setRootCause(e);

        throw namingException;
      }
    } else {
      return new ContextNamePair(internalContext, fromInternalNameToExternalName(internalName));
    }
  }

  /**
   * Converts an internal {@link Name} into a backing-store-specific {@link JavaName}.
   *
   * @param internalName the internal name whose components are to be translated
   * @return the equivalent external name in the backing store's naming convention
   * @throws InvalidNameException if any component of the internal name cannot be mapped
   */
  public abstract JavaName fromInternalNameToExternalName (Name internalName)
    throws InvalidNameException;

  /**
   * Renders a backing-store-specific {@link JavaName} as a string in the external naming format.
   * <p>
   * For LDAP, this produces a comma-separated distinguished name in reverse component order.
   *
   * @param internalName the external name to render
   * @return the string form appropriate for the backing store
   */
  public abstract String fromExternalNameToExternalString (JavaName internalName);

  /**
   * Converts a fully qualified external string (i.e., one that includes the backing store root)
   * into the equivalent internal path string, stripping the root prefix and reversing any
   * store-specific component encoding.
   *
   * @param externalName the absolute external name string to convert
   * @return the internal path string, beginning with {@code java:} for absolute names
   * @throws InvalidNameException if the string does not conform to the expected external format
   *                              or does not contain the configured root
   */
  public abstract String fromAbsoluteExternalStringToInternalString (String externalName)
    throws InvalidNameException;

  /**
   * Converts a relative external string (one that does not include the backing store root)
   * into the equivalent internal path string.
   *
   * @param externalName the relative external name string to convert
   * @return the internal path string
   * @throws InvalidNameException if the string does not conform to the expected external format
   */
  public abstract String fromExternalStringToInternalString (String externalName)
    throws InvalidNameException;
}
