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

import java.util.Collections;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DefaultLdapRealmTest {

  private InMemoryDirectoryServer directoryServer;
  private DefaultLdapRealm realm;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    InMemoryDirectoryServerConfig config;
    LdapConnectionDetails connectionDetails;
    String hashedPassword;

    config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
    config.addAdditionalBindCredentials("cn=Directory Manager", "managerpw");
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("test", 0));

    directoryServer = new InMemoryDirectoryServer(config);
    directoryServer.startListening();

    hashedPassword = "{SHA}" + new Sha256Hash("secret").toBase64();

    directoryServer.add("dn: dc=example,dc=com", "objectClass: top", "objectClass: domain", "dc: example");
    directoryServer.add("dn: ou=people,dc=example,dc=com", "objectClass: top", "objectClass: organizationalUnit", "ou: people");
    directoryServer.add("dn: ou=groups,dc=example,dc=com", "objectClass: top", "objectClass: organizationalUnit", "ou: groups");
    directoryServer.add("dn: uid=jdoe,ou=people,dc=example,dc=com", "objectClass: top", "objectClass: inetOrgPerson", "uid: jdoe", "cn: John Doe", "sn: Doe", "userPassword: " + hashedPassword);
    directoryServer.add("dn: uid=nopass,ou=people,dc=example,dc=com", "objectClass: top", "objectClass: inetOrgPerson", "uid: nopass", "cn: No Pass", "sn: Pass");
    directoryServer.add("dn: cn=admins,ou=groups,dc=example,dc=com", "objectClass: top", "objectClass: groupOfNames", "cn: admins", "member: uid=jdoe,ou=people,dc=example,dc=com");

    connectionDetails = new LdapConnectionDetails();
    connectionDetails.setHost("localhost");
    connectionDetails.setPort(directoryServer.getListenPort());
    connectionDetails.setRootNamespace("dc=example,dc=com");
    connectionDetails.setUserName("cn=Directory Manager");
    connectionDetails.setPassword("managerpw");

    realm = new DefaultLdapRealm(connectionDetails, "ou=people", "ou=groups", "groupOfNames", "member", "uid={0},ou=people,dc=example,dc=com");
  }

  @AfterClass(alwaysRun = true)
  public void afterClass () {

    if (directoryServer != null) {
      directoryServer.shutDown(true);
    }
  }

  public void testAuthenticationSucceedsWithCorrectPassword () {

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(new UsernamePasswordToken("jdoe", "secret"));

    Assert.assertNotNull(authenticationInfo);
    Assert.assertEquals(authenticationInfo.getPrincipals().getPrimaryPrincipal(), "jdoe");
  }

  public void testAuthenticationFailsWithWrongPassword () {

    Assert.assertNull(realm.doGetAuthenticationInfo(new UsernamePasswordToken("jdoe", "wrong")));
  }

  public void testAuthenticationReturnsNullWhenEntryHasNoPassword () {

    Assert.assertNull(realm.doGetAuthenticationInfo(new UsernamePasswordToken("nopass", "secret")));
  }

  @Test(expectedExceptions = AuthenticationException.class)
  public void testUnknownUserRaisesAuthenticationException () {

    realm.doGetAuthenticationInfo(new UsernamePasswordToken("nobody", "secret"));
  }

  public void testGetDirectoryGroupsReturnsMemberGroupDns () {

    Assert.assertTrue(realm.getDirectoryGroups("jdoe").contains("cn=admins,ou=groups,dc=example,dc=com"));
  }

  public void testGetDirectoryGroupsEmptyForUngroupedUser () {

    Assert.assertTrue(realm.getDirectoryGroups("nopass").isEmpty());
  }

  public void testAuthorizationGrantsRoleMappedFromGroup () {

    realm.setGroupRoleMap(Collections.singletonMap("cn=admins,ou=groups,dc=example,dc=com", RoleType.ADMIN));

    Assert.assertTrue(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("jdoe", "test")).getRoles().contains(RoleType.ADMIN.getCode()));
  }
}
