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
package org.smallmind.sleuth.runner;

import org.testng.Assert;
import org.testng.annotations.Test;

// The identifier map and counter are process-global static state, so these assertions are kept
// relational (stability, distinctness, monotonicity) rather than tied to absolute values that other
// tests in the same JVM would perturb. Unique class/method names avoid collisions with real test
// identities allocated elsewhere during the run.
@Test(groups = "unit")
public class TestIdentifierTest {

  public void testSamePairResolvesToAStableIdentifier () {

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestAlpha", "methodOne");

    long first = TestIdentifier.getTestIdentifier();

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestAlpha", "methodOne");

    Assert.assertEquals(TestIdentifier.getTestIdentifier(), first);
  }

  public void testDistinctPairsResolveToDistinctIdentifiers () {

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestBeta", "methodOne");

    long first = TestIdentifier.getTestIdentifier();

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestBeta", "methodTwo");

    long second = TestIdentifier.getTestIdentifier();

    Assert.assertNotEquals(second, first);

    // Returning to the original pair restores its originally allocated identifier.
    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestBeta", "methodOne");
    Assert.assertEquals(TestIdentifier.getTestIdentifier(), first);
  }

  public void testSuiteLevelNullMethodNameIsStable () {

    // Suites are identified with a null method name. Re-resolving the same suite key must not throw
    // (TestKey.equals has to compare the null method-name component null-safely) and must be stable.
    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestSuite", null);

    long first = TestIdentifier.getTestIdentifier();

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestSuite", null);

    Assert.assertEquals(TestIdentifier.getTestIdentifier(), first);
  }

  public void testNewPairAllocatesAGreaterIdentifier () {

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestGamma", "earlier");

    long earlier = TestIdentifier.getTestIdentifier();

    TestIdentifier.updateIdentifier("org.smallmind.sleuth.IdentTestGamma", "later");

    Assert.assertTrue(TestIdentifier.getTestIdentifier() > earlier, "A freshly seen pair must receive a higher identifier from the monotonic counter");
  }
}
