package org.smallmind.nutsnbolts.security;

import org.apache.shiro.realm.AuthorizingRealm;

public abstract class LdapAuthorizingRealm extends AuthorizingRealm {

  public abstract void setConnectionDetails (LdapConnectionDetails connectionDetails);

  public abstract void setSearchPath (String searchPath);
}
