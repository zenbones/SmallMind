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
package org.smallmind.persistence.cache.aop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.aop.Timed;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link CacheCoherentAspect}, exercised through real AspectJ weaving (the persistence module
 * weaves its test classes against the main aspects). A minimal {@link ORMDao} subclass declares
 * {@link CacheCoherent}-annotated methods returning a single durable, a {@code List}, an {@code Iterable},
 * and an illegal type; invoking them drives the around-advice. The {@link VectoredDao} delegate is a
 * Mockito mock, so the assertions verify that every returned durable is passed through
 * {@code persist(..., UpdateMode.SOFT)} when caching is enabled, and passed straight through when not.
 */
@Test(groups = "unit")
public class CacheCoherentAspectTest {

  @SuppressWarnings("unchecked")
  private CoherentWidgetDao dao (boolean cacheEnabled, VectoredDao<Long, Widget> vectoredDao) {

    ProxySession<Object, Object> proxySession = Mockito.mock(ProxySession.class);

    Mockito.when(proxySession.isCacheEnabled()).thenReturn(cacheEnabled);

    return new CoherentWidgetDao(proxySession, vectoredDao);
  }

  @SuppressWarnings("unchecked")
  private VectoredDao<Long, Widget> echoingVectoredDao () {

    VectoredDao<Long, Widget> vectoredDao = Mockito.mock(VectoredDao.class);

    Mockito.when(vectoredDao.persist(Mockito.eq(Widget.class), Mockito.any(Widget.class), Mockito.eq(UpdateMode.SOFT))).thenAnswer(invocation -> invocation.getArgument(1));

    return vectoredDao;
  }

  public void testSingleDurableReturnIsPersistedSoft () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    Widget widget = new Widget(1L);

    Widget result = dao(true, vectoredDao).findOne(widget);

