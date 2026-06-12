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
package org.smallmind.sleuth.runner.annotation;

import java.lang.reflect.Method;
import org.smallmind.nutsnbolts.util.Pair;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

// An unqualified Test refers to Sleuth's own annotation (this test lives in that package); the TestNG
// annotations used to build the fixtures and the run marker are fully qualified.
@org.testng.annotations.Test(groups = "unit")
public class TestNGAnnotationTranslatorTest {

  private static Test sleuthTestFor (AnnotationDictionary dictionary, String methodName) {

    for (Pair<Method, Test> pair : dictionary.getTestMethodology()) {
      if (pair.first().getName().equals(methodName)) {

        return pair.second();
      }
    }

    return null;
  }

  public void testClassLevelTestMapsToSuiteWithGroupsAndPriority () {

    AnnotationDictionary dictionary = new TestNGAnnotationTranslator().process(TestNgFixture.class);

    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertEquals(dictionary.getSuite().groups(), new String[] {"ng-group"});
    Assert.assertEquals(dictionary.getSuite().priority(), 3);
  }

  public void testLifecycleAnnotationsAreMapped () {

    AnnotationDictionary dictionary = new TestNGAnnotationTranslator().process(TestNgFixture.class);

    Assert.assertNotNull(dictionary.getBeforeSuiteMethodology());
    Assert.assertNotNull(dictionary.getAfterSuiteMethodology());
    Assert.assertNotNull(dictionary.getBeforeTestMethodology());
    Assert.assertNotNull(dictionary.getAfterTestMethodology());
  }

  public void testExpectedExceptionsAndDependenciesAreForwarded () {

    AnnotationDictionary dictionary = new TestNGAnnotationTranslator().process(TestNgFixture.class);
    Test secondary = sleuthTestFor(dictionary, "secondary");

    Assert.assertNotNull(secondary, "The @Test method should be translated");
    Assert.assertEquals(secondary.expectedExceptions(), new Class[] {IllegalStateException.class});
    Assert.assertEquals(secondary.dependsOn(), new String[] {"primary"});
    // TestNG has no soft-ordering equivalent, so executeAfter is left empty rather than guessed.
    Assert.assertEquals(secondary.executeAfter().length, 0);
  }

  public void testEmptyGroupsBecomeNull () {

    AnnotationDictionary dictionary = new TestNGAnnotationTranslator().process(GrouplessFixture.class);

    Assert.assertNull(dictionary.getSuite().groups());
  }

  @org.testng.annotations.Test(groups = {"ng-group"}, priority = 3)
  public static class TestNgFixture {

    @BeforeClass
    public void beforeClass () {

    }

    @AfterClass
    public void afterClass () {

    }

    @BeforeMethod
    public void beforeMethod () {

    }

    @AfterMethod
    public void afterMethod () {

    }

    @org.testng.annotations.Test
    public void primary () {

    }

    @org.testng.annotations.Test(dependsOnMethods = {"primary"}, expectedExceptions = IllegalStateException.class)
    public void secondary () {

    }
  }

  @org.testng.annotations.Test
  public static class GrouplessFixture {

    @org.testng.annotations.Test
    public void only () {

    }
  }
}
