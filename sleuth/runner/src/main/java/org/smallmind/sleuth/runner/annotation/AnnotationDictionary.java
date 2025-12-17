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

/**
 * Captures the set of Sleuth annotations discovered on a test class.
 * <p>
 * Provides access to lifecycle and test method methodologies for execution.
 */
public class AnnotationDictionary {

  private static final Suite DEFAULT_SUITE = new SuiteLiteral();
  private AnnotationMethodology<BeforeSuite> beforeSuiteMethodology;
  private AnnotationMethodology<AfterSuite> afterSuiteMethodology;
  private AnnotationMethodology<BeforeTest> beforeTestMethodology;
  private AnnotationMethodology<AfterTest> afterTestMethodology;
  private AnnotationMethodology<Test> testMethodology;
  private Suite suite;

  /**
   * @return resolved {@link Suite} annotation or a default when absent
   */
  public Suite getSuite () {

    return (suite == null) ? DEFAULT_SUITE : suite;
  }

  /**
   * Records the {@link Suite} annotation for the class.
   *
   * @param suite suite annotation
   */
  public void setSuite (Suite suite) {

    this.suite = suite;
  }

  /**
   * @return lifecycle methodology for {@link BeforeSuite} methods or {@code null}
   */
  public AnnotationMethodology<BeforeSuite> getBeforeSuiteMethodology () {

    return beforeSuiteMethodology;
  }

  /**
   * Adds a {@link BeforeSuite} method to the methodology.
   *
   * @param method       reflected method
   * @param beforeSuite  annotation instance
   */
  public void addBeforeSuiteMethod (Method method, BeforeSuite beforeSuite) {

    if (beforeSuiteMethodology == null) {
      beforeSuiteMethodology = new AnnotationMethodology<>();
    }
    beforeSuiteMethodology.add(method, beforeSuite);
  }

  /**
   * @return lifecycle methodology for {@link AfterSuite} methods or {@code null}
   */
  public AnnotationMethodology<AfterSuite> getAfterSuiteMethodology () {

    return afterSuiteMethodology;
  }

  /**
   * Adds an {@link AfterSuite} method to the methodology.
   *
   * @param method      reflected method
   * @param afterSuite  annotation instance
   */
  public void addAfterSuiteMethod (Method method, AfterSuite afterSuite) {

    if (afterSuiteMethodology == null) {
      afterSuiteMethodology = new AnnotationMethodology<>();
    }
    afterSuiteMethodology.add(method, afterSuite);
  }

  /**
   * @return lifecycle methodology for {@link BeforeTest} methods or {@code null}
   */
  public AnnotationMethodology<BeforeTest> getBeforeTestMethodology () {

    return beforeTestMethodology;
  }

  /**
   * Adds a {@link BeforeTest} method to the methodology.
   *
   * @param method      reflected method
   * @param beforeTest  annotation instance
   */
  public void addBeforeTestMethod (Method method, BeforeTest beforeTest) {

    if (beforeTestMethodology == null) {
      beforeTestMethodology = new AnnotationMethodology<>();
    }
    beforeTestMethodology.add(method, beforeTest);
  }

  /**
   * @return lifecycle methodology for {@link AfterTest} methods or {@code null}
   */
  public AnnotationMethodology<AfterTest> getAfterTestMethodology () {

    return afterTestMethodology;
  }

  /**
   * Adds an {@link AfterTest} method to the methodology.
   *
   * @param method     reflected method
   * @param afterTest  annotation instance
   */
  public void addAfterTestMethod (Method method, AfterTest afterTest) {

    if (afterTestMethodology == null) {
      afterTestMethodology = new AnnotationMethodology<>();
    }
    afterTestMethodology.add(method, afterTest);
  }

  /**
   * @return methodology for {@link Test} methods or {@code null}
   */
  public AnnotationMethodology<Test> getTestMethodology () {

    return testMethodology;
  }

  /**
   * Adds a {@link Test} method to the methodology.
   *
   * @param method reflected method
   * @param test   annotation instance
   */
  public void addTestMethod (Method method, Test test) {

    if (testMethodology == null) {
      testMethodology = new AnnotationMethodology<>();
    }
    testMethodology.add(method, test);
  }

  /**
   * @return {@code true} when at least one Sleuth annotation is present on the class
   */
  public boolean isImplemented () {

    return (suite != null) || (testMethodology != null) || (beforeSuiteMethodology != null) || (afterSuiteMethodology != null) || (beforeTestMethodology != null) || (afterTestMethodology != null);
  }
}
