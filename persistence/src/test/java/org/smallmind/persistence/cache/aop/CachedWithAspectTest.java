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
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.ProxySession;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link CachedWithAspect}, exercised through real AspectJ weaving. Each {@link CachedWith}-annotated
 * DAO is a minimal {@link ORMDao} subclass; invoking its {@code persist}/{@code delete} drives the around-advice,
 * which resolves the declared {@code filter}/{@code onPersist}/{@code finder}/{@code proxy} hook methods and then
 * issues the corresponding vector operation against a Mockito {@link VectoredDao} mock. Coverage spans inserts,
 * removals, invalidations, filter gating, the {@code OnPersist.REMOVE} strategy, a single-durable finder method,
 * a parameterized {@code Iterable} finder method (and its malformed-parameterization rejection), a proxy transform,
 * and the cache-disabled short circuit.
 */
@Test(groups = "unit")
public class CachedWithAspectTest {

  @SuppressWarnings("unchecked")
  private ProxySession<Object, Object> session (boolean cacheEnabled) {

    ProxySession<Object, Object> proxySession = Mockito.mock(ProxySession.class);

    Mockito.when(proxySession.isCacheEnabled()).thenReturn(cacheEnabled);

    return proxySession;
  }

  @SuppressWarnings("rawtypes")
  private VectoredDao vectoredDao () {

    return Mockito.mock(VectoredDao.class);
  }

  public void testPersistInsertsDurableIntoVector () {

    VectoredDao vectoredDao = vectoredDao();
    Widget widget = new Widget(1L);

    new UpdateInsertDao(session(true), vectoredDao).persist(widget);

    Mockito.verify(vectoredDao).updateInVector(Mockito.any(), Mockito.eq(widget));
  }

  public void testDeleteRemovesDurableFromVector () {

    VectoredDao vectoredDao = vectoredDao();
    Widget widget = new Widget(1L);

    new UpdateInsertDao(session(true), vectoredDao).delete(widget);

    Mockito.verify(vectoredDao).removeFromVector(Mockito.any(), Mockito.eq(widget));
  }

  public void testPersistInvalidatesVector () {

    VectoredDao vectoredDao = vectoredDao();

    new InvalidateDao(session(true), vectoredDao).persist(new Widget(1L));

    Mockito.verify(vectoredDao).deleteVector(Mockito.any());
  }

  public void testDeleteInvalidatesVector () {

    VectoredDao vectoredDao = vectoredDao();

    new InvalidateDao(session(true), vectoredDao).delete(new Widget(1L));

    Mockito.verify(vectoredDao).deleteVector(Mockito.any());
  }

  public void testFilterReturningFalseSkipsTheUpdate () {

    VectoredDao vectoredDao = vectoredDao();

    new FilterSkipDao(session(true), vectoredDao).persist(new Widget(1L));

    Mockito.verify(vectoredDao, Mockito.never()).updateInVector(Mockito.any(), Mockito.any());
  }

  public void testOnPersistRemoveStrategyRemovesFromVector () {

    VectoredDao vectoredDao = vectoredDao();
    Widget widget = new Widget(1L);

    new FilterPassRemoveDao(session(true), vectoredDao).persist(widget);

    Mockito.verify(vectoredDao).removeFromVector(Mockito.any(), Mockito.eq(widget));
    Mockito.verify(vectoredDao, Mockito.never()).updateInVector(Mockito.any(), Mockito.any());
  }

  public void testSingleDurableFinderMethodDrivesTheUpdate () {

    VectoredDao vectoredDao = vectoredDao();

    new SingleFinderDao(session(true), vectoredDao).persist(new Widget(1L));

    // The finder method returns the durable with id + 1, which becomes the indexed durable.
    Mockito.verify(vectoredDao).updateInVector(Mockito.any(), Mockito.eq(new Widget(2L)));
  }

