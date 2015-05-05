/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.nutsnbolts.shiro.realm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.Sha1Hash;
import org.apache.shiro.subject.PrincipalCollection;

public class DefaultLdapRealm extends LdapAuthorizingRealm {

  private static final CredentialsMatcher CREDENTIALS_MATCHER = new HashedCredentialsMatcher(Sha1Hash.ALGORITHM_NAME);
  private static HashSet<String> ROLE_SET;
  private LdapConnectionDetails connectionDetails;
  private String searchPath;

  static {

    ROLE_SET = new HashSet<String>();
    ROLE_SET.add(RoleType.ADMIN.getCode());
  }

  public void setConnectionDetails (LdapConnectionDetails connectionDetails) {

    this.connectionDetails = connectionDetails;
  }

  public void setSearchPath (String searchPath) {

    this.searchPath = searchPath;
  }

  @Override
  public CredentialsMatcher getCredentialsMatcher () {

    return CREDENTIALS_MATCHER;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo (PrincipalCollection principals) {

    return new SimpleAuthorizationInfo(Collections.unmodifiableSet(ROLE_SET));
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo (AuthenticationToken token)
    throws AuthenticationException {

    Hashtable<String, String> env;

    env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap://" + connectionDetails.getHost() + ":" + connectionDetails.getPort() + "/" + connectionDetails.getRootNamespace());
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, connectionDetails.getUserName());
    env.put(Context.SECURITY_CREDENTIALS, connectionDetails.getPassword());

    try {

      Attributes userAttributes;

      if ((userAttributes = ((DirContext)new InitialDirContext(env).lookup(searchPath)).getAttributes("uid=" + token.getPrincipal().toString())) != null) {

        Attribute passwordAttribute;

        if ((passwordAttribute = userAttributes.get("userPassword")) != null) {

          String hashedPasswordPlusAlgorithm;
          Hash sha1Hash;

          hashedPasswordPlusAlgorithm = new String((byte[])passwordAttribute.get());
          sha1Hash = new Sha1Hash(new String((char[])token.getCredentials()));
          if (hashedPasswordPlusAlgorithm.equals("{SHA}" + sha1Hash.toBase64())) {

            return new SimpleAuthenticationInfo(token.getPrincipal(), sha1Hash.getBytes(), getName());
          }
        }
      }
    }
    catch (NamingException namingException) {
      throw new AuthenticationException(namingException);
    }

    return null;
  }
}
