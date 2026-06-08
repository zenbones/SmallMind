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
package org.smallmind.persistence.orm.aop;

import java.lang.reflect.Field;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Drives the {@link TransactionalState} boundary state machine through its package-protected lifecycle
 * ({@code startBoundary}/{@code commitBoundary}/{@code rollbackBoundary}) using session and transaction
 * doubles, with no ORM, JDBC, or aspect weaving. Each method begins from cleared thread-local state so
 * leftover boundaries from a failed assertion cannot leak between tests.
 */
@Test(groups = "unit")
public class TransactionalStateTest {

  @Transactional(implicit = true)
  private static void implicitBoundary () {

  }

  @Transactional(implicit = false, dataSources = {"orders"})
  private static void explicitOrdersBoundary () {

  }

  @Transactional(implicit = false, rollbackOnly = true)
  private static void rollbackOnlyBoundary () {

  }

  @NonTransactional(implicit = true)
  private static void implicitNonTransactionalBoundary () {

  }

  @BeforeMethod
  public void clearThreadLocalState ()
    throws Exception {

    clearThreadLocal(TransactionalState.class, "TRANSACTION_SET_STACK_LOCAL");
    clearThreadLocal(NonTransactionalState.class, "SESSION_SET_STACK_LOCAL");
  }

  private static void clearThreadLocal (Class<?> stateClass, String fieldName)
    throws Exception {

    Field field = stateClass.getDeclaredField(fieldName);

    field.setAccessible(true);
    ((ThreadLocal<?>)field.get(null)).remove();
  }

  private static Transactional transactional (String holderMethodName) {

    try {

      return TransactionalStateTest.class.getDeclaredMethod(holderMethodName).getAnnotation(Transactional.class);
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException(noSuchMethodException);
    }
  }

  private static NonTransactional nonTransactional (String holderMethodName) {

    try {

      return TransactionalStateTest.class.getDeclaredMethod(holderMethodName).getAnnotation(NonTransactional.class);
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException(noSuchMethodException);
    }
  }

  public void testNoBoundaryMeansNoActiveTransaction () {

    Assert.assertFalse(TransactionalState.isInTransaction());
    Assert.assertNull(TransactionalState.currentTransaction(null));
    Assert.assertFalse(TransactionalState.withinBoundary("orders"));
    Assert.assertNull(TransactionalState.obtainBoundary(new FakeProxySession("orders")));
  }

  public void testWithinBoundaryHonorsExplicitSourceKeys () {

    TransactionalState.startBoundary(transactional("explicitOrdersBoundary"));

    Assert.assertTrue(TransactionalState.withinBoundary("orders"));
    Assert.assertFalse(TransactionalState.withinBoundary("invoices"));
    Assert.assertFalse(TransactionalState.withinBoundary((String)null));
  }

  public void testImplicitBoundaryAllowsEveryKey () {

    TransactionalState.startBoundary(transactional("implicitBoundary"));

    Assert.assertTrue(TransactionalState.withinBoundary("anything"));
    Assert.assertTrue(TransactionalState.withinBoundary((String)null));
  }

