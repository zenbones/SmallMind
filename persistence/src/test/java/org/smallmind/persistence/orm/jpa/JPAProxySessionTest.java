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
package org.smallmind.persistence.orm.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JPAProxySession}. The session is built over a Mockito {@link EntityManagerFactory} that
 * hands back a Mockito {@link EntityManager}, with {@code boundaryEnforced = false} so the lazy entity-manager
 * creation path runs without engaging the AOP {@code @Transactional}/{@code @NonTransactional} boundary stack.
 * The tests cover the lazy, thread-local {@link EntityManager} creation, the closed-state check, the lookup of
 * the current thread-bound transaction, the {@code clear()} delegation, and the native factory accessor. No
 * database is involved.
 */
@Test(groups = "unit")
public class JPAProxySessionTest {

  private EntityManagerFactory entityManagerFactory;
  private EntityManager entityManager;

  @BeforeMethod
  public void setUp () {

    entityManagerFactory = Mockito.mock(EntityManagerFactory.class);
    entityManager = Mockito.mock(EntityManager.class);

    Mockito.when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    Mockito.when(entityManager.isOpen()).thenReturn(true);
  }

  private JPAProxySession session () {

    return new JPAProxySession("test", null, entityManagerFactory, false, false);
  }

  public void testGetEntityManagerCreatesLazilyAndCachesPerThread () {

    JPAProxySession session = session();

    EntityManager first = session.getEntityManager();

    Assert.assertSame(first, entityManager, "the session should hand back the factory-created entity manager");

    EntityManager second = session.getEntityManager();

    Assert.assertSame(second, entityManager, "a second call on the same thread should reuse the cached entity manager");
    Mockito.verify(entityManagerFactory, Mockito.times(1)).createEntityManager();
  }

  public void testGetNativeSessionDelegatesToGetEntityManager () {

    JPAProxySession session = session();

    Assert.assertSame(session.getNativeSession(), entityManager, "getNativeSession should expose the active entity manager");
    Mockito.verify(entityManagerFactory, Mockito.times(1)).createEntityManager();
  }

  public void testGetEntityManagerReplacesAClosedManager () {

    JPAProxySession session = session();
    EntityManager replacement = Mockito.mock(EntityManager.class);

    Mockito.when(replacement.isOpen()).thenReturn(true);
    Mockito.when(entityManagerFactory.createEntityManager()).thenReturn(entityManager, replacement);

    Assert.assertSame(session.getEntityManager(), entityManager, "the first entity manager should be created");

    Mockito.when(entityManager.isOpen()).thenReturn(false);

    Assert.assertSame(session.getEntityManager(), replacement, "a closed entity manager should be discarded and replaced");
    Mockito.verify(entityManagerFactory, Mockito.times(2)).createEntityManager();
  }

  public void testIsClosedWhenNoEntityManagerHasBeenCreated () {

    Assert.assertTrue(session().isClosed(), "a session with no thread-bound entity manager should report closed");
  }

  public void testIsClosedFalseForAnOpenEntityManager () {

    JPAProxySession session = session();

    session.getEntityManager();

    Assert.assertFalse(session.isClosed(), "a session with an open entity manager should not report closed");
  }

  public void testIsClosedTrueForAClosedEntityManager () {

    JPAProxySession session = session();

    session.getEntityManager();
    Mockito.when(entityManager.isOpen()).thenReturn(false);

    Assert.assertTrue(session.isClosed(), "a session whose entity manager has been closed should report closed");
  }

  public void testCurrentTransactionIsNullUntilOneIsBegun () {

    Assert.assertNull(session().currentTransaction(), "no transaction should be reported before one is begun");
  }

  public void testCurrentTransactionReturnsTheBegunTransaction () {

    JPAProxySession session = session();

    Mockito.when(entityManager.getTransaction()).thenReturn(Mockito.mock(EntityTransaction.class));

    JPAProxyTransaction begun = session.beginTransaction();

    Assert.assertSame(session.currentTransaction(), begun, "currentTransaction should return the transaction bound to the thread");
  }

  public void testClearDelegatesToTheEntityManager () {

    session().clear();

    Mockito.verify(entityManager).clear();
  }

  public void testGetNativeSessionFactoryReturnsTheBackingFactory () {

    Assert.assertSame(session().getNativeSessionFactory(), entityManagerFactory, "the native session factory should be the one supplied at construction");
  }
}
