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
package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class ClassPathTemplateSourceTest {

  private static final String RESOURCE_PATH = "org/smallmind/nutsnbolts/freemarker/greeting.ftl";

  public void testExistsReturnsTrueForPresentResource () {

    ClassPathTemplateSource source = new ClassPathTemplateSource(Thread.currentThread().getContextClassLoader(), RESOURCE_PATH);

    Assert.assertTrue(source.exists());
    Assert.assertNotNull(source.getInputStream());
  }

  public void testExistsReturnsFalseForMissingResource () {

    ClassPathTemplateSource source = new ClassPathTemplateSource(Thread.currentThread().getContextClassLoader(), "missing.ftl");

    Assert.assertFalse(source.exists());
    Assert.assertNull(source.getInputStream());
  }

  public void testEqualsAndHashCodeBasedOnLoaderAndName () {

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    ClassPathTemplateSource a = new ClassPathTemplateSource(cl, RESOURCE_PATH);
    ClassPathTemplateSource b = new ClassPathTemplateSource(cl, RESOURCE_PATH);
    ClassPathTemplateSource c = new ClassPathTemplateSource(cl, "other.ftl");

    Assert.assertEquals(a, b);
    Assert.assertNotEquals(a, c);
    Assert.assertEquals(a.hashCode(), b.hashCode());
  }

  public void testAccessorsReturnConstructorArguments () {

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    ClassPathTemplateSource source = new ClassPathTemplateSource(cl, RESOURCE_PATH);

    Assert.assertSame(source.getClassLoader(), cl);
    Assert.assertEquals(source.getName(), RESOURCE_PATH);
  }

  public void testCloseOnMissingResourceIsNoop ()
    throws IOException {

    new ClassPathTemplateSource(Thread.currentThread().getContextClassLoader(), "missing.ftl").close();
  }
}