  public void testCurrentTransactionMatchesBySessionSourceKey () {

    FakeProxySession session = new FakeProxySession("orders");
    FakeProxyTransaction transaction = new FakeProxyTransaction(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    Assert.assertSame(TransactionalState.currentTransaction("orders"), transaction);
    Assert.assertTrue(TransactionalState.isInTransaction("orders"));
    Assert.assertNull(TransactionalState.currentTransaction("invoices"));
  }

  public void testCurrentTransactionWithNullKeyMatchesUnnamedSource () {

    FakeProxySession session = new FakeProxySession(null);
    FakeProxyTransaction transaction = new FakeProxyTransaction(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    Assert.assertSame(TransactionalState.currentTransaction(null), transaction);
    Assert.assertTrue(TransactionalState.isInTransaction());
    Assert.assertNull(TransactionalState.currentTransaction("orders"));
  }

  public void testObtainBoundaryReturnsBoundaryThatAllowsSession () {

    FakeProxySession session = new FakeProxySession("orders");

    TransactionalState.startBoundary(transactional("implicitBoundary"));

    Assert.assertNotNull(TransactionalState.obtainBoundary(session));
  }

  @Test(groups = "unit", expectedExceptions = StolenTransactionError.class)
  public void testObtainBoundaryThrowsWhenSessionAlreadyHeldNonTransactionally () {

    FakeProxySession session = new FakeProxySession("orders");

    NonTransactionalState.startBoundary(nonTransactional("implicitNonTransactionalBoundary"));
    NonTransactionalState.obtainBoundary(session).add(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session);
  }

  public void testCommitBoundaryCommitsContainedTransactions () {

    FakeProxySession session = new FakeProxySession("orders");
    FakeProxyTransaction transaction = new FakeProxyTransaction(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    TransactionalState.commitBoundary();

    Assert.assertTrue(transaction.wasCommitted());
    Assert.assertFalse(transaction.wasRolledBack());
    Assert.assertFalse(TransactionalState.isInTransaction("orders"));
  }

  public void testRollbackBoundaryRollsBackContainedTransactions () {

    FakeProxySession session = new FakeProxySession("orders");
    FakeProxyTransaction transaction = new FakeProxyTransaction(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    TransactionalState.rollbackBoundary(null);

    Assert.assertTrue(transaction.wasRolledBack());
    Assert.assertFalse(transaction.wasCommitted());
    Assert.assertFalse(TransactionalState.isInTransaction("orders"));
  }

  public void testRollbackOnlyBoundaryForcesRollbackOnCommit () {

    FakeProxySession session = new FakeProxySession(null);
    FakeProxyTransaction transaction = new FakeProxyTransaction(session);

    TransactionalState.startBoundary(transactional("rollbackOnlyBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    TransactionalState.commitBoundary();

    Assert.assertTrue(transaction.wasRolledBack());
    Assert.assertFalse(transaction.wasCommitted());
  }

  public void testIndividuallyFlaggedTransactionIsRolledBackOnCommit () {

    FakeProxySession session = new FakeProxySession("orders");
    FakeProxyTransaction transaction = new FakeProxyTransaction(session);

    transaction.setRollbackOnly();

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    TransactionalState.commitBoundary();

    Assert.assertTrue(transaction.wasRolledBack());
    Assert.assertFalse(transaction.wasCommitted());
  }

  @Test(groups = "unit", expectedExceptions = TransactionBoundaryError.class)
  public void testCommitBoundaryWithoutBoundaryThrows () {

    TransactionalState.commitBoundary();
  }

  @Test(groups = "unit", expectedExceptions = IncompleteTransactionError.class)
  public void testCommitBoundaryAggregatesContainedFailureIntoIncompleteTransactionError () {

    FakeProxySession session = new FakeProxySession("orders");
    ThrowingProxyTransaction transaction = new ThrowingProxyTransaction(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    TransactionalState.commitBoundary();
  }

  public void testCommitBoundaryWithTerminalErrorShortCircuitsWithoutTouchingBoundaries () {

    FakeProxySession session = new FakeProxySession("orders");
    ThrowingProxyTransaction transaction = new ThrowingProxyTransaction(session);

    TransactionalState.startBoundary(transactional("implicitBoundary"));
    TransactionalState.obtainBoundary(session).add(transaction);

    // A terminal TransactionError makes endBoundary return immediately: the boundary is left untouched,
    // the contained transaction is neither committed nor rolled back, and no exception is raised.
    TransactionalState.commitBoundary(new TransactionBoundaryError("terminal"));

    Assert.assertFalse(transaction.wasCommitAttempted());
    Assert.assertFalse(transaction.wasRollbackAttempted());
    Assert.assertTrue(TransactionalState.isInTransaction("orders"));
  }

  public static class FakeProxySession extends ProxySession<Object, Object> {

    private boolean closed;

    public FakeProxySession (String sessionSourceKey) {

      super("fake", sessionSourceKey, true, false);
    }

    public boolean wasClosed () {

      return closed;
    }

    @Override
    public Object getNativeSessionFactory () {

      throw new UnsupportedOperationException();
    }

    @Override
    public Object getNativeSession () {

      throw new UnsupportedOperationException();
    }

    @Override
    public ProxyTransaction<?> beginTransaction () {

      throw new UnsupportedOperationException();
    }

    @Override
    public ProxyTransaction<?> currentTransaction () {

      throw new UnsupportedOperationException();
    }

    @Override
    public void flush () {

      throw new UnsupportedOperationException();
    }

    @Override
    public void clear () {

      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed () {

      return closed;
    }

    @Override
    public void close () {

      closed = true;
    }
  }

  public static class FakeProxyTransaction extends ProxyTransaction<FakeProxySession> {

    private boolean committed;
    private boolean rolledBack;

    public FakeProxyTransaction (FakeProxySession proxySession) {

      super(proxySession);
    }

    public boolean wasCommitted () {

      return committed;
    }

    public boolean wasRolledBack () {

      return rolledBack;
    }

    @Override
    public void flush () {

      throw new UnsupportedOperationException();
    }

    @Override
    public void commit () {

      committed = true;
    }

    @Override
    public void rollback () {

      rolledBack = true;
    }

    @Override
    public boolean isCompleted () {

      return committed || rolledBack;
    }
  }

  public static class ThrowingProxyTransaction extends ProxyTransaction<FakeProxySession> {

    private boolean commitAttempted;
    private boolean rollbackAttempted;

    public ThrowingProxyTransaction (FakeProxySession proxySession) {

      super(proxySession);
    }

    public boolean wasCommitAttempted () {

      return commitAttempted;
    }

    public boolean wasRollbackAttempted () {

      return rollbackAttempted;
    }

    @Override
    public void flush () {

      throw new UnsupportedOperationException();
    }

    @Override
    public void commit () {

      commitAttempted = true;

      throw new IllegalStateException("commit failed");
    }

    @Override
    public void rollback () {

      rollbackAttempted = true;

      throw new IllegalStateException("rollback failed");
    }

    @Override
    public boolean isCompleted () {

      return false;
    }
  }
}
