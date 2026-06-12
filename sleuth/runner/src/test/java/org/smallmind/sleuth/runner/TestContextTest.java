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

import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.Test;

// The backing store is a per-thread InheritableThreadLocal map that persists for the life of the
// thread, so each method uses unique keys to stay independent of the others (and of any reuse of the
// test thread).
@Test(groups = "unit")
public class TestContextTest {

  public void testPutThenGetReturnsStoredValue () {

    TestContext.put("tc.basic", "stored");

    Assert.assertEquals(TestContext.get("tc.basic"), "stored");
  }

  public void testGetAbsentKeyReturnsNull () {

    Assert.assertNull(TestContext.get("tc.absent.never.written"));
  }

  public void testTypedGetCastsValue () {

    TestContext.put("tc.typed", "casted");

    String value = TestContext.get("tc.typed", String.class);

    Assert.assertEquals(value, "casted");
  }

  public void testTypedGetThrowsOnWrongType () {

    TestContext.put("tc.mistyped", "not a number");

    try {
      TestContext.get("tc.mistyped", Integer.class);
      Assert.fail("Expected a ClassCastException for the wrong requested type");
    } catch (ClassCastException classCastException) {
      // expected
    }
  }

  public void testPutIfAbsentDoesNotOverwrite () {

    TestContext.put("tc.absent.guard", "first");
    TestContext.putIfAbsent("tc.absent.guard", "second");

    Assert.assertEquals(TestContext.get("tc.absent.guard"), "first");
  }

  public void testPutIfAbsentStoresWhenMissing () {

    TestContext.putIfAbsent("tc.absent.fresh", "only");

    Assert.assertEquals(TestContext.get("tc.absent.fresh"), "only");
  }

  public void testValueIsInheritedByChildThread ()
    throws InterruptedException {

    TestContext.put("tc.inherited", "parent-value");

    AtomicReference<Object> childView = new AtomicReference<>();
    Thread child = new Thread(() -> childView.set(TestContext.get("tc.inherited")));

    child.start();
    child.join(2000);

    Assert.assertEquals(childView.get(), "parent-value", "A child thread must inherit the parent's context value");
  }
}
