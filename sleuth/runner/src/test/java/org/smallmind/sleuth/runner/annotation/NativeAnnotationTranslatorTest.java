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

// The test is in the annotation package, so an unqualified Test/Suite refers to Sleuth's own
// annotations; the TestNG run marker is fully qualified to keep that distinction unambiguous.
@org.testng.annotations.Test(groups = "unit")
public class NativeAnnotationTranslatorTest {

  public void testRecognisesEveryNativeLifecycleAnnotation () {

    AnnotationDictionary dictionary = new NativeAnnotationTranslator().process(NativeFixture.class);

    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertEquals(dictionary.getSuite().groups(), new String[] {"native-group"});
    Assert.assertNotNull(dictionary.getBeforeSuiteMethodology());
    Assert.assertNotNull(dictionary.getAfterSuiteMethodology());
    Assert.assertNotNull(dictionary.getBeforeTestMethodology());
    Assert.assertNotNull(dictionary.getAfterTestMethodology());
    Assert.assertNotNull(dictionary.getTestMethodology());
  }

  public void testTestMethodIsCarriedThrough () {

    AnnotationDictionary dictionary = new NativeAnnotationTranslator().process(NativeFixture.class);

    int testCount = 0;
    String testMethodName = null;

    for (Pair<Method, Test> pair : dictionary.getTestMethodology()) {
      testCount++;
      testMethodName = pair.first().getName();
    }

    Assert.assertEquals(testCount, 1);
    Assert.assertEquals(testMethodName, "aTest");
  }

  public void testClassWithNoNativeAnnotationsIsNotImplemented () {

    Assert.assertFalse(new NativeAnnotationTranslator().process(Unannotated.class).isImplemented());
  }

  @Suite(groups = {"native-group"})
  public static class NativeFixture {

    @BeforeSuite
    public void beforeSuite () {

    }

    @AfterSuite
    public void afterSuite () {

    }

    @BeforeTest
    public void beforeTest () {

    }

    @AfterTest
    public void afterTest () {

    }

    @Test
    public void aTest () {

    }
  }

  public static class Unannotated {

    public void notATest () {

    }
  }
}
