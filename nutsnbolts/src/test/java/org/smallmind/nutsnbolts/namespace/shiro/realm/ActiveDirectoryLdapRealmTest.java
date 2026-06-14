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
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSimpleBindRequest;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ActiveDirectoryLdapRealmTest {

  private static final String ADMINS_GROUP_DN = "cn=Admins,ou=groups,dc=example,dc=com";
  private static final String USER_PRINCIPAL_NAME = "jdoe@example.com";

  private InMemoryDirectoryServer directoryServer;
  private ActiveDirectoryLdapRealm realm;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    InMemoryDirectoryServerConfig config;
    LdapConnectionDetails connectionDetails;

    config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
    // Active Directory attributes (sAMAccountName, memberOf) and the "user" object class are not in the default schema.
    config.setSchema(null);
    config.addAdditionalBindCredentials("cn=Directory Manager", "managerpw");
    // Active Directory accepts user-principal-name binds (jdoe@example.com), which is not a DN the in-memory server
    // can parse — translate that bind into a bind against the real user entry so the server verifies the password,
    // exactly as a real domain controller would resolve the UPN.
    config.addInMemoryOperationInterceptor(new InMemoryOperationInterceptor() {

      @Override
      public void processSimpleBindRequest (InMemoryInterceptedSimpleBindRequest request)
        throws LDAPException {

        if (USER_PRINCIPAL_NAME.equals(request.getRequest().getBindDN())) {
          request.setRequest(new SimpleBindRequest("cn=John Doe,ou=people,dc=example,dc=com", request.getRequest().getPassword().stringValue()));
        }
      }
    });
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("test", 0));

    directoryServer = new InMemoryDirectoryServer(config);
    directoryServer.startListening();

    directoryServer.add("dn: dc=example,dc=com", "objectClass: domain", "dc: example");
    directoryServer.add("dn: ou=people,dc=example,dc=com", "objectClass: organizationalUnit", "ou: people");
    directoryServer.add("dn: cn=John Doe,ou=people,dc=example,dc=com", "objectClass: user", "cn: John Doe", "sAMAccountName: jdoe", "memberOf: " + ADMINS_GROUP_DN, "userPassword: secret");

    connectionDetails = new LdapConnectionDetails();
    connectionDetails.setHost("localhost");
    connectionDetails.setPort(directoryServer.getListenPort());
    connectionDetails.setRootNamespace("dc=example,dc=com");
    connectionDetails.setUserName("cn=Directory Manager");
    connectionDetails.setPassword("managerpw");

    realm = new ActiveDirectoryLdapRealm(connectionDetails, "ou=people", "example.com");
  }

  @AfterClass(alwaysRun = true)
  public void afterClass () {

    if (directoryServer != null) {
      directoryServer.shutDown(true);
    }
  }

  public void testAuthenticationSucceedsWhenVerificationBindSucceeds () {

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(new UsernamePasswordToken("jdoe", "secret"));

    Assert.assertNotNull(authenticationInfo);
    Assert.assertEquals(authenticationInfo.getPrincipals().getPrimaryPrincipal(), "jdoe");
  }

  @Test(expectedExceptions = AuthenticationException.class)
  public void testAuthenticationFailsWhenVerificationBindFails () {

    realm.doGetAuthenticationInfo(new UsernamePasswordToken("jdoe", "wrong"));
  }

  public void testAuthenticationReturnsNullForUnknownUser () {

    Assert.assertNull(realm.doGetAuthenticationInfo(new UsernamePasswordToken("nobody", "secret")));
  }

  public void testGetDirectoryGroupsReadsMemberOf () {

    Assert.assertTrue(realm.getDirectoryGroups("jdoe").contains(ADMINS_GROUP_DN));
  }

  public void testGetDirectoryGroupsEmptyForUnknownUser () {

    Assert.assertTrue(realm.getDirectoryGroups("nobody").isEmpty());
  }

  public void testAuthorizationGrantsRoleMappedFromMemberOf () {

    realm.setGroupRoleMap(Collections.singletonMap(ADMINS_GROUP_DN, RoleType.ADMIN));

    Assert.assertTrue(realm.doGetAuthorizationInfo(new SimplePrincipalCollection("jdoe", "test")).getRoles().contains(RoleType.ADMIN.getCode()));
  }
}
