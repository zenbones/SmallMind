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

import org.smallmind.nutsnbolts.lang.AnnotationLiteral;

/**
 * Concrete {@link Test} annotation instance for programmatic construction at runtime.
 * <p>
 * Used by {@link AnnotationTranslator} implementations — particularly
 * {@link TestNGAnnotationTranslator} — to bridge foreign test-method annotations onto the Sleuth
 * scheduling model without requiring compile-time annotation use.
 *
 * @see Test
 * @see AnnotationTranslator
 */
public class TestLiteral extends AnnotationLiteral<Test> implements Test {

  private final Class<?>[] expectedExceptions;
  private final String[] executeAfter;
  private final String[] dependsOn;
  private final boolean enabled;
  private final int priority;

  /**
   * Constructs a fully specified test literal.
   *
   * @param priority           scheduling priority within the suite; lower values execute first
   * @param executeAfter       names of methods that must finish before this test starts (soft ordering)
   * @param dependsOn          names of methods that must succeed before this test starts (hard dependency)
   * @param expectedExceptions exception types the test method is expected to throw; the test fails if none or a
   *                           different exception is thrown
   * @param enabled            {@code false} to unconditionally exclude this test from execution
   */
  public TestLiteral (int priority, String[] executeAfter, String[] dependsOn, Class<?>[] expectedExceptions, boolean enabled) {

    this.priority = priority;
    this.executeAfter = executeAfter;
    this.dependsOn = dependsOn;
    this.expectedExceptions = expectedExceptions;
    this.enabled = enabled;
  }

  /**
   * @return scheduling priority value; lower values run first
   */
  @Override
  public int priority () {

    return priority;
  }

  /**
   * @return names of methods that must complete (regardless of outcome) before this test starts
   */
  @Override
  public String[] executeAfter () {

    return executeAfter;
  }

  /**
   * @return names of methods that must succeed before this test starts
   */
  @Override
  public String[] dependsOn () {

    return dependsOn;
  }

  /**
   * @return exception types this test is expected to throw; the test fails if none or a different exception is thrown
   */
  @Override
  public Class<?>[] expectedExceptions () {

    return expectedExceptions;
  }

  /**
   * @return {@code true} if this test participates in execution; {@code false} to exclude it
   */
  @Override
  public boolean enabled () {

    return enabled;
  }
}
