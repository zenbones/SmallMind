package org.smallmind.nutsnbolts.security.spring;

import org.apache.shiro.realm.AuthorizingRealm;
import org.smallmind.nutsnbolts.security.LdapAuthorizingRealm;
import org.smallmind.nutsnbolts.security.LdapConnectionDetails;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class LdapRealmFactoryBean implements FactoryBean<LdapAuthorizingRealm>, InitializingBean {

  private LdapAuthorizingRealm realm;
  private Class<? extends LdapAuthorizingRealm> realmClass;
  private LdapConnectionDetails connectionDetails;
  private String searchPath;

  public void setRealmClass (Class<? extends LdapAuthorizingRealm> realmClass) {

    this.realmClass = realmClass;
  }

  public void setConnectionDetails (LdapConnectionDetails connectionDetails) {

    this.connectionDetails = connectionDetails;
  }

  public void setSearchPath (String searchPath) {

    this.searchPath = searchPath;
  }

  @Override
  public LdapAuthorizingRealm getObject () throws Exception {

    return realm;
  }

  @Override
  public Class<?> getObjectType () {

    return AuthorizingRealm.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public void afterPropertiesSet ()
    throws Exception {

    realm = realmClass.newInstance();
    realm.setConnectionDetails(connectionDetails);
    realm.setSearchPath(searchPath);
  }
}
