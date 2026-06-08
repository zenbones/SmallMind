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
import jakarta.persistence.EntityTransaction;
import org.mockito.Mockito;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link JPAProxyTransaction}. The owning {@link JPAProxySession} and the native
 * {@link EntityTransaction} are Mockito mocks, so the commit/rollback state machine is driven without a database.
 * The tests verify that the constructor begins an inactive transaction, that {@code commit()} flushes and commits
 * (firing the session close on the way out), that the rollback-only and SQL-failure paths funnel through the
 * native {@code rollback()} and surface a {@link ProxyTransactionException}, that a closed session blocks both
 * commit and rollback, that {@code isCompleted()} mirrors the native active flag, and that {@code flush()}
 * delegates to the session.
 */
@Test(groups = "unit")
public class JPAProxyTransactionTest {

  private JPAProxySession proxySession;
  private EntityManager entityManager;
  private EntityTransaction transaction;

  @BeforeMethod
  public void setUp () {

    proxySession = Mockito.mock(JPAProxySession.class);
    entityManager = Mockito.mock(EntityManager.class);
    transaction = Mockito.mock(EntityTransaction.class);

    Mockito.when(proxySession.getNativeSession()).thenReturn(entityManager);
    Mockito.when(entityManager.isOpen()).thenReturn(true);
  }

  public void testConstructorBeginsAnInactiveTransaction () {

    Mockito.when(transaction.isActive()).thenReturn(false);

    new JPAProxyTransaction(proxySession, transaction);

    Mockito.verify(transaction).begin();
  }

  public void testConstructorDoesNotBeginAnAlreadyActiveTransaction () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    new JPAProxyTransaction(proxySession, transaction);

    Mockito.verify(transaction, Mockito.never()).begin();
  }

  public void testCommitFlushesCommitsAndClosesTheSession () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    proxyTransaction.commit();

    Mockito.verify(proxySession).flush();
    Mockito.verify(transaction).commit();
    Mockito.verify(proxySession).close();
  }

  public void testCommitOnAClosedSessionThrowsWithoutCommitting () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    Mockito.when(entityManager.isOpen()).thenReturn(false);

    try {
      proxyTransaction.commit();
      Assert.fail("committing with a closed session should throw a ProxyTransactionException");
    } catch (ProxyTransactionException proxyTransactionException) {
      Assert.assertTrue(proxyTransactionException.getMessage().contains("no longer open"), "the failure should explain that the session is no longer open");
    }

    Mockito.verify(transaction, Mockito.never()).commit();
  }

  public void testCommitOfARollbackOnlyTransactionRollsBackAndThrows () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    proxyTransaction.setRollbackOnly();

    try {
      proxyTransaction.commit();
      Assert.fail("committing a rollback-only transaction should throw a ProxyTransactionException");
    } catch (ProxyTransactionException proxyTransactionException) {
      Assert.assertTrue(proxyTransactionException.getMessage().contains("rollback only"), "the failure should describe the rollback-only condition");
    }

    Mockito.verify(transaction).rollback();
    Mockito.verify(transaction, Mockito.never()).commit();
    Mockito.verify(proxySession).close();
  }

  public void testCommitFailureFunnelsThroughRollback () {

    Mockito.when(transaction.isActive()).thenReturn(true);
    Mockito.doThrow(new RuntimeException("commit boom")).when(transaction).commit();

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    try {
      proxyTransaction.commit();
      Assert.fail("a commit failure should be wrapped and surfaced as a ProxyTransactionException");
    } catch (ProxyTransactionException proxyTransactionException) {
      Assert.assertTrue(throwableInCauseChain(proxyTransactionException, "commit boom"), "the original commit failure should remain in the cause chain");
    }

    Mockito.verify(transaction).rollback();
    // A commit failure closes the session twice (once in rollback, once in commit's finally); close() is idempotent.
    Mockito.verify(proxySession, Mockito.atLeastOnce()).close();
  }

  public void testRollbackInvokesTheNativeRollbackAndClosesTheSession () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    proxyTransaction.rollback();

    Mockito.verify(transaction).rollback();
    Mockito.verify(proxySession).close();
  }

  public void testRollbackOnAClosedSessionThrows () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    Mockito.when(entityManager.isOpen()).thenReturn(false);

    try {
      proxyTransaction.rollback();
      Assert.fail("rolling back with a closed session should throw a ProxyTransactionException");
    } catch (ProxyTransactionException proxyTransactionException) {
      Assert.assertTrue(proxyTransactionException.getMessage().contains("no longer open"), "the failure should explain that the session is no longer open");
    }

    Mockito.verify(transaction, Mockito.never()).rollback();
  }

  public void testRollbackIsIdempotent () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    proxyTransaction.rollback();
    proxyTransaction.rollback();

    Mockito.verify(transaction, Mockito.times(1)).rollback();
  }

  public void testIsCompletedMirrorsTheNativeActiveFlag () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    Assert.assertFalse(proxyTransaction.isCompleted(), "an active transaction should not be reported as completed");

    Mockito.when(transaction.isActive()).thenReturn(false);

    Assert.assertTrue(proxyTransaction.isCompleted(), "an inactive transaction should be reported as completed");
  }

  public void testFlushDelegatesToTheSession () {

    Mockito.when(transaction.isActive()).thenReturn(true);

    JPAProxyTransaction proxyTransaction = new JPAProxyTransaction(proxySession, transaction);

    proxyTransaction.flush();

    Mockito.verify(proxySession).flush();
  }

  private boolean throwableInCauseChain (Throwable throwable, String message) {

    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if ((cause.getMessage() != null) && cause.getMessage().contains(message)) {

        return true;
      }
    }

    return false;
  }
}
