/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.cloud.namespace.java.backingStore;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.smallmind.cloud.namespace.java.ContextNamePair;
import org.smallmind.cloud.namespace.java.JavaName;

public abstract class NameTranslator {

  private ContextCreator contextCreator;

  public NameTranslator (ContextCreator contextCreator) {

    this.contextCreator = contextCreator;
  }

  public ContextCreator getContextCreator () {

    return contextCreator;
  }

  public ContextNamePair fromInternalNameToExternalContext (DirContext internalContext, Name internalName)
    throws NamingException {

    if (internalContext == null) {
      if ((internalName.size() == 0) || (!internalName.get(0).equals("java:"))) {
        throw new NamingException("No starting context from which to resolve (" + internalName + ")");
      }

      try {
        return new ContextNamePair(contextCreator.getInitialContext(), fromInternalNameToExternalName(internalName.getSuffix(1)));
      }
      catch (NamingException namingException) {
        throw namingException;
      }
      catch (Exception e) {

        NamingException namingException;

        namingException = new NamingException(e.getMessage());
        namingException.setRootCause(e);

        throw namingException;
      }
    }
    else {
      return new ContextNamePair(internalContext, fromInternalNameToExternalName(internalName));
    }
  }

  public abstract JavaName fromInternalNameToExternalName (Name internalName)
    throws InvalidNameException;

  public abstract String fromExternalNameToExternalString (JavaName internalName);

  public abstract String fromAbsoluteExternalStringToInternalString (String externalName)
    throws InvalidNameException;

  public abstract String fromExternalStringToInternalString (String externalName)
    throws InvalidNameException;

}
