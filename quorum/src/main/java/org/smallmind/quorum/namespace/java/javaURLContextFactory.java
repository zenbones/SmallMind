/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.namespace.java;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import org.smallmind.nutsnbolts.util.StringUtilities;
import org.smallmind.quorum.namespace.java.backingStore.ContextCreator;
import org.smallmind.quorum.namespace.java.backingStore.NameTranslator;
import org.smallmind.quorum.namespace.java.backingStore.NamingConnectionDetails;

public class javaURLContextFactory implements ObjectFactory {

  private static final Class[] CONTEXT_CREATOR_SIGNATURE = new Class[]{NamingConnectionDetails.class};
  private static final Class[] NAME_TRANSLATOR_SIGNATURE = new Class[]{ContextCreator.class};

  public Object getObjectInstance (Object obj, Name name, Context nameCtx, Hashtable environment)
    throws Exception {

    ContextCreator contextCreator;
    NameTranslator nameTranslator;
    Class contextCreatorClass;
    Class nameTranslatorClass;
    Constructor contextCreatorConstructor;
    Constructor nameTranslatorConstructor;
    String backingStore;
    boolean modifiable = false;
    boolean pooled = false;

    if (obj == null) {
      if (environment.containsKey(JavaContext.CONTEXT_MODIFIABLE)) {
        if ((environment.get(JavaContext.CONTEXT_MODIFIABLE)).equals("true")) {
          modifiable = true;
        }
      }
      if (environment.containsKey(JavaContext.POOLED_CONNECTION)) {
        if ((environment.get(JavaContext.POOLED_CONNECTION)).equals("true")) {
          pooled = true;
        }
      }

      backingStore = (String)environment.get(JavaContext.CONTEXT_STORE);

      contextCreatorClass = Class.forName(ContextCreator.class.getPackage().getName() + '.' + backingStore + '.' + StringUtilities.toDisplayCase(backingStore) + ContextCreator.class.getSimpleName());
      contextCreatorConstructor = contextCreatorClass.getConstructor(CONTEXT_CREATOR_SIGNATURE);
      contextCreator = (ContextCreator)contextCreatorConstructor.newInstance((NamingConnectionDetails)environment.get(JavaContext.CONNECTION_DETAILS));

      nameTranslatorClass = Class.forName(NameTranslator.class.getPackage().getName() + '.' + backingStore + '.' + StringUtilities.toDisplayCase(backingStore) + NameTranslator.class.getSimpleName());
      nameTranslatorConstructor = nameTranslatorClass.getConstructor(NAME_TRANSLATOR_SIGNATURE);
      nameTranslator = (NameTranslator)nameTranslatorConstructor.newInstance(contextCreator);

      return new JavaContext(nameTranslator, (Hashtable<String, Object>)environment, modifiable, pooled);
    }

    return null;
  }
}
