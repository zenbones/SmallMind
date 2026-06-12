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

@org.testng.annotations.Test(groups = "unit")
public class AnnotationDictionaryTest {

  private static Method method (String name)
    throws NoSuchMethodException {

    return Fixture.class.getMethod(name);
  }

  private static TestLiteral testLiteral () {

    return new TestLiteral(0, new String[0], new String[0], new Class[0], true);
  }

  private static int countMethodology (AnnotationMethodology<Test> methodology) {

    int count = 0;

    for (Pair<Method, Test> ignored : methodology) {
      count++;
    }

    return count;
  }

  public void testGetSuiteSubstitutesAnEnabledDefaultWhenUnset () {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    Assert.assertNotNull(dictionary.getSuite());
    Assert.assertTrue(dictionary.getSuite().enabled());
    Assert.assertEquals(dictionary.getSuite().priority(), 0);
  }

  public void testSetSuiteReplacesTheDefault () {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.setSuite(new SuiteLiteral(new String[] {"group"}, 7, new String[0], new String[0], true));

    Assert.assertEquals(dictionary.getSuite().priority(), 7);
    Assert.assertEquals(dictionary.getSuite().groups(), new String[] {"group"});
  }

  public void testEmptyDictionaryIsNotImplemented () {

    Assert.assertFalse(new AnnotationDictionary().isImplemented());
  }

  public void testSuitePresenceMakesDictionaryImplemented () {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.setSuite(new SuiteLiteral());

    Assert.assertTrue(dictionary.isImplemented());
  }

  // Each test adds two methods: the first exercises the lazy methodology creation, the second the
  // already-present path, and the presence of any lifecycle methodology alone marks the dictionary
  // implemented.
  public void testBeforeSuiteMethodAloneMakesDictionaryImplemented ()
    throws NoSuchMethodException {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.addBeforeSuiteMethod(method("alpha"), new BeforeSuiteLiteral());
    dictionary.addBeforeSuiteMethod(method("beta"), new BeforeSuiteLiteral());

    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertNotNull(dictionary.getBeforeSuiteMethodology());
  }

  public void testAfterSuiteMethodAloneMakesDictionaryImplemented ()
    throws NoSuchMethodException {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.addAfterSuiteMethod(method("alpha"), new AfterSuiteLiteral());
    dictionary.addAfterSuiteMethod(method("beta"), new AfterSuiteLiteral());

    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertNotNull(dictionary.getAfterSuiteMethodology());
  }

  public void testBeforeTestMethodAloneMakesDictionaryImplemented ()
    throws NoSuchMethodException {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.addBeforeTestMethod(method("alpha"), new BeforeTestLiteral());
    dictionary.addBeforeTestMethod(method("beta"), new BeforeTestLiteral());

    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertNotNull(dictionary.getBeforeTestMethodology());
  }

  public void testAfterTestMethodAloneMakesDictionaryImplemented ()
    throws NoSuchMethodException {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    dictionary.addAfterTestMethod(method("alpha"), new AfterTestLiteral());
    dictionary.addAfterTestMethod(method("beta"), new AfterTestLiteral());

    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertNotNull(dictionary.getAfterTestMethodology());
  }

  public void testTestMethodologyIsLazyAndAccumulates ()
    throws NoSuchMethodException {

    AnnotationDictionary dictionary = new AnnotationDictionary();

    Assert.assertNull(dictionary.getTestMethodology());

    dictionary.addTestMethod(method("alpha"), testLiteral());
    Assert.assertTrue(dictionary.isImplemented());
    Assert.assertNotNull(dictionary.getTestMethodology());
    Assert.assertEquals(countMethodology(dictionary.getTestMethodology()), 1);

    dictionary.addTestMethod(method("beta"), testLiteral());
    Assert.assertEquals(countMethodology(dictionary.getTestMethodology()), 2);
  }

  public static class Fixture {

    public void alpha () {

    }

    public void beta () {

    }
  }
}
