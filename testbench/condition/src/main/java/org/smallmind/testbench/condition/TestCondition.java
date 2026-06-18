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
package org.smallmind.testbench.condition;

/**
 * A single, repeatable precondition check evaluated by {@link TestConditions} before a test runs.
 * A condition is expected to be polled: each call to {@link #test()} reports the <em>current</em>
 * state of the world rather than waiting internally, leaving the retry-and-timeout loop to the
 * caller.
 *
 * <p>This is a functional interface; implementations typically probe an external resource (a
 * Docker container, a socket, a message broker) and report whether it has reached the desired
 * state. Existing implementations include {@link ContainerAbsentTestCondition},
 * {@link GreenmailAbsentTestCondition}, and {@link RabbitMQAvailableTestCondition}.
 */
@FunctionalInterface
public interface TestCondition {

  /**
   * Evaluates the condition once against the current state of the world.
   *
   * @return {@code null} if the condition is satisfied, or a {@link TestConditionFailure}
   * describing why it is not yet satisfied
   * @throws Exception if the underlying probe cannot be carried out at all (as opposed to simply
   * reporting an unsatisfied condition)
   */
  TestConditionFailure test ()
    throws Exception;
}
