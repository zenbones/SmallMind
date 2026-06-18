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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.smallmind.nutsnbolts.util.MutationUtility;

/**
 * Static driver that polls one or more {@link TestCondition}s until they are satisfied or a shared
 * timeout elapses, throwing {@link TestConditionTimeoutException} on timeout. Polling sleeps one
 * second between attempts; the deadline is measured from the moment the call begins and is shared
 * across every condition. Both methods treat a {@code null} or empty {@code testConditions} array
 * as nothing to do and return immediately.
 */
public class TestConditions {

  /**
   * Waits for each condition in turn, advancing to the next only once the current one is satisfied.
   * Every condition is polled against a single deadline established when the method is entered, so
   * the {@code timeoutSeconds} budget is shared across all of them rather than reset per condition.
   * The first condition that has not been satisfied by the deadline aborts the call.
   *
   * @param timeoutSeconds the overall budget, in seconds, shared across all conditions
   * @param testConditions the conditions to satisfy, evaluated left to right; may be {@code null}
   * or empty, in which case the method returns immediately
   * @throws TestConditionTimeoutException if a condition is still unsatisfied when the shared
   * deadline is reached
   * @throws Exception if a condition's {@link TestCondition#test()} cannot be carried out, or the
   * polling sleep is interrupted
   */
  public static void serial (int timeoutSeconds, TestCondition... testConditions)
    throws Exception {

    if (testConditions != null) {

      long start = System.currentTimeMillis();

      for (TestCondition condition : testConditions) {

        TestConditionFailure failure;
        boolean first = true;

        do {
          if (!first) {
            Thread.sleep(1000);
          }

          first = false;
        } while (((failure = condition.test()) != null) && ((System.currentTimeMillis() - start) < (timeoutSeconds * 1000L)));

        if (failure != null) {
          throw new TestConditionTimeoutException(timeoutSeconds, List.of(failure));
        }
      }
    }
  }

  /**
   * Waits for every condition together, re-testing the full set on each polling round and dropping
   * conditions as they become satisfied. The method returns as soon as no conditions remain, or
   * aborts once the deadline is reached with any still outstanding. Unlike {@link #serial}, a slow
   * condition does not hold up the evaluation of the others.
   *
   * @param timeoutSeconds the overall budget, in seconds, allowed for all conditions to be satisfied
   * @param testConditions the conditions to satisfy, all polled on each round; may be {@code null}
   * or empty, in which case the method returns immediately
   * @throws TestConditionTimeoutException if any conditions remain unsatisfied when the deadline is
   * reached; the exception reports every outstanding failure
   * @throws Exception if a condition's {@link TestCondition#test()} cannot be carried out, or the
   * polling sleep is interrupted
   */
  public static void parallel (int timeoutSeconds, TestCondition... testConditions)
    throws Exception {

    if (testConditions != null) {

      Map<TestCondition, TestConditionFailure> remainingConditionMap = MutationUtility.toMap(testConditions, testCondition -> testCondition, testCondition -> null);
      boolean first = true;
      long start = System.currentTimeMillis();

      do {

        Iterator<TestCondition> conditionIter = remainingConditionMap.keySet().iterator();

        if (!first) {
          Thread.sleep(1000);
        }

        while (conditionIter.hasNext()) {

          TestCondition condition;
          TestConditionFailure failure;

          if ((failure = (condition = conditionIter.next()).test()) == null) {
            conditionIter.remove();
          } else {
            remainingConditionMap.put(condition, failure);
          }
        }

        first = false;
      } while ((!remainingConditionMap.isEmpty()) && ((System.currentTimeMillis() - start) < (timeoutSeconds * 1000L)));

      if (!remainingConditionMap.isEmpty()) {
        throw new TestConditionTimeoutException(timeoutSeconds, remainingConditionMap.values());
      }
    }
  }
}
