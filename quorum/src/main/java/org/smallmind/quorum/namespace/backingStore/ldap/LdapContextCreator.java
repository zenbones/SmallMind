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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.smallmind.quorum.namespace.backingStore.ContextCreator;
import org.smallmind.quorum.namespace.backingStore.NamingConnectionDetails;

/**
 * {@link ContextCreator} implementation that opens LDAP directory contexts using the Sun LDAP
 * provider, and provides a utility for ensuring that all intermediate DN nodes exist.
 * <p>
 * The provider URL is constructed from the {@link NamingConnectionDetails} as
 * {@code ldap[s]://host:port/rootNamespace}. When TLS is requested the URL scheme is switched to
 * {@code ldaps} and the {@link Context#SECURITY_PROTOCOL} environment property is set to
 * {@code ssl}. Authentication uses {@code simple} security and the configured credentials.
 */
public class LdapContextCreator extends ContextCreator {

  /**
   * Creates a creator that will open LDAP contexts using the supplied connection details.
   *
   * @param connectionDetails LDAP server address, port, TLS flag, root DN, and credentials
   */
  public LdapContextCreator (NamingConnectionDetails connectionDetails) {

    super(connectionDetails);
  }

  /**
   * Ensures that every component of {@code namingPath} exists as a subcontext under
   * {@code dirContext}, creating any missing intermediate nodes from the root inward.
   * <p>
   * The path is split on commas; components are iterated from the last (most general) to the
   * first (most specific), accumulating a candidate DN and calling {@link DirContext#lookup}
   * on it. A {@link NameNotFoundException} triggers a {@link DirContext#createSubcontext} call
   * for that node.
   *
   * @param dirContext the directory context relative to which the path is resolved
   * @param namingPath a comma-separated LDAP DN whose nodes should all exist
   * @throws NamingException if any lookup or subcontext creation fails for a reason other than
   *                         the node not yet existing
   */
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

  /**
   * Returns the LDAP root DN configured in the connection details.
   * <p>
   * This is the base DN appended to the provider URL and used as the starting point for all
   * relative name resolution.
   *
   * @return the root distinguished name; never {@code null}
   */
  public String getRoot () {

    return getConnectionDetails().getRootNamespace();
  }

  /**
   * Opens and returns a new {@link InitialDirContext} connected to the configured LDAP server.
   * <p>
   * The environment is built from the {@link NamingConnectionDetails}: TLS is enabled by using
   * the {@code ldaps} scheme and setting {@link Context#SECURITY_PROTOCOL} to {@code ssl}.
   * Authentication is always {@code simple} using the configured principal and credential.
   *
   * @return a new {@link DirContext} bound to the LDAP server at the configured root DN
   * @throws NamingException if the server is unreachable, the credentials are rejected, or the
   *                         JNDI environment is misconfigured
   */
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
