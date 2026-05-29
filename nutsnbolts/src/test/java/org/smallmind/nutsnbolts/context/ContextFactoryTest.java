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
package org.smallmind.nutsnbolts.context;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ContextFactoryTest {

  public static class TestContext implements Context {

    private final String value;

    public TestContext (String value) {

      this.value = value;
    }

    public String getValue () {

      return value;
    }
  }

  public static class OtherContext implements Context {

  }

  @AfterMethod
  public void clearStacks () {

    ContextFactory.clearContextTrace(TestContext.class);
    ContextFactory.clearContextTrace(OtherContext.class);
  }

  public void testPushAndGetReturnsTopOfStack () {

    ContextFactory.pushContext(new TestContext("a"));
    ContextFactory.pushContext(new TestContext("b"));

    Assert.assertEquals(ContextFactory.getContext(TestContext.class).getValue(), "b");
    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 2);
  }

  public void testPopReturnsTopAndShrinksStack () {

    TestContext bottom = new TestContext("bottom");
    TestContext top = new TestContext("top");

    ContextFactory.pushContext(bottom);
    ContextFactory.pushContext(top);

    Assert.assertSame(ContextFactory.popContext(TestContext.class), top);
    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 1);
    Assert.assertSame(ContextFactory.popContext(TestContext.class), bottom);
    Assert.assertNull(ContextFactory.popContext(TestContext.class));
  }

  public void testExistsReflectsCurrentStackState () {

    Assert.assertFalse(ContextFactory.exists(TestContext.class));
    ContextFactory.pushContext(new TestContext("only"));
    Assert.assertTrue(ContextFactory.exists(TestContext.class));
    ContextFactory.popContext(TestContext.class);
    Assert.assertFalse(ContextFactory.exists(TestContext.class));
  }

  public void testRemoveContextDropsSpecificInstance () {

    TestContext keep = new TestContext("keep");
    TestContext drop = new TestContext("drop");

    ContextFactory.pushContext(keep);
    ContextFactory.pushContext(drop);

    Assert.assertSame(ContextFactory.removeContext(drop), drop);
    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 1);
    Assert.assertSame(ContextFactory.getContext(TestContext.class), keep);
  }

  public void testSizeForReturnsZeroForEmptyStack () {

    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 0);
  }

  public void testExportContextTraceProducesBottomToTopOrder () {

    ContextFactory.pushContext(new TestContext("first"));
    ContextFactory.pushContext(new TestContext("second"));
    ContextFactory.pushContext(new TestContext("third"));

    TestContext[] exported = ContextFactory.exportContextTrace(TestContext.class);

    Assert.assertEquals(exported.length, 3);
    Assert.assertEquals(exported[0].getValue(), "first");
    Assert.assertEquals(exported[2].getValue(), "third");
    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 0);
  }

  public void testImportContextTraceRestoresStackTopMostLast () {

    TestContext first = new TestContext("first");
    TestContext second = new TestContext("second");
    TestContext third = new TestContext("third");

    ContextFactory.importContextTrace(TestContext.class, first, second, third);

    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 3);
    Assert.assertSame(ContextFactory.getContext(TestContext.class), third);
  }

  @ExpectedContexts({TestContext.class})
  public void annotatedTarget () {

  }

  public void testFilterContextsOnAcceptsWhenExpectationIsSatisfied ()
    throws NoSuchMethodException {

    Method method = ContextFactoryTest.class.getMethod("annotatedTarget");

    ContextFactory.pushContext(new TestContext("present"));

    Context[] contexts = ContextFactory.filterContextsOn(method);

    Assert.assertEquals(contexts.length, 1);
    Assert.assertTrue(contexts[0] instanceof TestContext);
  }

  @Test(expectedExceptions = ContextException.class)
  public void testFilterContextsOnRejectsWhenExpectationIsMissing ()
    throws NoSuchMethodException {

    Method method = ContextFactoryTest.class.getMethod("annotatedTarget");

    ContextFactory.filterContextsOn(method);
  }

  public void testChildThreadInheritsSnapshotOfParentStack ()
    throws InterruptedException {

    ContextFactory.pushContext(new TestContext("parent-value"));

    AtomicReference<String> seen = new AtomicReference<>();
    AtomicReference<Integer> parentSize = new AtomicReference<>();
    Thread child = new Thread(() -> {
      seen.set(ContextFactory.getContext(TestContext.class).getValue());
      ContextFactory.pushContext(new TestContext("child-only"));
      parentSize.set(ContextFactory.sizeFor(TestContext.class));
    });

    child.start();
    child.join();

    Assert.assertEquals(seen.get(), "parent-value");
    Assert.assertEquals(parentSize.get().intValue(), 2);
    Assert.assertEquals(ContextFactory.sizeFor(TestContext.class), 1);
  }
}
