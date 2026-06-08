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
 * Drives the {@link NonTransactionalState} session-boundary state machine through its package-protected
 * lifecycle ({@code startBoundary}/{@code endBoundary}) using session and transaction doubles, and verifies
 * the coordination with {@link TransactionalState} whereby an active transaction's session takes precedence.
 * Each method begins from cleared thread-local state.
 */
@Test(groups = "unit")
public class NonTransactionalStateTest {

  @NonTransactional(implicit = true)
  private static void implicitBoundary () {

  }

  @NonTransactional(implicit = false, dataSources = {"orders"})
  private static void explicitOrdersBoundary () {

  }

  @Transactional(implicit = true)
  private static void implicitTransactionalBoundary () {

  }

  @BeforeMethod
  public void clearThreadLocalState ()
    throws Exception {

    clearThreadLocal(NonTransactionalState.class, "SESSION_SET_STACK_LOCAL");
    clearThreadLocal(TransactionalState.class, "TRANSACTION_SET_STACK_LOCAL");
  }

  private static void clearThreadLocal (Class<?> stateClass, String fieldName)
    throws Exception {

    Field field = stateClass.getDeclaredField(fieldName);

    field.setAccessible(true);
    ((ThreadLocal<?>)field.get(null)).remove();
  }

  private static NonTransactional nonTransactional (String holderMethodName) {

    try {

      return NonTransactionalStateTest.class.getDeclaredMethod(holderMethodName).getAnnotation(NonTransactional.class);
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException(noSuchMethodException);
    }
  }

  private static Transactional transactional (String holderMethodName) {

    try {

      return NonTransactionalStateTest.class.getDeclaredMethod(holderMethodName).getAnnotation(Transactional.class);
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException(noSuchMethodException);
    }
  }

  public void testNoBoundaryMeansNoActiveSession () {

    Assert.assertFalse(NonTransactionalState.isInSession());
    Assert.assertNull(NonTransactionalState.currentSession("orders"));
    Assert.assertNull(NonTransactionalState.obtainBoundary(new FakeProxySession("orders")));
  }

  public void testCurrentSessionMatchesBySourceKey () {

    FakeProxySession session = new FakeProxySession("orders");

    NonTransactionalState.startBoundary(nonTransactional("implicitBoundary"));
    NonTransactionalState.obtainBoundary(session).add(session);

    Assert.assertSame(NonTransactionalState.currentSession("orders"), session);
    Assert.assertTrue(NonTransactionalState.isInSession("orders"));
    Assert.assertNull(NonTransactionalState.currentSession("invoices"));
  }

  public void testObtainBoundaryRespectsExplicitSourceKeys () {

    NonTransactionalState.startBoundary(nonTransactional("explicitOrdersBoundary"));

    Assert.assertNotNull(NonTransactionalState.obtainBoundary(new FakeProxySession("orders")));
    Assert.assertNull(NonTransactionalState.obtainBoundary(new FakeProxySession("invoices")));
  }

  public void testCurrentSessionPrefersActiveTransactionalSession () {

    FakeProxySession transactionalSession = new FakeProxySession("orders");
    FakeProxyTransaction transaction = new FakeProxyTransaction(transactionalSession);

    TransactionalState.startBoundary(transactional("implicitTransactionalBoundary"));
    TransactionalState.obtainBoundary(transactionalSession).add(transaction);

    FakeProxySession nonTransactionalSession = new FakeProxySession("orders");

    NonTransactionalState.startBoundary(nonTransactional("implicitBoundary"));
    NonTransactionalState.obtainBoundary(nonTransactionalSession).add(nonTransactionalSession);

    Assert.assertSame(NonTransactionalState.currentSession("orders"), transactionalSession);
  }

  public void testEndBoundaryClosesContainedSessions () {

    FakeProxySession session = new FakeProxySession("orders");

    NonTransactionalState.startBoundary(nonTransactional("implicitBoundary"));
    NonTransactionalState.obtainBoundary(session).add(session);

    NonTransactionalState.endBoundary(null);

    Assert.assertTrue(session.wasClosed());
    Assert.assertFalse(NonTransactionalState.isInSession("orders"));
  }

  @Test(groups = "unit", expectedExceptions = SessionBoundaryError.class)
  public void testEndBoundaryWithoutBoundaryThrows () {

    NonTransactionalState.endBoundary(null);
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
}
