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
 * Holds the complete set of Sleuth lifecycle and test metadata discovered on a single test class.
 * <p>
 * An {@link AnnotationProcessor} populates one {@code AnnotationDictionary} per class by delegating
 * to each registered {@link AnnotationTranslator}. Callers then retrieve the class-level {@link Suite}
 * descriptor and the ordered collections of lifecycle and test methods, each wrapped in an
 * {@link AnnotationMethodology} instance that provides invocation support.
 * <p>
 * A dictionary is considered <em>implemented</em> — via {@link #isImplemented()} — when at least one
 * Sleuth annotation element is present: a class-level {@link Suite}, or one or more lifecycle or test
 * methods. An empty dictionary indicates that no supported annotations were found.
 *
 * @see AnnotationProcessor
 * @see AnnotationMethodology
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
   * Returns the {@link Suite} annotation for the class, substituting a default instance when none
   * was explicitly declared.
   *
   * @return declared suite annotation, or a default {@link SuiteLiteral} when absent; never {@code null}
   */
  public Suite getSuite () {

    return (suite == null) ? DEFAULT_SUITE : suite;
  }

  /**
   * Records the class-level {@link Suite} annotation.
   *
   * @param suite suite annotation to associate with the class; must not be {@code null}
   */
  public void setSuite (Suite suite) {

    this.suite = suite;
  }

  /**
   * Returns the ordered collection of {@link BeforeSuite}-annotated methods.
   *
   * @return before-suite methodology, or {@code null} when no such methods are present
   */
  public AnnotationMethodology<BeforeSuite> getBeforeSuiteMethodology () {

    return beforeSuiteMethodology;
  }

  /**
   * Registers a {@link BeforeSuite}-annotated method, creating the methodology on first call.
   *
   * @param method      reflected method carrying the annotation; must not be {@code null}
   * @param beforeSuite associated annotation instance; must not be {@code null}
   */
  public void addBeforeSuiteMethod (Method method, BeforeSuite beforeSuite) {

    if (beforeSuiteMethodology == null) {
      beforeSuiteMethodology = new AnnotationMethodology<>();
    }
    beforeSuiteMethodology.add(method, beforeSuite);
  }

  /**
   * Returns the ordered collection of {@link AfterSuite}-annotated methods.
   *
   * @return after-suite methodology, or {@code null} when no such methods are present
   */
  public AnnotationMethodology<AfterSuite> getAfterSuiteMethodology () {

    return afterSuiteMethodology;
  }

  /**
   * Registers an {@link AfterSuite}-annotated method, creating the methodology on first call.
   *
   * @param method     reflected method carrying the annotation; must not be {@code null}
   * @param afterSuite associated annotation instance; must not be {@code null}
   */
  public void addAfterSuiteMethod (Method method, AfterSuite afterSuite) {

    if (afterSuiteMethodology == null) {
      afterSuiteMethodology = new AnnotationMethodology<>();
    }
    afterSuiteMethodology.add(method, afterSuite);
  }

  /**
   * Returns the ordered collection of {@link BeforeTest}-annotated methods.
   *
   * @return before-test methodology, or {@code null} when no such methods are present
   */
  public AnnotationMethodology<BeforeTest> getBeforeTestMethodology () {

    return beforeTestMethodology;
  }

  /**
   * Registers a {@link BeforeTest}-annotated method, creating the methodology on first call.
   *
   * @param method     reflected method carrying the annotation; must not be {@code null}
   * @param beforeTest associated annotation instance; must not be {@code null}
   */
  public void addBeforeTestMethod (Method method, BeforeTest beforeTest) {

    if (beforeTestMethodology == null) {
      beforeTestMethodology = new AnnotationMethodology<>();
    }
    beforeTestMethodology.add(method, beforeTest);
  }

  /**
   * Returns the ordered collection of {@link AfterTest}-annotated methods.
   *
   * @return after-test methodology, or {@code null} when no such methods are present
   */
  public AnnotationMethodology<AfterTest> getAfterTestMethodology () {

    return afterTestMethodology;
  }

  /**
   * Registers an {@link AfterTest}-annotated method, creating the methodology on first call.
   *
   * @param method    reflected method carrying the annotation; must not be {@code null}
   * @param afterTest associated annotation instance; must not be {@code null}
   */
  public void addAfterTestMethod (Method method, AfterTest afterTest) {

    if (afterTestMethodology == null) {
      afterTestMethodology = new AnnotationMethodology<>();
    }
    afterTestMethodology.add(method, afterTest);
  }

  /**
   * Returns the ordered collection of {@link Test}-annotated methods.
   *
   * @return test methodology, or {@code null} when no test methods are present
   */
  public AnnotationMethodology<Test> getTestMethodology () {

    return testMethodology;
  }

  /**
   * Registers a {@link Test}-annotated method, creating the methodology on first call.
   *
   * @param method reflected method carrying the annotation; must not be {@code null}
   * @param test   associated annotation instance; must not be {@code null}
   */
  public void addTestMethod (Method method, Test test) {

    if (testMethodology == null) {
      testMethodology = new AnnotationMethodology<>();
    }
    testMethodology.add(method, test);
  }

  /**
   * Returns {@code true} when at least one supported annotation element is present on the class.
   * <p>
   * An un-implemented dictionary indicates that no registered translator found any supported
   * annotations, and the class should be excluded from execution.
   *
   * @return {@code true} if this dictionary contains any lifecycle or test annotation data
   */
  public boolean isImplemented () {

    return (suite != null) || (testMethodology != null) || (beforeSuiteMethodology != null) || (afterSuiteMethodology != null) || (beforeTestMethodology != null) || (afterTestMethodology != null);
  }
}
