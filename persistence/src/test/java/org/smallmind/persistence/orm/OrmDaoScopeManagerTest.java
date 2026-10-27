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

import org.mockito.Mockito;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.AbstractDurable;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link OrmDaoScopeManager} registration and lookup inside a {@link PerApplicationScope}. Each test owns
 * a fresh scope, so registrations never leak across methods, and an unbound thread fails fast.
 */
@Test(groups = "unit")
public class OrmDaoScopeManagerTest {

  @SuppressWarnings("unchecked")
  public void testFindDurableClassMatchesBySimpleAndFullyQualifiedName () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> {
      OrmDaoScopeManager.register(Widget.class, Mockito.mock(ORMDao.class));

      Assert.assertEquals(OrmDaoScopeManager.findDurableClass("Widget"), Widget.class);
      Assert.assertEquals(OrmDaoScopeManager.findDurableClass(Widget.class.getName()), Widget.class);
      Assert.assertNull(OrmDaoScopeManager.findDurableClass("Gadget"));
    });
  }

  @SuppressWarnings("unchecked")
  public void testGetByNameAndByClassResolveTheRegisteredDao () {

    PerApplicationScope scope = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    scope.run(() -> {
      OrmDaoScopeManager.register(Widget.class, widgetDao);
      OrmDaoScopeManager.register(Gadget.class, Mockito.mock(ORMDao.class));

      Assert.assertSame(OrmDaoScopeManager.get("Widget"), widgetDao);
      Assert.assertSame(OrmDaoScopeManager.get(Widget.class.getName()), widgetDao);
      Assert.assertSame(OrmDaoScopeManager.get(Widget.class), widgetDao);
      Assert.assertNull(OrmDaoScopeManager.get("Sprocket"));
    });
  }

  @SuppressWarnings("unchecked")
  public void testRegistrationIsVisibleInLaterEntriesOfTheSameScopeOnly () {

    PerApplicationScope first = new PerApplicationScope();
    PerApplicationScope second = new PerApplicationScope();
    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    first.run(() -> OrmDaoScopeManager.register(Widget.class, widgetDao));

    first.run(() -> Assert.assertSame(OrmDaoScopeManager.get(Widget.class), widgetDao));
    second.run(() -> Assert.assertNull(PerApplicationScope.getPerApplicationData(OrmDaoScopeManager.class, Object.class)));
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testGetWithoutScopeThrows () {

    OrmDaoScopeManager.get(Widget.class);
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
