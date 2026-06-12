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
package org.smallmind.scribe.pen;

import java.time.LocalDateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class RolloverTest {

  /**
   * A {@link RolloverRule} stub with a fixed verdict that records whether it was consulted, so a test
   * can prove short-circuit evaluation never reaches it.
   */
  private static class RecordingRule implements RolloverRule {

    private final boolean verdict;
    private boolean consulted;

    private RecordingRule (boolean verdict) {

      this.verdict = verdict;
    }

    private boolean wasConsulted () {

      return consulted;
    }

    @Override
    public boolean willRollover (long fileSize, long lastModified, long bytesToBeWritten) {

      consulted = true;

      return verdict;
    }
  }

  public void testFirstTriggeringRuleShortCircuitsTheRest () {

    RecordingRule first = new RecordingRule(true);
    RecordingRule second = new RecordingRule(false);

    Assert.assertTrue(new Rollover(first, second).willRollover(0, 0, 0));
    Assert.assertTrue(first.wasConsulted());
    Assert.assertFalse(second.wasConsulted(), "evaluation should have stopped at the first triggering rule");
  }

  public void testNoTriggeringRuleReturnsFalse () {

    Assert.assertFalse(new Rollover(new RecordingRule(false), new RecordingRule(false)).willRollover(0, 0, 0));
  }

  public void testEmptyRuleSetNeverRolls () {

    Assert.assertFalse(new Rollover(new RolloverRule[0]).willRollover(0, 0, 0));
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNoArgConstructorLeavesRulesUnsetAndThrowsOnEvaluation () {

    // Documents the pitfall: the no-arg Rollover() leaves the rule array null, so willRollover throws.
    new Rollover().willRollover(0, 0, 0);
  }

  public void testTimestampSuffixDelegatesToTheConfiguredTimestamp () {

    Rollover rollover = new Rollover(new NullTimestamp());

    Assert.assertEquals(rollover.getTimestampSuffix(LocalDateTime.of(2026, 6, 11, 9, 30)), "");
  }

  public void testDefaultSeparatorIsHyphen () {

    Assert.assertEquals(new Rollover().getSeparator(), '-');
  }
}
