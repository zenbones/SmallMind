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
package org.smallmind.persistence.orm;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.AbstractDurable;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the bridge semantics of {@link OrmDaoManager}: registration lands in every bound registry, lookups read
 * the scope first and fall back to the context, a scope-only thread never touches the context, and a thread with
 * neither binding fails through the context flavor.
 */
@Test(groups = "unit")
public class OrmDaoManagerTest {

  @BeforeMethod
  public void attachApplicationContext () {

    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(OrmDaoContextManager.class, new ConcurrentHashMap<>());
  }

  @SuppressWarnings("unchecked")
  public void testRegisterWithBothBoundWritesBothRegistries () {

    PerApplicationScope scope = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    scope.run(() -> OrmDaoManager.register(Widget.class, widgetDao));

    Assert.assertSame(scope.<ORMDao, RuntimeException>call(() -> OrmDaoScopeManager.get(Widget.class)), widgetDao);
    Assert.assertSame(OrmDaoContextManager.get(Widget.class), widgetDao);
    Assert.assertSame(OrmDaoManager.get(Widget.class), widgetDao, "visible from a thread carrying only the context");
    Assert.assertSame(scope.<ORMDao, RuntimeException>call(() -> OrmDaoManager.get(Widget.class)), widgetDao, "visible from a thread carrying the scope");
  }

  @SuppressWarnings("unchecked")
  public void testRegisterWithOnlyContextBoundWritesOnlyTheContext () {

    PerApplicationScope scope = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    OrmDaoManager.register(Widget.class, widgetDao);

    Assert.assertSame(OrmDaoContextManager.get(Widget.class), widgetDao);
    Assert.assertNull(scope.<ORMDao, RuntimeException>call(() -> OrmDaoScopeManager.get(Widget.class)));
  }

  @SuppressWarnings("unchecked")
  public void testLookupFallsBackToTheContextWhenTheScopeHoldsNothing () {

    PerApplicationScope scope = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    OrmDaoContextManager.register(Widget.class, widgetDao);

    Assert.assertSame(scope.<ORMDao, RuntimeException>call(() -> OrmDaoManager.get(Widget.class)), widgetDao);
    Assert.assertSame(scope.call(() -> OrmDaoManager.get("Widget")), widgetDao);
    Assert.assertEquals(scope.call(() -> OrmDaoManager.findDurableClass("Widget")), Widget.class);
  }

  @SuppressWarnings("unchecked")
  public void testLookupPrefersTheScopeWhenBothHoldAValue () {

    PerApplicationScope scope = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> scopedDao = Mockito.mock(ORMDao.class);
    ORMDao<Long, Widget, ?, ?> contextDao = Mockito.mock(ORMDao.class);

    scope.run(() -> OrmDaoScopeManager.register(Widget.class, scopedDao));
    OrmDaoContextManager.register(Widget.class, contextDao);

    Assert.assertSame(scope.<ORMDao, RuntimeException>call(() -> OrmDaoManager.get(Widget.class)), scopedDao);
    Assert.assertSame(OrmDaoManager.get(Widget.class), contextDao);
  }

  @SuppressWarnings("unchecked")
  public void testScopeOnlyThreadResolvesWithoutAContext ()
    throws Exception {

    PerApplicationScope scope = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    Assert.assertSame(onIsolatedThread(() -> scope.<ORMDao, RuntimeException>call(() -> {
      OrmDaoManager.register(Widget.class, widgetDao);

      return OrmDaoManager.get(Widget.class);
    })), widgetDao);
    Assert.assertNull(onIsolatedThread(() -> scope.<ORMDao, RuntimeException>call(() -> OrmDaoManager.get(Gadget.class))), "an unregistered class on a scope-only thread is null, not an exception");
  }

  @Test(expectedExceptions = MissingPerApplicationContextException.class)
  public void testUnboundThreadFailsThroughTheContextFlavor ()
    throws Exception {

    onIsolatedThread(() -> OrmDaoManager.get("Widget"));
  }

  // The test thread carries an inherited PerApplicationContext shared with every other test on it, so the scope-only
  // and unbound cases can only be observed on a thread that does not inherit it.
  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        result.set(body.call());
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }

    return result.get();
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  public static class Gadget extends AbstractDurable<Long, Gadget> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }
}
