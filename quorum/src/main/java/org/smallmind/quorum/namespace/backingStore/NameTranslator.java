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
 * Translates names between external representations and internal directory-specific formats.
 * Implementations are specific to the backing store type.
 */
public abstract class NameTranslator {

  private final ContextCreator contextCreator;

  /**
   * Creates a translator using the provided context creator.
   *
   * @param contextCreator factory for backing contexts
   */
  public NameTranslator (ContextCreator contextCreator) {

    this.contextCreator = contextCreator;
  }

  /**
   * Returns the backing {@link ContextCreator}.
   *
   * @return context creator
   */
  public ContextCreator getContextCreator () {

    return contextCreator;
  }

  /**
   * Converts an internal name into a pair of external context and name.
   *
   * @param internalContext current internal context (may be {@code null} for root)
   * @param internalName    internal name to translate
   * @return external context/name pair
   * @throws NamingException if translation fails or no base context exists
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
   * Converts an internal name to an external {@link JavaName}.
   *
   * @param internalName internal representation
   * @return external name
   * @throws InvalidNameException if conversion fails
   */
  public abstract JavaName fromInternalNameToExternalName (Name internalName)
    throws InvalidNameException;

  /**
   * Renders an external name to a string form.
   *
   * @param internalName external name
   * @return external string form
   */
  public abstract String fromExternalNameToExternalString (JavaName internalName);

  /**
   * Converts an absolute external string into an internal string form.
   *
   * @param externalName absolute external name
   * @return internal string form
   * @throws InvalidNameException if conversion fails
   */
  public abstract String fromAbsoluteExternalStringToInternalString (String externalName)
    throws InvalidNameException;

  /**
   * Converts an external string into an internal string form.
   *
   * @param externalName external name
   * @return internal string form
   * @throws InvalidNameException if conversion fails
   */
  public abstract String fromExternalStringToInternalString (String externalName)
    throws InvalidNameException;
}
