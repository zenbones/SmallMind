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
import java.util.Comparator;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;
import org.smallmind.persistence.cache.praxis.intrinsic.ByReferenceIntrinsicVector;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.aop.Timed;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link CacheAsAspect}, exercised through real AspectJ weaving. A minimal {@link ORMDao} subclass
 * declares {@link CacheAs}-annotated methods with a variety of (valid and invalid) configurations; invoking
 * them drives the around-advice. The misconfiguration branches throw {@link CacheAutomationError} before any
 * cache access, so they need no backing cache; the read-through path uses a Mockito {@link VectoredDao} mock
 * and a real {@link ByReferenceSingularVector} to verify both the cache-hit (served from the vector) and
 * cache-miss (method invoked, result stored as a singular vector) behaviours.
 */
@Test(groups = "unit")
public class CacheAsAspectTest {

  @SuppressWarnings("unchecked")
  private CacheAsWidgetDao dao (VectoredDao<Long, Widget> vectoredDao) {

    ProxySession<Object, Object> proxySession = Mockito.mock(ProxySession.class);

    Mockito.when(proxySession.isCacheEnabled()).thenReturn(true);

    return new CacheAsWidgetDao(proxySession, vectoredDao);
  }

  @SuppressWarnings("unchecked")
  private VectoredDao<Long, Widget> vectoredDao () {

    return Mockito.mock(VectoredDao.class);
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testNegativeBaseTimeIsRejected () {

    dao(vectoredDao()).negativeTime();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testNegativeStochasticIsRejected () {

    dao(vectoredDao()).negativeStochastic();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testOrderedOnSingleReturnIsRejected () {

    dao(vectoredDao()).orderedSingle();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testMaxOnSingleReturnIsRejected () {

    dao(vectoredDao()).maxSingle();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testComparatorOnSingleReturnIsRejected () {

    dao(vectoredDao()).comparatorSingle();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testComparatorWithoutOrderedOnIterableIsRejected () {

    dao(vectoredDao()).comparatorUnorderedIterable();
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testIllegalReturnTypeIsRejected () {

    dao(vectoredDao()).illegalReturn();
  }

  public void testSingleCacheMissInvokesMethodAndStoresSingularVector () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    Widget loaded = new Widget(1L);
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, loaded);

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(null);
    Mockito.when(vectoredDao.createSingularVector(Mockito.any(), Mockito.eq(loaded), Mockito.anyInt())).thenReturn(new ByReferenceSingularVector<>(loaded, 0));
    Mockito.when(vectoredDao.persistVector(Mockito.any(), Mockito.any())).thenAnswer(invocation -> invocation.getArgument(1));

    Widget result = dao.loadActive();

    Assert.assertSame(result, loaded, "a cache miss should invoke the method and return its durable through the stored vector");
    Assert.assertTrue(dao.wasInvoked(), "the underlying method should have been invoked on a cache miss");
    Mockito.verify(vectoredDao).persistVector(Mockito.any(), Mockito.any());
  }

  public void testSingleCacheHitServesFromVectorWithoutInvokingMethod () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    Widget cached = new Widget(2L);
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, new Widget(99L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(new ByReferenceSingularVector<>(cached, 0));

    Widget result = dao.loadActive();

    Assert.assertSame(result, cached, "a live cache hit should be served from the vector");
    Assert.assertFalse(dao.wasInvoked(), "the underlying method must not run on a cache hit");
    Mockito.verify(vectoredDao, Mockito.never()).persistVector(Mockito.any(), Mockito.any());
  }

  private List<Widget> widgets (long... ids) {

    List<Widget> list = new ArrayList<>();

    for (long id : ids) {
      list.add(new Widget(id));
    }

    return list;
  }

  private DurableVector<Long, Widget> vector (long... ids) {

    IntrinsicRoster<Widget> roster = new IntrinsicRoster<>();

    for (long id : ids) {
      roster.add(new Widget(id));
    }

    return new ByReferenceIntrinsicVector<>(roster, null, 0, 0, false);
  }

  @SuppressWarnings("unchecked")
  private void stubCreateAndPersist (VectoredDao<Long, Widget> vectoredDao, DurableVector<Long, Widget> stored) {

    Mockito.when(vectoredDao.createVector(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(stored);
    Mockito.when(vectoredDao.persistVector(Mockito.any(), Mockito.any())).thenAnswer(invocation -> invocation.getArgument(1));
  }

  public void testListCacheMissInvokesMethodAndStoresVector () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, widgets(1L, 2L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(null);
    stubCreateAndPersist(vectoredDao, vector(1L, 2L));

    List<Widget> result = dao.loadActiveList();

    Assert.assertEquals(result.size(), 2, "a cache miss should store and return the freshly fetched list");
    Assert.assertTrue(dao.wasInvoked(), "the underlying method should run on a cache miss");
    Mockito.verify(vectoredDao).createVector(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());
    Mockito.verify(vectoredDao).persistVector(Mockito.any(), Mockito.any());
  }

  public void testIterableCacheMissInvokesMethodAndStoresVector () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, widgets(1L, 2L, 3L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(null);
    stubCreateAndPersist(vectoredDao, vector(1L, 2L, 3L));

    int count = 0;

    for (Widget ignored : dao.loadActiveIterable()) {
      count++;
    }

    Assert.assertEquals(count, 3, "a non-List Iterable cache miss should store and return the lazy list");
    Assert.assertTrue(dao.wasInvoked());
    Mockito.verify(vectoredDao).persistVector(Mockito.any(), Mockito.any());
  }

  public void testListCacheHitServesPreFetchedListWithoutInvokingMethod () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, widgets(1L, 2L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(vector(5L));

    List<Widget> result = dao.loadActiveList();

    Assert.assertEquals(result.size(), 1, "a live cache hit should be served from the vector, not the method");
    Assert.assertEquals(result.get(0).getId(), Long.valueOf(5L));
    Assert.assertFalse(dao.wasInvoked(), "the underlying method must not run on a cache hit");
    Mockito.verify(vectoredDao, Mockito.never()).persistVector(Mockito.any(), Mockito.any());
  }

  public void testIterableCacheHitServesLazyListWithoutInvokingMethod () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, widgets(1L, 2L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(vector(5L));

    int count = 0;

    for (Widget ignored : dao.loadActiveIterable()) {
      count++;
    }

    Assert.assertEquals(count, 1);
    Assert.assertFalse(dao.wasInvoked());
    Mockito.verify(vectoredDao, Mockito.never()).persistVector(Mockito.any(), Mockito.any());
  }

  public void testDeadIterableVectorIsDeletedThenRefetched () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, widgets(1L, 2L));
    DurableVector<Long, Widget> dead = deadVector();

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(dead);
    stubCreateAndPersist(vectoredDao, vector(1L, 2L));

    List<Widget> result = dao.loadActiveList();

    Mockito.verify(vectoredDao).deleteVector(Mockito.any());
    Assert.assertTrue(dao.wasInvoked(), "an expired vector should be deleted and the method re-run");
    Assert.assertEquals(result.size(), 2);
    Mockito.verify(vectoredDao).persistVector(Mockito.any(), Mockito.any());
  }

  public void testOrderedIterablePassesComparatorToCreateVector () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, widgets(2L, 1L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(null);
    stubCreateAndPersist(vectoredDao, vector(1L, 2L));

    dao.loadOrderedList();

    ArgumentCaptor<Comparator<Widget>> comparatorCaptor = ArgumentCaptor.forClass(Comparator.class);

    Mockito.verify(vectoredDao).createVector(Mockito.any(), Mockito.any(), comparatorCaptor.capture(), Mockito.anyInt(), Mockito.anyInt(), Mockito.eq(true));
    Assert.assertTrue(comparatorCaptor.getValue() instanceof WidgetComparator, "the declared comparator should be instantiated and passed to createVector for an ordered method");
  }

  public void testNullIterableResultReturnsNull () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null, null);

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(null);

    Assert.assertNull(dao.loadNullList(), "a null method result should yield a null cache result");
    Mockito.verify(vectoredDao, Mockito.never()).persistVector(Mockito.any(), Mockito.any());
  }

  public void testIterableWithNoVectoredDaoProceedsToMethod () {

    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), null, null, widgets(1L, 2L));

    List<Widget> result = dao.loadActiveList();

    Assert.assertEquals(result.size(), 2, "with no vectored DAO the method result should pass straight through");
    Assert.assertTrue(dao.wasInvoked());
  }

  public void testSingleWithNoVectoredDaoProceedsToMethod () {

    Widget loaded = new Widget(1L);
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), null, loaded);

