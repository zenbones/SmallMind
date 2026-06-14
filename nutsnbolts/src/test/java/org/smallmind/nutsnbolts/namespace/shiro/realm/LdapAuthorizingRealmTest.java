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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LdapAuthorizingRealmTest {

  private static AuthorizationInfo authorize (StubLdapRealm realm) {

    return realm.doGetAuthorizationInfo(new SimplePrincipalCollection("jdoe", "test"));
  }

  public void testMapsGroupToRole () {

    StubLdapRealm realm = new StubLdapRealm(Collections.singletonList("cn=admins"));

    realm.setGroupRoleMap(Collections.singletonMap("cn=admins", RoleType.ADMIN));

    Assert.assertEquals(authorize(realm).getRoles(), Collections.singleton("ADMIN"));
  }

  public void testMapsMultipleGroupsToMultipleRoles () {

    StubLdapRealm realm = new StubLdapRealm(Arrays.asList("cn=admins", "cn=staff"));
    HashMap<String, RoleType> groupRoleMap = new HashMap<>();

    groupRoleMap.put("cn=admins", RoleType.ADMIN);
    groupRoleMap.put("cn=staff", RoleType.USER);
    realm.setGroupRoleMap(groupRoleMap);

    Assert.assertEquals(authorize(realm).getRoles(), new HashSet<>(Arrays.asList("ADMIN", "USER")));
  }

  public void testUnmappedGroupsGrantNoRoles () {

    StubLdapRealm realm = new StubLdapRealm(Collections.singletonList("cn=nobody"));

    realm.setGroupRoleMap(Collections.singletonMap("cn=admins", RoleType.ADMIN));

    Assert.assertTrue(authorize(realm).getRoles().isEmpty());
  }

  public void testEmptyMapGrantsNoRoles () {

    StubLdapRealm realm = new StubLdapRealm(Collections.singletonList("cn=admins"));

    Assert.assertTrue(authorize(realm).getRoles().isEmpty());
  }

  public void testNullMapClearsExistingMapping () {

    StubLdapRealm realm = new StubLdapRealm(Collections.singletonList("cn=admins"));

    realm.setGroupRoleMap(Collections.singletonMap("cn=admins", RoleType.ADMIN));
    realm.setGroupRoleMap(null);

    Assert.assertTrue(authorize(realm).getRoles().isEmpty());
  }

  public void testGroupRoleMapIsCopiedDefensively () {

    StubLdapRealm realm = new StubLdapRealm(Collections.singletonList("cn=staff"));
    HashMap<String, RoleType> groupRoleMap = new HashMap<>();

    groupRoleMap.put("cn=admins", RoleType.ADMIN);
    realm.setGroupRoleMap(groupRoleMap);
    groupRoleMap.put("cn=staff", RoleType.USER);

    Assert.assertTrue(authorize(realm).getRoles().isEmpty());
  }

  public void testDuplicateGroupsCollapseToSingleRole () {

    StubLdapRealm realm = new StubLdapRealm(Arrays.asList("cn=admins", "cn=admins"));

    realm.setGroupRoleMap(Collections.singletonMap("cn=admins", RoleType.ADMIN));

    Assert.assertEquals(authorize(realm).getRoles(), Collections.singleton("ADMIN"));
  }

  private static final class StubLdapRealm extends LdapAuthorizingRealm {

    private final Collection<String> groups;

    private StubLdapRealm (Collection<String> groups) {

      super(null, null);

      this.groups = groups;
    }

    @Override
    protected Collection<String> getDirectoryGroups (Object principal) {

      return groups;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo (AuthenticationToken token)
      throws AuthenticationException {

      return null;
    }
  }
}
