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
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
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

/**
 * An {@link LdapAuthorizingRealm} for a generic LDAP directory that stores a hashed password on the user entry. The realm
 * binds to the directory with the configured service-account credentials, looks up the entry {@code uid=<principal>} under
 * the search path, and reads its {@code userPassword} attribute. Authentication succeeds only when the stored value equals
 * {@code "{SHA}"} followed by the Base64-encoded SHA-256 hash of the presented credentials; the SHA-256 digest is then
 * returned as the stored credential so Shiro's {@link HashedCredentialsMatcher} can complete the match. A failed lookup, a
 * missing {@code userPassword} attribute, or a password mismatch all yield {@code null} (no authentication info), while a
 * directory {@link NamingException} is wrapped in an {@link AuthenticationException}.
 *
 * <p>Authorization (the group-to-{@link RoleType role} mapping inherited from {@link LdapAuthorizingRealm}) reads group
 * membership using the schema supplied at construction. Because generic LDAP has no single group convention,
 * {@link #getDirectoryGroups(Object)} searches the group subtree for entries of the configured object class whose member
 * attribute references the user, and returns each matched group's distinguished name — so the keys in the group-to-role
 * map are full group DNs, uniform with {@link ActiveDirectoryLdapRealm}. All collaborators — the connection details and
 * search path through the superclass, and the four group-schema parameters — are required constructor arguments, so this
 * realm has no public no-argument constructor.
 */
public class DefaultLdapRealm extends LdapAuthorizingRealm {

  private static final CredentialsMatcher CREDENTIALS_MATCHER = new HashedCredentialsMatcher(Sha256Hash.ALGORITHM_NAME);
  private final String groupSearchPath;
  private final String groupObjectClass;
  private final String memberAttribute;
  private final String memberValueTemplate;

  /**
   * Creates a realm that resolves roles from directory groups using the supplied group schema.
   *
   * @param connectionDetails   host, port, root namespace, and service-account credentials for the directory
   * @param searchPath          distinguished name of the subtree under which user entries are located
   * @param groupSearchPath     distinguished name of the subtree searched for group entries (often a different subtree
   *                            than the user search path)
   * @param groupObjectClass    object class of group entries, used in the membership filter (for example
   *                            {@code groupOfNames}, {@code groupOfUniqueNames}, or {@code posixGroup})
   * @param memberAttribute     attribute on a group entry that lists members (for example {@code member},
   *                            {@code uniqueMember}, or {@code memberUid})
   * @param memberValueTemplate template for the member value to match, with {@code {0}} replaced by the principal — for
   *                            example {@code "uid={0},ou=people"} when members are stored as full user DNs, or
   *                            {@code "{0}"} when they are stored as bare user ids (posix {@code memberUid})
   */
  public DefaultLdapRealm (LdapConnectionDetails connectionDetails, String searchPath, String groupSearchPath, String groupObjectClass, String memberAttribute, String memberValueTemplate) {

    super(connectionDetails, searchPath);

    this.groupSearchPath = groupSearchPath;
    this.groupObjectClass = groupObjectClass;
    this.memberAttribute = memberAttribute;
    this.memberValueTemplate = memberValueTemplate;
  }

  /**
   * Returns the SHA-256 {@link HashedCredentialsMatcher} this realm uses to compare the digest returned from the directory
   * against the digest of the presented credentials.
   *
   * @return the shared SHA-256 credentials matcher
   */
  @Override
  public CredentialsMatcher getCredentialsMatcher () {

    return CREDENTIALS_MATCHER;
  }

  /**
   * Binds with the service account and searches the configured group subtree for entries of the configured object class
   * whose member attribute references the supplied principal, returning the distinguished name of each match. These DNs
   * are the keys the inherited authorization logic matches against the configured group-to-role map.
   *
   * @param principal the primary principal of the authenticated subject
   * @return the distinguished names of the groups the principal belongs to; empty when the principal is in no group
   * @throws AuthorizationException if the directory search fails with a {@link NamingException}
   */
  @Override
  protected Collection<String> getDirectoryGroups (Object principal) {

    ArrayList<String> groupList = new ArrayList<>();

    try {

      SearchControls searchControls;
      NamingEnumeration<?> answer;
      String searchFilter;

      searchFilter = "(&(objectClass=" + groupObjectClass + ")(" + memberAttribute + "=" + memberValueTemplate.replace("{0}", principal.toString()) + "))";

      searchControls = new SearchControls();
      searchControls.setReturningAttributes(new String[0]);
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

      answer = getLdapContext(getConnectionDetails().getUserName(), getConnectionDetails().getPassword()).search(groupSearchPath, searchFilter, searchControls);
      while (answer.hasMoreElements()) {
        groupList.add(((SearchResult)answer.next()).getNameInNamespace());
      }
    } catch (NamingException namingException) {
      throw new AuthorizationException(namingException);
    }

    return groupList;
  }

  /**
   * Binds with the service account, looks up {@code uid=<principal>} under the search path, and compares the stored
   * {@code userPassword} (expected as {@code "{SHA}" + Base64(SHA-256(credentials))}) against the presented credentials.
   *
   * @param token the submitted authentication token; its principal is the user id and its credentials are the cleartext
   *              password as a {@code char[]}
   * @return authentication info carrying the SHA-256 digest of the credentials when the stored password matches, or
   *         {@code null} when the user is not found, has no password attribute, or the password does not match
   * @throws AuthenticationException if the directory lookup fails with a {@link NamingException}
   */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo (AuthenticationToken token)
    throws AuthenticationException {

    try {

      Attributes userAttributes;

      if ((userAttributes = ((DirContext)getLdapContext(getConnectionDetails().getUserName(), getConnectionDetails().getPassword()).lookup(getSearchPath())).getAttributes("uid=" + token.getPrincipal().toString())) != null) {

        Attribute passwordAttribute;

        if ((passwordAttribute = userAttributes.get("userPassword")) != null) {

          String hashedPasswordPlusAlgorithm;
          Hash sha256Hash;

          hashedPasswordPlusAlgorithm = new String((byte[])passwordAttribute.get());
          sha256Hash = new Sha256Hash(new String((char[])token.getCredentials()));
          if (hashedPasswordPlusAlgorithm.equals("{SHA}" + sha256Hash.toBase64())) {

            return new SimpleAuthenticationInfo(token.getPrincipal(), sha256Hash.getBytes(), getName());
          }
        }
      }
    } catch (NamingException namingException) {
      throw new AuthenticationException(namingException);
    }

    return null;
  }
}
