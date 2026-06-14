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

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.lang.util.ByteSource;
import org.apache.shiro.lang.util.SimpleByteSource;

/**
 * An {@link LdapAuthorizingRealm} for Microsoft Active Directory. Rather than reading and comparing a stored password
 * attribute, this realm verifies credentials by attempting a fresh directory bind as the user. It first binds with the
 * configured service account and runs a subtree search for {@code (&(objectClass=user)(sAMAccountName=<principal>))} under
 * the search path; when a matching entry is found, it performs a second bind using the user principal name
 * {@code <principal>@<domain>} and the presented credentials. A successful second bind authenticates the user; the realm
 * then returns a salted SHA-256 digest of the credentials (the salt is a random {@link UUID}, so the value is fresh on
 * every call). A search that returns no entry yields {@code null}, and any directory {@link NamingException} — including
 * the bind failure raised when the user's credentials are wrong — is wrapped in an {@link AuthenticationException}.
 *
 * <p>Authorization reads the user's {@code memberOf} group distinguished names (see {@link #getDirectoryGroups(Object)})
 * and grants the roles those groups are mapped to through the inherited group-to-{@link RoleType role} map; a user whose
 * groups are not mapped receives no roles.
 *
 * <p>All collaborators are required constructor arguments — the connection details and search path through the superclass,
 * and the {@code domain} that supplies the user-principal-name suffix for the verification bind (a principal of
 * {@code jdoe} binds as {@code jdoe@<domain>}). This realm therefore has no public no-argument constructor.
 */
public class ActiveDirectoryLdapRealm extends LdapAuthorizingRealm {

  private static final CredentialsMatcher CREDENTIALS_MATCHER = new HashedCredentialsMatcher(Sha256Hash.ALGORITHM_NAME);
  private static final String[] RETURNED_ATTRIBUTES = {"sn", "givenName", "mail"};
  private static final String[] MEMBER_OF_ATTRIBUTES = {"memberOf"};
  private final String domain;

  /**
   * Creates a realm that verifies credentials against the given Active Directory domain.
   *
   * @param connectionDetails host, port, root namespace, and service-account credentials for the directory
   * @param searchPath        distinguished name of the subtree under which user entries are located
   * @param domain            the user-principal-name suffix appended to the principal for the verification bind (the part
   *                          after the {@code @} in {@code <principal>@<domain>})
   */
  public ActiveDirectoryLdapRealm (LdapConnectionDetails connectionDetails, String searchPath, String domain) {

    super(connectionDetails, searchPath);

    this.domain = domain;
  }

  /**
   * Returns the SHA-256 {@link HashedCredentialsMatcher} that compares the salted digest produced on a successful bind
   * against the digest of the presented credentials.
   *
   * @return the shared SHA-256 credentials matcher
   */
  @Override
  public CredentialsMatcher getCredentialsMatcher () {

    return CREDENTIALS_MATCHER;
  }

  /**
   * Binds with the service account, locates the user by {@code sAMAccountName}, and returns the distinguished names listed
   * on the entry's {@code memberOf} attribute. These DNs are the keys the inherited authorization logic matches against the
   * configured group-to-role map.
   *
   * @param principal the primary principal of the authenticated subject (the {@code sAMAccountName})
   * @return the group distinguished names from the user's {@code memberOf} attribute; empty when the user is not found or
   *         belongs to no groups
   * @throws AuthorizationException if the directory search fails with a {@link NamingException}
   */
  @Override
  protected Collection<String> getDirectoryGroups (Object principal) {

    ArrayList<String> groupList = new ArrayList<>();

    try {

      SearchControls searchControls;
      NamingEnumeration answer;
      String searchFilter;

      searchFilter = "(&(objectClass=user)(sAMAccountName=" + principal + "))";

      searchControls = new SearchControls();
      searchControls.setReturningAttributes(MEMBER_OF_ATTRIBUTES);
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      searchControls.setCountLimit(1);

      answer = getLdapContext(getConnectionDetails().getUserName(), getConnectionDetails().getPassword()).search(getSearchPath(), searchFilter, searchControls);
      if (answer.hasMoreElements()) {

        Attributes attributes;
        Attribute memberOfAttribute;

        if (((attributes = ((SearchResult)answer.next()).getAttributes()) != null) && ((memberOfAttribute = attributes.get("memberOf")) != null)) {

          NamingEnumeration memberOfValues;

          memberOfValues = memberOfAttribute.getAll();
          while (memberOfValues.hasMoreElements()) {
            groupList.add(memberOfValues.next().toString());
          }
        }
      }
    } catch (NamingException namingException) {
      throw new AuthorizationException(namingException);
    }

    return groupList;
  }

  /**
   * Searches for the user by {@code sAMAccountName} using the service account, then verifies the presented credentials by
   * rebinding to the directory as {@code <principal>@<domain>}.
   *
   * @param token the submitted authentication token; its principal is the {@code sAMAccountName} and its credentials are
   *              the cleartext password as a {@code char[]}
   * @return authentication info carrying a freshly salted SHA-256 digest of the credentials when the verification bind
   *         succeeds, or {@code null} when no matching directory entry is found
   * @throws AuthenticationException if the directory search or the verification bind fails with a {@link NamingException}
   */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo (AuthenticationToken token)
    throws AuthenticationException {

    try {

      SearchControls searchControls;
      NamingEnumeration<?> answer;
      String searchFilter;

      searchFilter = "(&(objectClass=user)(sAMAccountName=" + token.getPrincipal() + "))";

      searchControls = new SearchControls();
      searchControls.setReturningAttributes(RETURNED_ATTRIBUTES);
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      searchControls.setCountLimit(1);

      answer = getLdapContext(getConnectionDetails().getUserName(), getConnectionDetails().getPassword()).search(getSearchPath(), searchFilter, searchControls);
      if (answer.hasMoreElements()) {
        if (((SearchResult)answer.next()).getAttributes() != null) {
          getLdapContext(token.getPrincipal().toString() + "@" + domain, new String((char[])token.getCredentials()));

          Hash sha256Hash;
          ByteSource salt;

          sha256Hash = new Sha256Hash(new String((char[])token.getCredentials()), salt = new SimpleByteSource(UUID.randomUUID().toString()));

          return new SimpleAuthenticationInfo(token.getPrincipal(), sha256Hash.getBytes(), salt, getName());
        }
      }
    } catch (NamingException namingException) {
      throw new AuthenticationException(namingException);
    }

    return null;
  }
}
