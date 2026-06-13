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
package org.smallmind.web.json.doppelganger;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the {@link Visibility} enum's compose and direction-matching behavior.
 */
@Test(groups = "unit")
public class VisibilityTest {

  public void testComposeWithNull () {

    Assert.assertEquals(Visibility.IN.compose(null), Visibility.IN);
    Assert.assertEquals(Visibility.OUT.compose(null), Visibility.OUT);
    Assert.assertEquals(Visibility.BOTH.compose(null), Visibility.BOTH);
  }

  public void testComposeWithSame () {

    Assert.assertEquals(Visibility.IN.compose(Visibility.IN), Visibility.IN);
    Assert.assertEquals(Visibility.OUT.compose(Visibility.OUT), Visibility.OUT);
    Assert.assertEquals(Visibility.BOTH.compose(Visibility.BOTH), Visibility.BOTH);
  }

  public void testComposeWithDifferent () {

    Assert.assertEquals(Visibility.IN.compose(Visibility.OUT), Visibility.BOTH);
    Assert.assertEquals(Visibility.OUT.compose(Visibility.IN), Visibility.BOTH);
    Assert.assertEquals(Visibility.IN.compose(Visibility.BOTH), Visibility.BOTH);
    Assert.assertEquals(Visibility.OUT.compose(Visibility.BOTH), Visibility.BOTH);
  }

  public void testMatches () {

    Assert.assertTrue(Visibility.IN.matches(Direction.IN));
    Assert.assertFalse(Visibility.IN.matches(Direction.OUT));

    Assert.assertTrue(Visibility.OUT.matches(Direction.OUT));
    Assert.assertFalse(Visibility.OUT.matches(Direction.IN));

    Assert.assertTrue(Visibility.BOTH.matches(Direction.IN));
    Assert.assertTrue(Visibility.BOTH.matches(Direction.OUT));
  }
}
