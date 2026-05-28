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
package org.smallmind.mongodb.utility.spring;

import com.mongodb.MongoCredential;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MongoCredentialFactoryBeanTest {

  public void testIsSingletonReturnsTrue () {

    Assert.assertTrue(new MongoCredentialFactoryBean().isSingleton());
  }

  public void testGetObjectTypeReturnsMongoCredentialClass () {

    Assert.assertEquals(new MongoCredentialFactoryBean().getObjectType(), MongoCredential.class);
  }

  public void testAfterPropertiesSetBuildsScramSha1Credential () {

    MongoCredentialFactoryBean bean = new MongoCredentialFactoryBean();

    bean.setUser("app-service");
    bean.setPassword("hunter2");
    bean.setSource("admin");
    bean.afterPropertiesSet();

    MongoCredential credential = bean.getObject();

    Assert.assertNotNull(credential);
    Assert.assertEquals(credential.getMechanism(), "SCRAM-SHA-1");
    Assert.assertEquals(credential.getUserName(), "app-service");
    Assert.assertEquals(credential.getSource(), "admin");
    Assert.assertEquals(credential.getPassword(), "hunter2".toCharArray());
  }

  public void testGetObjectBeforeAfterPropertiesSetReturnsNull () {

    MongoCredentialFactoryBean bean = new MongoCredentialFactoryBean();

    Assert.assertNull(bean.getObject());
  }
}