    Assert.assertSame(dao.loadActive(), loaded, "with no vectored DAO the durable should pass straight through");
    Assert.assertTrue(dao.wasInvoked());
  }

  public void testDeadSingleVectorIsDeletedThenRefetched () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    Widget loaded = new Widget(1L);
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, loaded);
    DurableVector<Long, Widget> dead = deadVector();

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(dead);
    Mockito.when(vectoredDao.createSingularVector(Mockito.any(), Mockito.eq(loaded), Mockito.anyInt())).thenReturn(new ByReferenceSingularVector<>(loaded, 0));
    Mockito.when(vectoredDao.persistVector(Mockito.any(), Mockito.any())).thenAnswer(invocation -> invocation.getArgument(1));

    Widget result = dao.loadActive();

    Mockito.verify(vectoredDao).deleteVector(Mockito.any());
    Assert.assertSame(result, loaded, "an expired singular vector should be deleted and the method re-run");
    Assert.assertTrue(dao.wasInvoked());
    Mockito.verify(vectoredDao).persistVector(Mockito.any(), Mockito.any());
  }

  public void testNullSingleResultReturnsNull () {

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    CacheAsWidgetDao dao = new CacheAsWidgetDao(proxySession(), vectoredDao, null);

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(null);

    Assert.assertNull(dao.loadActive(), "a null durable result should yield a null cache result");
    Assert.assertTrue(dao.wasInvoked());
    Mockito.verify(vectoredDao, Mockito.never()).persistVector(Mockito.any(), Mockito.any());
  }

  public void testTimedDaoServesSingleCacheHitWithoutInvokingMethod () {

    // An empty per-application context lets Instrument.with(...) resolve to the no-op instrumentation instead of throwing.
    new PerApplicationContext();

    VectoredDao<Long, Widget> vectoredDao = vectoredDao();
    Widget cached = new Widget(2L);
    TimedCacheAsWidgetDao dao = new TimedCacheAsWidgetDao(proxySession(), vectoredDao, new Widget(99L));

    Mockito.when(vectoredDao.getVector(Mockito.any())).thenReturn(new ByReferenceSingularVector<>(cached, 0));
    // The @Timed finally-block tags the metric with the cache-hit metric source, which must be non-empty.
    Mockito.when(vectoredDao.getMetricSource()).thenReturn("test");

    Assert.assertSame(dao.loadActive(), cached, "timing must not change the cache-hit result");
    Assert.assertFalse(dao.wasInvoked());
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testTimedDaoDisablesTimingWhenAdviceThrows () {

    new TimedCacheAsWidgetDao(proxySession(), vectoredDao(), null).illegalReturn();
  }

  @SuppressWarnings("unchecked")
  private DurableVector<Long, Widget> deadVector () {

    DurableVector<Long, Widget> dead = Mockito.mock(DurableVector.class);

    Mockito.when(dead.isAlive()).thenReturn(false);

    return dead;
  }

  @SuppressWarnings("unchecked")
  private ProxySession<Object, Object> proxySession () {

    ProxySession<Object, Object> proxySession = Mockito.mock(ProxySession.class);

    Mockito.when(proxySession.isCacheEnabled()).thenReturn(true);

    return proxySession;
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

  public static class WidgetComparator implements Comparator<Widget> {

    @Override
    public int compare (Widget first, Widget second) {

      return first.getId().compareTo(second.getId());
    }
  }

  private static class CacheAsWidgetDao extends ORMDao<Long, Widget, Object, Object> {

    private final Widget loaded;
    private final List<Widget> loadedList;
    private boolean invoked;

    private CacheAsWidgetDao (ProxySession<Object, Object> proxySession, VectoredDao<Long, Widget> vectoredDao) {

      this(proxySession, vectoredDao, null, null);
    }

    private CacheAsWidgetDao (ProxySession<Object, Object> proxySession, VectoredDao<Long, Widget> vectoredDao, Widget loaded) {

      this(proxySession, vectoredDao, loaded, null);
    }

    private CacheAsWidgetDao (ProxySession<Object, Object> proxySession, VectoredDao<Long, Widget> vectoredDao, Widget loaded, List<Widget> loadedList) {

      super(proxySession, vectoredDao);

      this.loaded = loaded;
      this.loadedList = loadedList;
    }

    private boolean wasInvoked () {

      return invoked;
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

    @CacheAs(value = @Vector(namespace = "active", value = {@Key(value = "ACTIVE", constant = true)}))
    public Widget loadActive () {

      invoked = true;

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "activeList", value = {@Key(value = "ACTIVE", constant = true)}))
    public List<Widget> loadActiveList () {

      invoked = true;

      return loadedList;
    }

    @CacheAs(value = @Vector(namespace = "activeIterable", value = {@Key(value = "ACTIVE", constant = true)}))
    public Iterable<Widget> loadActiveIterable () {

      invoked = true;

      return loadedList;
    }

    @CacheAs(value = @Vector(namespace = "ordered", value = {@Key(value = "ACTIVE", constant = true)}), comparator = WidgetComparator.class, ordered = true)
    public List<Widget> loadOrderedList () {

      invoked = true;

      return loadedList;
    }

    @CacheAs(value = @Vector(namespace = "nullList", value = {@Key(value = "ACTIVE", constant = true)}))
    public List<Widget> loadNullList () {

      invoked = true;

      return loadedList;
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}), time = @Time(value = -1))
    public Widget negativeTime () {

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}), time = @Time(value = 0, stochastic = -1))
    public Widget negativeStochastic () {

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}), ordered = true)
    public Widget orderedSingle () {

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}), max = 5)
    public Widget maxSingle () {

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}), comparator = WidgetComparator.class)
    public Widget comparatorSingle () {

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}), comparator = WidgetComparator.class, ordered = false)
    public Iterable<Widget> comparatorUnorderedIterable () {

      return new ArrayList<>();
    }

    @CacheAs(value = @Vector(namespace = "n", value = {@Key(value = "ACTIVE", constant = true)}))
    public String illegalReturn () {

      return "not-a-durable";
    }
  }

  @Timed
  private static class TimedCacheAsWidgetDao extends ORMDao<Long, Widget, Object, Object> {

    private final Widget loaded;
    private boolean invoked;

    private TimedCacheAsWidgetDao (ProxySession<Object, Object> proxySession, VectoredDao<Long, Widget> vectoredDao, Widget loaded) {

      super(proxySession, vectoredDao);

      this.loaded = loaded;
    }

    private boolean wasInvoked () {

      return invoked;
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

    @CacheAs(value = @Vector(namespace = "timedActive", value = {@Key(value = "ACTIVE", constant = true)}))
    public Widget loadActive () {

      invoked = true;

      return loaded;
    }

    @CacheAs(value = @Vector(namespace = "timedN", value = {@Key(value = "ACTIVE", constant = true)}))
    public String illegalReturn () {

      return "not-a-durable";
    }
  }
}
