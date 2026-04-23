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
 * Identifies a class as a Sleuth test suite and declares its scheduling constraints.
 * <p>
 * Apply this annotation at the class level to participate in Sleuth test discovery. The
 * {@link #groups()} attribute controls group-filter inclusion. The {@link #priority()} attribute
 * controls cross-suite ordering. The {@link #dependsOn()} attribute introduces hard prerequisites:
 * if any named suite fails, this suite is marked skipped and its culprit is propagated. The
 * {@link #executeAfter()} attribute imposes soft ordering without failure propagation.
 *
 * @see Test
 * @see SuiteLiteral
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Suite {

  /**
   * Groups that this suite belongs to.
   * <p>
   * When the runner filters by group, only suites whose {@code groups} array intersects the
   * requested set are executed. An empty array means the suite belongs to no named group and
   * will be excluded when any group filter is active.
   *
   * @return group names for this suite; never {@code null}
   */
  String[] groups ();

  /**
   * Relative execution priority among all suites.
   * <p>
   * Lower values are scheduled first. Suites sharing the same priority may execute concurrently
   * subject to the thread-pool limit for the {@link org.smallmind.sleuth.runner.TestTier#SUITE}
   * tier.
   *
   * @return priority value; defaults to {@code 0}
   */
  int priority () default 0;

  /**
   * Names of suites that must finish execution before this suite may start.
   * <p>
   * Unlike {@link #dependsOn()}, completion — regardless of outcome — is sufficient to unblock
   * this suite. No culprit is inherited from a named suite's failure.
   *
   * @return suite names imposing a soft ordering constraint; defaults to empty
   */
  String[] executeAfter () default {};

  /**
   * Names of suites that must succeed before this suite may start.
   * <p>
   * If any named suite produced a failure or error, this suite is skipped and the culprit from
   * the failed prerequisite is propagated into its result.
   *
   * @return suite names that are hard prerequisites; defaults to empty
   */
  String[] dependsOn () default {};

  /**
   * Whether this suite participates in any run.
   * <p>
   * When {@code false}, the suite is unconditionally excluded regardless of group filtering
   * or dependency configuration.
   *
   * @return {@code true} if the suite should execute; defaults to {@code true}
   */
  boolean enabled () default true;
}
