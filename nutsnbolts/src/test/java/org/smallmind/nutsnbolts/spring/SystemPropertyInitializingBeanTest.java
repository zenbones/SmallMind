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
package org.smallmind.nutsnbolts.spring;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class SystemPropertyInitializingBeanTest {

  public void testPostProcessSetsAbsentSystemProperties () {

    String key = "nutsnbolts.test.spring.absent." + System.nanoTime();

    System.clearProperty(key);

    try {

      Map<String, String> values = new HashMap<>();

      values.put(key, "from-bean");

      SystemPropertyInitializingBean bean = new SystemPropertyInitializingBean();

      bean.setPropertyMap(values);

      GenericApplicationContext context = new GenericApplicationContext();

      try {
        bean.postProcessBeanFactory(context.getDefaultListableBeanFactory());
        Assert.assertEquals(System.getProperty(key), "from-bean");
      } finally {
        context.close();
        System.clearProperty(key);
      }
    } catch (Throwable throwable) {
      System.clearProperty(key);
      throw throwable;
    }
  }

  public void testPostProcessRespectsExistingSystemPropertyWhenOverrideIsFalse () {

    String key = "nutsnbolts.test.spring.existing." + System.nanoTime();

    System.setProperty(key, "original");
    try {

      Map<String, String> values = new HashMap<>();

      values.put(key, "should-be-ignored");

      SystemPropertyInitializingBean bean = new SystemPropertyInitializingBean();

      bean.setPropertyMap(values);
      bean.setOverride(false);

      GenericApplicationContext context = new GenericApplicationContext();

      try {
        bean.postProcessBeanFactory(context.getDefaultListableBeanFactory());
        Assert.assertEquals(System.getProperty(key), "original");
      } finally {
        context.close();
      }
    } finally {
      System.clearProperty(key);
    }
  }

  public void testPostProcessOverridesWhenFlagSet () {

    String key = "nutsnbolts.test.spring.override." + System.nanoTime();

    System.setProperty(key, "original");
    try {

      Map<String, String> values = new HashMap<>();

      values.put(key, "new-value");

      SystemPropertyInitializingBean bean = new SystemPropertyInitializingBean();

      bean.setPropertyMap(values);
      bean.setOverride(true);

      GenericApplicationContext context = new GenericApplicationContext();

      try {
        bean.postProcessBeanFactory(context.getDefaultListableBeanFactory());
        Assert.assertEquals(System.getProperty(key), "new-value");
      } finally {
        context.close();
      }
    } finally {
      System.clearProperty(key);
    }
  }

  public void testOrderAccessorsRoundTrip () {

    SystemPropertyInitializingBean bean = new SystemPropertyInitializingBean();

    bean.setOrder(42);

    Assert.assertEquals(bean.getOrder(), 42);
  }
}
