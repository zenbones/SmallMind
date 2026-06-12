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
package org.smallmind.scribe.pen.spring;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.spring.PropertyPlaceholderStringValueResolver;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.LoggerManagerTestSupport;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DynamicClassNameTemplateInitializingBeanTest {

  @BeforeMethod
  public void setUp () {

    new PerApplicationContext();
    LoggerManagerTestSupport.reset();
  }

  @AfterMethod
  public void tearDown () {

    LoggerManagerTestSupport.reset();
  }

  private void seedSpringProperties (Map<String, Object> propertyMap)
    throws org.smallmind.nutsnbolts.property.PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false);
    PropertyPlaceholderStringValueResolver resolver = new PropertyPlaceholderStringValueResolver(expander, propertyMap);

    SpringPropertyAccessorManager.register(new SpringPropertyAccessor(resolver));
  }

  public void testSettersDoNotThrow () {

    DynamicClassNameTemplateInitializingBean initializingBean = new DynamicClassNameTemplateInitializingBean();

    initializingBean.setFilters(new org.smallmind.scribe.pen.Filter[0]);
    initializingBean.setAppenders(new org.smallmind.scribe.pen.Appender[0]);
    initializingBean.setEnhancers(new org.smallmind.scribe.pen.Enhancer[0]);
    initializingBean.setAutoFillLoggerContext(true);
  }

  public void testTemplateRegisteredFromSpringProperties ()
    throws Exception {

    Map<String, Object> propertyMap = new HashMap<>();

    propertyMap.put("log.pattern.app", "com.example.dynamic.*");
    propertyMap.put("log.level.app", "ERROR");
    seedSpringProperties(propertyMap);

    DynamicClassNameTemplateInitializingBean initializingBean = new DynamicClassNameTemplateInitializingBean();

    initializingBean.afterPropertiesSet();

    Logger matchedLogger = LoggerManager.getLogger("com.example.dynamic.Widget");

    Assert.assertNotNull(matchedLogger.getTemplate());
    Assert.assertEquals(matchedLogger.getLevel(), Level.ERROR);
  }

  @Test(expectedExceptions = org.smallmind.scribe.pen.LoggerException.class)
  public void testMissingLevelForPatternThrows ()
    throws Exception {

    Map<String, Object> propertyMap = new HashMap<>();

    propertyMap.put("log.pattern.orphan", "com.example.orphan.*");
    seedSpringProperties(propertyMap);

    DynamicClassNameTemplateInitializingBean initializingBean = new DynamicClassNameTemplateInitializingBean();

    initializingBean.afterPropertiesSet();
  }
}
