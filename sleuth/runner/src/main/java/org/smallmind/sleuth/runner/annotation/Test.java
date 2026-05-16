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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an individual test case within a Sleuth suite.
 * <p>
 * Methods annotated with {@code @Test} are discovered by {@link NativeAnnotationTranslator},
 * wrapped in a {@link org.smallmind.sleuth.runner.TestRunner}, and scheduled according to their
 * {@link #priority()}, {@link #executeAfter()}, and {@link #dependsOn()} constraints. The
 * annotated method must accept no arguments; its return value is ignored. An {@link AssertionError}
 * thrown by the method is reported as a
 * {@link org.smallmind.sleuth.runner.event.SleuthEventType#FAILURE}; any other exception is
 * reported as an {@link org.smallmind.sleuth.runner.event.SleuthEventType#ERROR}.
 *
 * @see Suite
 * @see TestLiteral
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Test {

  /**
   * Relative execution priority among tests in the same suite.
   * <p>
   * Lower values are scheduled before higher values. Tests with the same priority may execute
   * concurrently subject to the thread-pool limit for the
   * {@link org.smallmind.sleuth.runner.TestTier#TEST} tier.
   *
   * @return priority value; defaults to {@code 0}
   */
  int priority () default 0;

  /**
   * Names of methods in the same suite that must finish execution before this test may start.
   * <p>
   * Unlike {@link #dependsOn()}, a named method's outcome does not affect whether this test runs;
   * only completion ordering is imposed.
   *
   * @return method names imposing a soft ordering constraint; defaults to empty
   */
  String[] executeAfter () default {};

  /**
   * Names of methods in the same suite that must succeed before this test may start.
   * <p>
   * If any named method produced a failure or error, this test is skipped and the culprit from
   * the failed prerequisite is propagated into its result.
   *
   * @return method names that are hard prerequisites; defaults to empty
   */
  String[] dependsOn () default {};

  /**
   * The list of exceptions that a test method is expected to throw. If no exception is thrown, or a different
   * exception than one on this list is thrown, this test will be marked a failure.
   *
   * @return the value
   */
  Class[] expectedExceptions () default {};

  /**
   * Whether this test participates in any run.
   * <p>
   * When {@code false}, the test is unconditionally excluded regardless of suite configuration.
   *
   * @return {@code true} if the test should execute; defaults to {@code true}
   */
  boolean enabled () default true;
}