    Assert.assertSame(result, widget);
    Mockito.verify(vectoredDao).persist(Widget.class, widget, UpdateMode.SOFT);
  }

  public void testNullSingleReturnSkipsCache () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();

    Assert.assertNull(dao(true, vectoredDao).findOne(null));
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testSingleReturnWithCacheDisabledPassesThroughWithoutCache () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    Widget widget = new Widget(2L);

    Assert.assertSame(dao(false, vectoredDao).findOne(widget), widget);
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testListReturnPersistsEachElementSoft () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    List<Widget> source = new ArrayList<>();
    source.add(new Widget(1L));
    source.add(new Widget(2L));

    List<Widget> result = dao(true, vectoredDao).findList(source);

    Assert.assertEquals(result.size(), 2);
    Mockito.verify(vectoredDao).persist(Widget.class, source.get(0), UpdateMode.SOFT);
    Mockito.verify(vectoredDao).persist(Widget.class, source.get(1), UpdateMode.SOFT);
  }

  public void testIterableReturnPersistsEachElementSoftWhenDrained () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    List<Widget> source = new ArrayList<>();
    source.add(new Widget(1L));
    source.add(new Widget(2L));

    Iterable<Widget> result = dao(true, vectoredDao).findIterable(source);

    int count = 0;
    for (Widget ignored : result) {
      count++;
    }

    Assert.assertEquals(count, 2);
    Mockito.verify(vectoredDao).persist(Widget.class, source.get(0), UpdateMode.SOFT);
    Mockito.verify(vectoredDao).persist(Widget.class, source.get(1), UpdateMode.SOFT);
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testIllegalReturnTypeRaisesCacheAutomationError () {

    dao(true, echoingVectoredDao()).findIllegal();
  }

  public void testListReturnWithCacheDisabledPassesThroughWithoutCache () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    List<Widget> source = new ArrayList<>();
    source.add(new Widget(1L));
    source.add(new Widget(2L));

    Assert.assertSame(dao(false, vectoredDao).findList(source), source);
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testIterableReturnWithCacheDisabledPassesThroughWithoutCache () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    List<Widget> source = new ArrayList<>();
    source.add(new Widget(1L));

    Assert.assertSame(dao(false, vectoredDao).findIterable(source), source);
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testNullListReturnSkipsCache () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();

    Assert.assertNull(dao(true, vectoredDao).findList(null));
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testNullIterableReturnSkipsCache () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();

    Assert.assertNull(dao(true, vectoredDao).findIterable(null));
    Mockito.verify(vectoredDao, Mockito.never()).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testListReturnPersistsNonNullElementsAndPreservesNulls () {

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    List<Widget> source = new ArrayList<>();
    Widget widget = new Widget(1L);
    source.add(widget);
    source.add(null);

    List<Widget> result = dao(true, vectoredDao).findList(source);

    Assert.assertEquals(result.size(), 2);
    Assert.assertSame(result.get(0), widget);
    Assert.assertNull(result.get(1));
    Mockito.verify(vectoredDao).persist(Widget.class, widget, UpdateMode.SOFT);
    Mockito.verify(vectoredDao, Mockito.times(1)).persist(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testRawListReturnTypeRaisesCacheAutomationError () {

    dao(true, echoingVectoredDao()).findRawList();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testRawIterableReturnTypeRaisesCacheAutomationError () {

    dao(true, echoingVectoredDao()).findRawIterable();
  }

  @SuppressWarnings("unchecked")
  private TimedCoherentWidgetDao timedDao (VectoredDao<Long, Widget> vectoredDao) {

    ProxySession<Object, Object> proxySession = Mockito.mock(ProxySession.class);

    Mockito.when(proxySession.isCacheEnabled()).thenReturn(true);
    // The DAO's metric source derives from the session's data source type; the @Timed path tags with it, so it must be non-empty.
    Mockito.when(proxySession.getDataSourceType()).thenReturn("test");

    return new TimedCoherentWidgetDao(proxySession, vectoredDao);
  }

  public void testTimedDaoStillPersistsSingleDurableSoft () {

    // An empty per-application context lets Instrument.with(...) resolve to the no-op instrumentation instead of throwing.
    new PerApplicationContext();

    VectoredDao<Long, Widget> vectoredDao = echoingVectoredDao();
    Widget widget = new Widget(7L);

    Widget result = timedDao(vectoredDao).findOne(widget);

    Assert.assertSame(result, widget);
    Mockito.verify(vectoredDao).persist(Widget.class, widget, UpdateMode.SOFT);
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testTimedDaoDisablesTimingWhenAdviceThrows () {

    timedDao(echoingVectoredDao()).findIllegal();
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private static final long serialVersionUID = 1L;

    private Long id;

    public Widget () {

    }

    public Widget (Long id) {

      this.id = id;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }

  private static class CoherentWidgetDao extends ORMDao<Long, Widget, Object, Object> {

    private CoherentWidgetDao (ProxySession<Object, Object> proxySession, VectoredDao<Long, Widget> vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget acquire (Class<Widget> durableClass, Long id) {

      return null;
    }

    @Override
    public Widget get (Class<Widget> durableClass, Long id) {

      return null;
    }

    @Override
    public Widget persist (Class<Widget> durableClass, Widget durable) {

      return durable;
    }

    @Override
    public void delete (Class<Widget> durableClass, Widget durable) {

    }

    @Override
    public Widget detach (Widget durable) {

      return durable;
    }

    @Override
    public Iterable<Widget> scroll () {

      return new ArrayList<>();
    }

    @Override
    public Iterable<Widget> scroll (int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public Iterable<Widget> scrollById (Long greaterThan, int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public long size () {

      return 0;
    }

    @Override
    public List<Widget> list () {

      return new ArrayList<>();
    }

    @Override
    public List<Widget> list (int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public List<Widget> list (Long greaterThan, int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public List<Widget> list (java.util.Collection<Long> idCollection) {

      return new ArrayList<>();
    }

    @CacheCoherent
    public Widget findOne (Widget widget) {

      return widget;
    }

    @CacheCoherent
    public List<Widget> findList (List<Widget> widgets) {

      return widgets;
    }

    @CacheCoherent
    public Iterable<Widget> findIterable (Iterable<Widget> widgets) {

      return widgets;
    }

    @CacheCoherent
    public String findIllegal () {

      return "not-a-durable";
    }

    @SuppressWarnings("rawtypes")
    @CacheCoherent
    public List findRawList () {

      return new ArrayList();
    }

    @SuppressWarnings("rawtypes")
    @CacheCoherent
    public Iterable findRawIterable () {

      return new ArrayList();
    }
  }

  @Timed
  private static class TimedCoherentWidgetDao extends ORMDao<Long, Widget, Object, Object> {

    private TimedCoherentWidgetDao (ProxySession<Object, Object> proxySession, VectoredDao<Long, Widget> vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget acquire (Class<Widget> durableClass, Long id) {

      return null;
    }

    @Override
    public Widget get (Class<Widget> durableClass, Long id) {

      return null;
    }

    @Override
    public Widget persist (Class<Widget> durableClass, Widget durable) {

      return durable;
    }

    @Override
    public void delete (Class<Widget> durableClass, Widget durable) {

    }

    @Override
    public Widget detach (Widget durable) {

      return durable;
    }

    @Override
    public Iterable<Widget> scroll () {

      return new ArrayList<>();
    }

    @Override
    public Iterable<Widget> scroll (int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public Iterable<Widget> scrollById (Long greaterThan, int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public long size () {

      return 0;
    }

    @Override
    public List<Widget> list () {

      return new ArrayList<>();
    }

    @Override
    public List<Widget> list (int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public List<Widget> list (Long greaterThan, int fetchSize) {

      return new ArrayList<>();
    }

    @Override
    public List<Widget> list (Collection<Long> idCollection) {

      return new ArrayList<>();
    }

    @CacheCoherent
    public Widget findOne (Widget widget) {

      return widget;
    }

    @CacheCoherent
    public String findIllegal () {

      return "not-a-durable";
    }
  }
}
