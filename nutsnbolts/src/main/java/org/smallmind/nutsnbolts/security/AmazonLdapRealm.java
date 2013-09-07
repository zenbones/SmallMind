package org.smallmind.nutsnbolts.security;

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

public class AmazonLdapRealm extends LdapAuthorizingRealm {

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
