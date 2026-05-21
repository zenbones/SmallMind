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

public class TestConditions {

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
