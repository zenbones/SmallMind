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

import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link OrmDaoContextManager} registration and lookup. The registry is backed by
 * {@link PerApplicationContext}, so each test attaches a fresh per-application context to the thread
 * first (matching {@code ByKeyRosterTest}). Covers simple-name versus fully-qualified-name resolution
 * in {@link OrmDaoContextManager#findDurableClass}, the absent-name {@code null} result, and both
 * {@link OrmDaoContextManager#get(String)} and {@link OrmDaoContextManager#get(Class)} resolution paths.
 */
@Test(groups = "unit")
public class OrmDaoContextManagerTest {

  @BeforeMethod
  public void attachApplicationContext () {

    // OrmDaoContextManager reads PerApplicationContext, which throws unless a context map is bound to this
    // thread. The thread-local map is reused across calls (and shared with other test classes on the
    // same thread), so install a fresh, empty registry per method to keep the absent-lookup cases honest.
    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(OrmDaoContextManager.class, new ConcurrentHashMap<>());
  }

  @SuppressWarnings("unchecked")
  public void testFindDurableClassMatchesBySimpleName () {

    OrmDaoContextManager.register(Widget.class, Mockito.mock(ORMDao.class));

    Assert.assertEquals(OrmDaoContextManager.findDurableClass("Widget"), Widget.class);
  }

  @SuppressWarnings("unchecked")
  public void testFindDurableClassMatchesByFullyQualifiedName () {

    OrmDaoContextManager.register(Widget.class, Mockito.mock(ORMDao.class));

    Assert.assertEquals(OrmDaoContextManager.findDurableClass(Widget.class.getName()), Widget.class);
  }

  @SuppressWarnings("unchecked")
  public void testFindDurableClassReturnsNullWhenNoNameMatches () {

    OrmDaoContextManager.register(Widget.class, Mockito.mock(ORMDao.class));

    Assert.assertNull(OrmDaoContextManager.findDurableClass("Gadget"));
    Assert.assertNull(OrmDaoContextManager.findDurableClass("org.smallmind.persistence.orm.OrmDaoContextManagerTest$Gadget"));
  }

  @SuppressWarnings("unchecked")
  public void testFindDurableClassDistinguishesAmongMultipleRegistrations () {

    OrmDaoContextManager.register(Widget.class, Mockito.mock(ORMDao.class));
    OrmDaoContextManager.register(Gadget.class, Mockito.mock(ORMDao.class));

    Assert.assertEquals(OrmDaoContextManager.findDurableClass("Widget"), Widget.class);
    Assert.assertEquals(OrmDaoContextManager.findDurableClass("Gadget"), Gadget.class);
  }

  @SuppressWarnings("unchecked")
  public void testGetByNameResolvesRegisteredDao () {

    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    OrmDaoContextManager.register(Widget.class, widgetDao);

    Assert.assertSame(OrmDaoContextManager.get("Widget"), widgetDao);
    Assert.assertSame(OrmDaoContextManager.get(Widget.class.getName()), widgetDao);
  }

  @SuppressWarnings("unchecked")
  public void testGetByNameReturnsNullWhenAbsent () {

    OrmDaoContextManager.register(Widget.class, Mockito.mock(ORMDao.class));

    Assert.assertNull(OrmDaoContextManager.get("Gadget"));
  }

  @SuppressWarnings("unchecked")
  public void testGetByClassResolvesRegisteredDao () {

    ORMDao<Long, Widget, ?, ?> widgetDao = Mockito.mock(ORMDao.class);

    OrmDaoContextManager.register(Widget.class, widgetDao);

    Assert.assertSame(OrmDaoContextManager.get(Widget.class), widgetDao);
  }

  @SuppressWarnings("unchecked")
  public void testGetByClassReturnsNullWhenAbsent () {

    OrmDaoContextManager.register(Widget.class, Mockito.mock(ORMDao.class));

    Assert.assertNull(OrmDaoContextManager.get(Gadget.class));
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
