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
import org.smallmind.nutsnbolts.namespace.shiro.realm.DefaultLdapRealm;
import org.smallmind.nutsnbolts.namespace.shiro.realm.LdapConnectionDetails;
import org.smallmind.nutsnbolts.namespace.shiro.realm.RoleType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs a {@link DefaultLdapRealm} for use as a Shiro realm. Every collaborator the
 * realm needs is set as a property and passed to its constructor in {@link #afterPropertiesSet()}: the connection details
 * and user-entry search path, and the four group-schema arguments ({@code groupSearchPath}, {@code groupObjectClass},
 * {@code memberAttribute}, {@code memberValueTemplate}) that describe how group membership is read. The optional
 * {@code groupRoleMap} maps directory group distinguished names to {@link RoleType roles}; when it is left unset the realm
 * grants no roles. The realm is built once and returned as a singleton on every {@link #getObject()} call.
 */
public class DefaultLdapRealmFactoryBean implements FactoryBean<DefaultLdapRealm>, InitializingBean {

  private DefaultLdapRealm realm;
  private LdapConnectionDetails connectionDetails;
  private Map<String, RoleType> groupRoleMap;
  private String searchPath;
  private String groupSearchPath;
  private String groupObjectClass;
  private String memberAttribute;
  private String memberValueTemplate;

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
   * Sets the subtree searched for group entries.
   *
   * @param groupSearchPath distinguished name of the subtree searched for group entries
   */
  public void setGroupSearchPath (String groupSearchPath) {

    this.groupSearchPath = groupSearchPath;
  }

  /**
   * Sets the object class of group entries used in the membership filter.
   *
   * @param groupObjectClass object class of group entries (for example {@code groupOfNames} or {@code posixGroup})
   */
  public void setGroupObjectClass (String groupObjectClass) {

    this.groupObjectClass = groupObjectClass;
  }

  /**
   * Sets the attribute on a group entry that lists members.
   *
   * @param memberAttribute member attribute (for example {@code member}, {@code uniqueMember}, or {@code memberUid})
   */
  public void setMemberAttribute (String memberAttribute) {

    this.memberAttribute = memberAttribute;
  }

  /**
   * Sets the template for the member value to match, with {@code {0}} replaced by the principal.
   *
   * @param memberValueTemplate member-value template (for example {@code "uid={0},ou=people"} or {@code "{0}"})
   */
  public void setMemberValueTemplate (String memberValueTemplate) {

    this.memberValueTemplate = memberValueTemplate;
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
   * @return the configured {@link DefaultLdapRealm} singleton
   * @throws Exception never thrown directly; declared by the {@link FactoryBean} contract
   */
  @Override
  public DefaultLdapRealm getObject ()
    throws Exception {

    return realm;
  }

  /**
   * Reports the realm type produced by this factory.
   *
   * @return {@link DefaultLdapRealm}
   */
  @Override
  public Class<?> getObjectType () {

    return DefaultLdapRealm.class;
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
   * Constructs the realm from the configured connection details, search path, and group schema, then applies the
   * group-to-role map.
   *
   * @throws Exception if construction fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    realm = new DefaultLdapRealm(connectionDetails, searchPath, groupSearchPath, groupObjectClass, memberAttribute, memberValueTemplate);
    realm.setGroupRoleMap(groupRoleMap);
  }
}
