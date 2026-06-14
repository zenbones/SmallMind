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

import org.smallmind.nutsnbolts.namespace.shiro.realm.ActiveDirectoryLdapRealm;
import org.smallmind.nutsnbolts.namespace.shiro.realm.LdapConnectionDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ActiveDirectoryLdapRealmFactoryBeanTest {

  private ActiveDirectoryLdapRealmFactoryBean initializedBean ()
    throws Exception {

    ActiveDirectoryLdapRealmFactoryBean factoryBean = new ActiveDirectoryLdapRealmFactoryBean();

    factoryBean.setConnectionDetails(new LdapConnectionDetails());
    factoryBean.setSearchPath("ou=people");
    factoryBean.setDomain("example.com");
    factoryBean.afterPropertiesSet();

    return factoryBean;
  }

  public void testBuildsActiveDirectoryLdapRealm ()
    throws Exception {

    Assert.assertTrue(initializedBean().getObject() instanceof ActiveDirectoryLdapRealm);
  }

  public void testObjectTypeIsConcreteRealm () {

    Assert.assertEquals(new ActiveDirectoryLdapRealmFactoryBean().getObjectType(), ActiveDirectoryLdapRealm.class);
  }

  public void testIsSingleton () {

    Assert.assertTrue(new ActiveDirectoryLdapRealmFactoryBean().isSingleton());
  }

  public void testObjectIsCachedAcrossCalls ()
    throws Exception {

    ActiveDirectoryLdapRealmFactoryBean factoryBean = initializedBean();

    Assert.assertSame(factoryBean.getObject(), factoryBean.getObject());
  }

  public void testObjectIsNullBeforeInitialization ()
    throws Exception {

    Assert.assertNull(new ActiveDirectoryLdapRealmFactoryBean().getObject());
  }
}
