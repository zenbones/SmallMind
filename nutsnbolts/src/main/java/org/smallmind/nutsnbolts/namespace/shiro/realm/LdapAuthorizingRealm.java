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
package org.smallmind.nutsnbolts.namespace.shiro.realm;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

/**
 * Base class for Shiro {@link AuthorizingRealm} implementations that authenticate against an LDAP or Active Directory
 * directory. Concrete subclasses ({@link DefaultLdapRealm}, {@link ActiveDirectoryLdapRealm}) supply the directory-specific
 * bind and search behavior — including how a subject's directory groups are read, via {@link #getDirectoryGroups(Object)} —
 * while this type owns the collaborators every implementation needs: the connection coordinates and the subtree under which
 * user entries are searched, both supplied as constructor arguments, plus the mapping from directory groups to
 * {@link RoleType roles}. Realms are typically assembled through one of the Spring factory beans
 * ({@link org.smallmind.nutsnbolts.namespace.shiro.realm.spring.DefaultLdapRealmFactoryBean},
 * {@link org.smallmind.nutsnbolts.namespace.shiro.realm.spring.ActiveDirectoryLdapRealmFactoryBean}) but may be constructed
 * directly.
 *
 * <p>Authorization is resolved here: {@link #doGetAuthorizationInfo(PrincipalCollection)} reads the groups the subject
 * belongs to and grants the role each group is mapped to. A subject whose groups are not present in the map receives no
 * roles, so an unconfigured (empty) {@code groupRoleMap} grants nothing.
 */
public abstract class LdapAuthorizingRealm extends AuthorizingRealm {

  private final HashMap<String, RoleType> groupRoleMap = new HashMap<>();
  private final LdapConnectionDetails connectionDetails;
  private final String searchPath;

  /**
   * Creates a realm bound to the supplied directory coordinates and user-entry search base.
   *
   * @param connectionDetails host, port, root namespace, and service-account credentials for the directory
   * @param searchPath        distinguished name of the subtree under which user entries are located, resolved against the
   *                          configured root namespace
   */
  public LdapAuthorizingRealm (LdapConnectionDetails connectionDetails, String searchPath) {

    this.connectionDetails = connectionDetails;
    this.searchPath = searchPath;
  }

  /**
   * Replaces the mapping from directory group identifier to granted {@link RoleType}. The key form must match exactly what
   * the concrete realm's {@link #getDirectoryGroups(Object)} returns (for example, full group distinguished names for an
   * Active Directory {@code memberOf} attribute). A {@code null} map clears the mapping, in which case no roles are
   * granted.
   *
   * @param groupRoleMap directory-group-identifier to role mapping, copied defensively; may be {@code null} to clear
   */
  public void setGroupRoleMap (Map<String, RoleType> groupRoleMap) {

    this.groupRoleMap.clear();

    if (groupRoleMap != null) {
      this.groupRoleMap.putAll(groupRoleMap);
    }
  }

  /**
   * Returns the directory coordinates and service-account credentials supplied at construction.
   *
   * @return the connection details
   */
  protected LdapConnectionDetails getConnectionDetails () {

    return connectionDetails;
  }

  /**
   * Returns the user-entry search base supplied at construction.
   *
   * @return the search path
   */
  protected String getSearchPath () {

    return searchPath;
  }

  /**
   * Opens a simple-authentication {@link DirContext} bound as the supplied user against the configured server and root
   * namespace. Subclasses use this both for the service-account bind that performs searches and, where applicable, for a
   * credential-verifying bind as the user.
   *
   * @param user     the bind principal (a service-account distinguished name or, for verification, a user principal name)
   * @param password the bind password
   * @return a directory context bound as the supplied user
   * @throws NamingException if the directory rejects the bind or cannot be reached
   */
  protected DirContext getLdapContext (String user, String password)
    throws NamingException {

    Hashtable<String, String> env;

    env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap://" + connectionDetails.getHost() + ":" + connectionDetails.getPort() + "/" + connectionDetails.getRootNamespace());
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, user);
    env.put(Context.SECURITY_CREDENTIALS, password);

    return new InitialDirContext(env);
  }

  /**
   * Returns the directory group identifiers the supplied principal belongs to, in whatever string form the concrete realm
   * reads from its directory (for example, the group distinguished names from an Active Directory {@code memberOf}
   * attribute). These identifiers are matched against the keys configured through {@link #setGroupRoleMap(Map)}.
   *
   * @param principal the primary principal of the authenticated subject
   * @return the subject's group identifiers; never {@code null}, empty when the subject is in no recognized group
   */
  protected abstract Collection<String> getDirectoryGroups (Object principal);

  /**
   * Grants each subject the roles its directory groups map to. Groups absent from the configured {@code groupRoleMap}
   * contribute no roles.
   *
   * @param principals the authenticated principals; the primary principal identifies the subject whose groups are read
   * @return authorization info containing the role codes mapped from the subject's directory groups
   */
  @Override
  protected AuthorizationInfo doGetAuthorizationInfo (PrincipalCollection principals) {

    HashSet<String> roleCodeSet = new HashSet<>();

    for (String group : getDirectoryGroups(principals.getPrimaryPrincipal())) {

      RoleType roleType;

      if ((roleType = groupRoleMap.get(group)) != null) {
        roleCodeSet.add(roleType.getCode());
      }
    }

    return new SimpleAuthorizationInfo(roleCodeSet);
  }
}
