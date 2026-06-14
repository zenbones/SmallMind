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
package org.smallmind.nutsnbolts.namespace.shiro.realm.spring;

import java.util.Map;
import org.smallmind.nutsnbolts.namespace.shiro.realm.ActiveDirectoryLdapRealm;
import org.smallmind.nutsnbolts.namespace.shiro.realm.LdapConnectionDetails;
import org.smallmind.nutsnbolts.namespace.shiro.realm.RoleType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs an {@link ActiveDirectoryLdapRealm} for use as a Shiro realm. Every
 * collaborator the realm needs is set as a property and passed to its constructor in {@link #afterPropertiesSet()}: the
 * connection details, the user-entry search path, and the {@code domain} that supplies the user-principal-name suffix for
 * the verification bind. The optional {@code groupRoleMap} maps the {@code memberOf} group distinguished names to
 * {@link RoleType roles}; when it is left unset the realm grants no roles. The realm is built once and returned as a
 * singleton on every {@link #getObject()} call.
 */
public class ActiveDirectoryLdapRealmFactoryBean implements FactoryBean<ActiveDirectoryLdapRealm>, InitializingBean {

  private ActiveDirectoryLdapRealm realm;
  private LdapConnectionDetails connectionDetails;
  private Map<String, RoleType> groupRoleMap;
  private String searchPath;
  private String domain;

  /**
   * Sets the directory coordinates and bind credentials passed to the constructed realm.
   *
   * @param connectionDetails host, port, root namespace, and service-account credentials for the directory
   */
  public void setConnectionDetails (LdapConnectionDetails connectionDetails) {

    this.connectionDetails = connectionDetails;
  }

  /**
   * Sets the user-entry search base passed to the constructed realm.
   *
   * @param searchPath distinguished name of the subtree under which user entries are located
   */
  public void setSearchPath (String searchPath) {

    this.searchPath = searchPath;
  }

  /**
   * Sets the Active Directory domain used as the user-principal-name suffix for the verification bind.
   *
   * @param domain the part after the {@code @} in {@code <principal>@<domain>}
   */
  public void setDomain (String domain) {

    this.domain = domain;
  }

  /**
   * Sets the mapping from group distinguished name to granted {@link RoleType}. Optional; without it the realm grants no
   * roles.
   *
   * @param groupRoleMap group-DN to role mapping
   */
  public void setGroupRoleMap (Map<String, RoleType> groupRoleMap) {

    this.groupRoleMap = groupRoleMap;
  }

  /**
   * Returns the realm built during {@link #afterPropertiesSet()}.
   *
   * @return the configured {@link ActiveDirectoryLdapRealm} singleton
   * @throws Exception never thrown directly; declared by the {@link FactoryBean} contract
   */
  @Override
  public ActiveDirectoryLdapRealm getObject ()
    throws Exception {

    return realm;
  }

  /**
   * Reports the realm type produced by this factory.
   *
   * @return {@link ActiveDirectoryLdapRealm}
   */
  @Override
  public Class<?> getObjectType () {

    return ActiveDirectoryLdapRealm.class;
  }

  /**
   * Reports that this factory produces a singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Constructs the realm from the configured connection details, search path, and domain, then applies the group-to-role
   * map.
   *
   * @throws Exception if construction fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    realm = new ActiveDirectoryLdapRealm(connectionDetails, searchPath, domain);
    realm.setGroupRoleMap(groupRoleMap);
  }
}
