/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.quorum.namespace.java.backingStore.ldap;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.smallmind.quorum.namespace.java.backingStore.ContextCreator;
import org.smallmind.quorum.namespace.java.backingStore.NamingConnectionDetails;

public class LdapContextCreator extends ContextCreator {

  public static void insureContext (DirContext dirContext, String namingPath)
    throws NamingException {

    StringBuilder pathSoFar;
    String[] pathArray;

    pathArray = namingPath.split(",", -1);
    pathSoFar = new StringBuilder();
    for (int count = pathArray.length - 1; count >= 0; count--) {
      if (pathSoFar.length() > 0) {
        pathSoFar.insert(0, ',');
      }
      pathSoFar.insert(0, pathArray[count]);
      try {
        dirContext.lookup(pathSoFar.toString());
      } catch (NameNotFoundException n) {
        dirContext.createSubcontext(pathSoFar.toString());
      }
    }
  }

  public LdapContextCreator (NamingConnectionDetails connectionDetails) {

    super(connectionDetails);
  }

  public String getRoot () {

    return getConnectionDetails().getRootNamespace();
  }

  public DirContext getInitialContext ()
    throws NamingException {

    Hashtable<String, String> env;

    env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap" + (getConnectionDetails().useTLS() ? "s" : "") + "://" + getConnectionDetails().getHost() + ":" + getConnectionDetails().getPort() + "/" + getConnectionDetails().getRootNamespace());
    if (getConnectionDetails().useTLS()) {
      env.put(Context.SECURITY_PROTOCOL, "ssl");
    }
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, getConnectionDetails().getUserName());
    env.put(Context.SECURITY_CREDENTIALS, getConnectionDetails().getPassword());

    return new InitialDirContext(env);
  }
}
