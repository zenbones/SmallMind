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
package org.smallmind.sleuth.maven.surefire;

import java.lang.reflect.Method;
import org.testng.Assert;
import org.testng.annotations.Test;

// parseGroups is a pure private helper; it is reached reflectively because driving it through invoke()
// would require the full Surefire provider wiring (an integration-level concern).
@Test(groups = "unit")
public class SleuthProviderTest {

  private static String[] parseGroups (String groupsParameter)
    throws Exception {

    Method method = SleuthProvider.class.getDeclaredMethod("parseGroups", String.class);

    method.setAccessible(true);

    return (String[])method.invoke(new SleuthProvider(null), groupsParameter);
  }

  public void testNullInputYieldsNull ()
    throws Exception {

    Assert.assertNull(parseGroups(null));
  }

  public void testEmptyInputYieldsNull ()
    throws Exception {

    Assert.assertNull(parseGroups(""));
  }

  public void testAllSentinelYieldsEmptyArray ()
    throws Exception {

    Assert.assertEquals(parseGroups("all"), new String[0]);
  }

  public void testAllSentinelAmongOthersStillYieldsEmptyArray ()
    throws Exception {

    Assert.assertEquals(parseGroups("unit,all,integration"), new String[0]);
  }

  public void testSingleGroupIsParsed ()
    throws Exception {

    Assert.assertEquals(parseGroups("unit"), new String[] {"unit"});
  }

  public void testCommaSeparatedGroupsArePreservedInOrder ()
    throws Exception {

    Assert.assertEquals(parseGroups("unit,integration,smoke"), new String[] {"unit", "integration", "smoke"});
  }
}