  public void testIterableFinderMethodDrivesAnUpdatePerElement () {

    VectoredDao vectoredDao = vectoredDao();
    Widget widget = new Widget(1L);

    new MultiFinderDao(session(true), vectoredDao).persist(widget);

    // The finder returns a List<Widget> of [widget, widget + 100]; each element drives its own update.
    Mockito.verify(vectoredDao).updateInVector(Mockito.any(), Mockito.eq(widget));
    Mockito.verify(vectoredDao).updateInVector(Mockito.any(), Mockito.eq(new Widget(101L)));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testMalformedIterableFinderIsRejected () {

    // The finder returns List<Object>, which is not parameterized to the managed durable type.
    new MalformedFinderDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  public void testProxyTransformIsAppliedDuringKeyConstruction () {

    VectoredDao vectoredDao = vectoredDao();
    Widget widget = new Widget(1L);

    new ProxyDao(session(true), vectoredDao).persist(widget);

    // The proxy transforms the durable for key construction only; the indexed durable stays the original.
    Mockito.verify(vectoredDao).updateInVector(Mockito.any(), Mockito.eq(widget));
  }

  public void testCacheDisabledSkipsAllVectorOperations () {

    VectoredDao vectoredDao = vectoredDao();
    Widget widget = new Widget(1L);

    Widget result = new UpdateInsertDao(session(false), vectoredDao).persist(widget);

    Assert.assertSame(result, widget, "with caching disabled the durable should pass straight through");
    Mockito.verifyNoInteractions(vectoredDao);
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testNonBooleanFilterIsRejected () {

    new NonBooleanFilterDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testMissingFilterMethodIsRejected () {

    new MissingFilterDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testNonOnPersistReturnTypeIsRejected () {

    new BadOnPersistDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testMissingOnPersistMethodIsRejected () {

    new MissingOnPersistDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testMissingFinderMethodIsRejected () {

    new MissingFinderDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testMissingProxyMethodIsRejected () {

    new MissingProxyDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  @Test(groups = "unit", expectedExceptions = CacheAutomationError.class)
  public void testNonAssignableProxyReturnTypeIsRejected () {

    new BadProxyDao(session(true), vectoredDao()).persist(new Widget(1L));
  }

  public void testDeleteWithNullDurableSkipsVectorOperations () {

    VectoredDao vectoredDao = vectoredDao();

    new UpdateInsertDao(session(true), vectoredDao).delete(null);

    Mockito.verifyNoInteractions(vectoredDao);
  }

  public void testRepeatedPersistReusesTheResolvedFilterMethod () {

    VectoredDao vectoredDao = vectoredDao();
    FilterPassRemoveDao dao = new FilterPassRemoveDao(session(true), vectoredDao);

    // Two persists resolve the same filter/onPersist methods; the second drives the cached MethodKey lookup path.
    dao.persist(new Widget(1L));
    dao.persist(new Widget(2L));

    Mockito.verify(vectoredDao, Mockito.times(2)).removeFromVector(Mockito.any(), Mockito.any());
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

  private abstract static class AbstractWidgetCacheDao extends ORMDao<Long, Widget, Object, Object> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AbstractWidgetCacheDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

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

    public boolean includeAll (Widget widget) {

      return true;
    }

    public boolean excludeAll (Widget widget) {

      return false;
    }

    public OnPersist removeAction (Widget widget) {

      return OnPersist.REMOVE;
    }

    public Widget plusOne (Widget widget) {

      return new Widget(widget.getId() + 1);
    }

    public Widget single (Widget widget) {

      return new Widget(widget.getId() + 1);
    }

    public List<Widget> pair (Widget widget) {

      List<Widget> found = new ArrayList<>();

      found.add(widget);
      found.add(new Widget(widget.getId() + 100));

      return found;
    }

    public List<Object> malformedFinder (Widget widget) {

      return new ArrayList<>();
    }

    public String notBooleanFilter (Widget widget) {

      return "not-a-boolean";
    }

    public String badOnPersist (Widget widget) {

      return "not-an-onpersist";
    }

    public Object badProxy (Widget widget) {

      return new Object();
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}))})
  private static class UpdateInsertDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private UpdateInsertDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(invalidates = {@Invalidate(value = @Vector(namespace = "v", value = {@Key(value = "id")}))})
  private static class InvalidateDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private InvalidateDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), filter = "excludeAll")})
  private static class FilterSkipDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private FilterSkipDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), filter = "includeAll", onPersist = "removeAction")})
  private static class FilterPassRemoveDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private FilterPassRemoveDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), finder = @Finder(with = Widget.class, method = "single"))})
  private static class SingleFinderDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private SingleFinderDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), finder = @Finder(with = Widget.class, method = "pair"))})
  private static class MultiFinderDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private MultiFinderDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), finder = @Finder(with = Widget.class, method = "malformedFinder"))})
  private static class MalformedFinderDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private MalformedFinderDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), proxy = @Proxy(with = Widget.class, method = "plusOne"))})
  private static class ProxyDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private ProxyDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), filter = "notBooleanFilter")})
  private static class NonBooleanFilterDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private NonBooleanFilterDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), filter = "missingFilter")})
  private static class MissingFilterDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private MissingFilterDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), onPersist = "badOnPersist")})
  private static class BadOnPersistDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private BadOnPersistDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), onPersist = "missingOnPersist")})
  private static class MissingOnPersistDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private MissingOnPersistDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), finder = @Finder(with = Widget.class, method = "missingFinder"))})
  private static class MissingFinderDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private MissingFinderDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), proxy = @Proxy(with = Widget.class, method = "missingProxy"))})
  private static class MissingProxyDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private MissingProxyDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }

  @CachedWith(updates = {@Update(value = @Vector(namespace = "v", value = {@Key(value = "id")}), proxy = @Proxy(method = "badProxy"))})
  private static class BadProxyDao extends AbstractWidgetCacheDao {

    @SuppressWarnings("rawtypes")
    private BadProxyDao (ProxySession<Object, Object> proxySession, VectoredDao vectoredDao) {

      super(proxySession, vectoredDao);
    }

    @Override
    public Widget persist (Widget durable) {

      return super.persist(durable);
    }

    @Override
    public void delete (Widget durable) {

      super.delete(durable);
    }
  }
}
