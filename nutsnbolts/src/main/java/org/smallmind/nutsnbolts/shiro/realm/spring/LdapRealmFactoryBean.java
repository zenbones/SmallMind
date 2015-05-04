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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.shiro.realm.spring;

import org.apache.shiro.realm.AuthorizingRealm;
import org.smallmind.nutsnbolts.shiro.realm.LdapAuthorizingRealm;
import org.smallmind.nutsnbolts.shiro.realm.LdapConnectionDetails;
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
