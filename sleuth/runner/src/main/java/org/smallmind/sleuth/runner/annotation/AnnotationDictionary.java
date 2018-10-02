/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

public class AnnotationDictionary {

  private static final Suite DEFAULT_SUITE = new SuiteLiteral();
  private AnnotationMethodology<BeforeSuite> beforeSuiteMethodology;
  private AnnotationMethodology<AfterSuite> afterSuiteMethodology;
  private AnnotationMethodology<BeforeTest> beforeTestMethodology;
  private AnnotationMethodology<AfterTest> afterTestMethodology;
  private AnnotationMethodology<Test> testMethodology;
  private Suite suite;

  public Suite getSuite () {

    return (suite == null) ? DEFAULT_SUITE : suite;
  }

  public void setSuite (Suite suite) {

    this.suite = suite;
  }

  public AnnotationMethodology<BeforeSuite> getBeforeSuiteMethodology () {

    return beforeSuiteMethodology;
  }

  public void addBeforeSuiteMethod (Method method, BeforeSuite beforeSuite) {

    if (beforeSuiteMethodology == null) {
      beforeSuiteMethodology = new AnnotationMethodology<>();
    }
    beforeSuiteMethodology.add(method, beforeSuite);
  }

  public AnnotationMethodology<AfterSuite> getAfterSuiteMethodology () {

    return afterSuiteMethodology;
  }

  public void addAfterSuiteMethod (Method method, AfterSuite afterSuite) {

    if (afterSuiteMethodology == null) {
      afterSuiteMethodology = new AnnotationMethodology<>();
    }
    afterSuiteMethodology.add(method, afterSuite);
  }

  public AnnotationMethodology<BeforeTest> getBeforeTestMethodology () {

    return beforeTestMethodology;
  }

  public void addBeforeTestMethod (Method method, BeforeTest beforeTest) {

    if (beforeTestMethodology == null) {
      beforeTestMethodology = new AnnotationMethodology<>();
    }
    beforeTestMethodology.add(method, beforeTest);
  }

  public AnnotationMethodology<AfterTest> getAfterTestMethodology () {

    return afterTestMethodology;
  }

  public void addAfterTestMethod (Method method, AfterTest afterTest) {

    if (afterTestMethodology == null) {
      afterTestMethodology = new AnnotationMethodology<>();
    }
    afterTestMethodology.add(method, afterTest);
  }

  public AnnotationMethodology<Test> getTestMethodology () {

    return testMethodology;
  }

  public void addTestMethod (Method method, Test test) {

    if (testMethodology == null) {
      testMethodology = new AnnotationMethodology<>();
    }
    testMethodology.add(method, test);
  }

  public boolean isImplemented () {

    return (suite != null) || (testMethodology != null) || (beforeSuiteMethodology != null) || (afterSuiteMethodology != null) || (beforeTestMethodology != null) || (afterTestMethodology != null);
  }
}
