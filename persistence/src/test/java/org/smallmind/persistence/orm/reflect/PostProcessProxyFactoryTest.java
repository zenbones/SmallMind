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
package org.smallmind.persistence.orm.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcess;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the dynamic-proxy post-process plumbing in {@link PostProcessInvocationHandler},
 * {@link PostProcessProxyFactory}, and {@link DelayedInvocationPostProcess}: the void/non-void dispatch
 * decision in the handler, the registration of a {@link DelayedInvocationPostProcess} on the supplied
 * transaction, the factory's rejection when no transaction is active for a session key, and the
 * checked-exception unwrapping performed by {@code process()}.
 */
@Test(groups = "unit")
public class PostProcessProxyFactoryTest {

  public void testHandlerRegistersDelayedInvocationForVoidMethod ()
    throws Throwable {

    RecordingProxyTransaction proxyTransaction = new RecordingProxyTransaction();
    TargetRecorder target = new TargetRecorder();
    PostProcessInvocationHandler handler = new PostProcessInvocationHandler(proxyTransaction, target, TransactionEndState.COMMIT, ProcessPriority.MIDDLE);

    Method voidMethod = ProxyContract.class.getMethod("doVoidThing", String.class);
    Object result = handler.invoke(null, voidMethod, new Object[] {"payload"});

    Assert.assertNull(result);
    Assert.assertEquals(proxyTransaction.getPostProcesses().size(), 1);
    Assert.assertTrue(proxyTransaction.getPostProcesses().get(0) instanceof DelayedInvocationPostProcess);
  }

  @Test(groups = "unit", expectedExceptions = ProxyTransactionException.class)
  public void testHandlerRejectsNonVoidMethod ()
    throws Throwable {

    RecordingProxyTransaction proxyTransaction = new RecordingProxyTransaction();
    TargetRecorder target = new TargetRecorder();
    PostProcessInvocationHandler handler = new PostProcessInvocationHandler(proxyTransaction, target, TransactionEndState.COMMIT, ProcessPriority.MIDDLE);

    Method nonVoidMethod = ProxyContract.class.getMethod("returnsValue");
    handler.invoke(null, nonVoidMethod, new Object[0]);
  }

  public void testFactoryWithExplicitTransactionProducesProxyThatRegistersPostProcess () {

    RecordingProxyTransaction proxyTransaction = new RecordingProxyTransaction();
    TargetRecorder target = new TargetRecorder();

    Proxy proxy = PostProcessProxyFactory.generatePostProcessProxy(proxyTransaction, ProxyContract.class, target, TransactionEndState.COMMIT, ProcessPriority.MIDDLE);
    ((ProxyContract)proxy).doVoidThing("payload");

    Assert.assertEquals(proxyTransaction.getPostProcesses().size(), 1);
    Assert.assertTrue(proxyTransaction.getPostProcesses().get(0) instanceof DelayedInvocationPostProcess);
  }

  @Test(groups = "unit", expectedExceptions = ProxyTransactionException.class)
  public void testFactoryThrowsWhenNoCurrentTransactionForSessionKey () {

    // With no transactional boundary started on this thread, TransactionalState.currentTransaction(...)
    // returns null for any key, so the session-key overload must raise ProxyTransactionException.
    PostProcessProxyFactory.generatePostProcessProxy("no-such-source", ProxyContract.class, new TargetRecorder(), TransactionEndState.COMMIT, ProcessPriority.MIDDLE);
  }

  public void testDelayedInvocationProcessInvokesTargetMethod ()
    throws Exception {

    TargetRecorder target = new TargetRecorder();
    Method voidMethod = TargetRecorder.class.getMethod("record", String.class);
    DelayedInvocationPostProcess postProcess = new DelayedInvocationPostProcess(TransactionEndState.COMMIT, ProcessPriority.MIDDLE, target, voidMethod, "captured");

    postProcess.process();

    Assert.assertEquals(target.getRecorded(), "captured");
  }

  @Test(groups = "unit", expectedExceptions = ExplodingException.class)
  public void testDelayedInvocationProcessUnwrapsCheckedCauseFromInvocationTarget ()
    throws Exception {

    TargetRecorder target = new TargetRecorder();
    Method explodingMethod = TargetRecorder.class.getMethod("explode");
    DelayedInvocationPostProcess postProcess = new DelayedInvocationPostProcess(TransactionEndState.COMMIT, ProcessPriority.MIDDLE, target, explodingMethod);

    // The target throws ExplodingException; Method.invoke wraps it in InvocationTargetException, and
    // process() must unwrap and rethrow the underlying checked exception.
    postProcess.process();
  }

  public interface ProxyContract {

    void doVoidThing (String value);

    String returnsValue ();
  }

  public static class TargetRecorder {

    private String recorded;

    public String getRecorded () {

      return recorded;
    }

    public void record (String value) {

      this.recorded = value;
    }

    public void explode ()
      throws ExplodingException {

      throw new ExplodingException("boom");
    }
  }

  public static class ExplodingException extends Exception {

    public ExplodingException (String message) {

      super(message);
    }
  }

  /**
   * Minimal concrete {@link ProxySession} so a {@link ProxyTransaction} can be constructed without a
   * native ORM. None of the abstract members are exercised by these tests.
   */
  public static class FakeProxySession extends ProxySession<Object, Object> {

    public FakeProxySession () {

      super("fake", "fake-source", false, false);
    }

    @Override
    public Object getNativeSessionFactory () {

      return null;
    }

    @Override
    public Object getNativeSession () {

      return null;
    }

    @Override
    public ProxyTransaction<?> beginTransaction () {

      return null;
    }

    @Override
    public ProxyTransaction<?> currentTransaction () {

      return null;
    }

    @Override
    public void flush () {

    }

    @Override
    public void clear () {

    }

    @Override
    public boolean isClosed () {

      return false;
    }

    @Override
    public void close () {

    }
  }

  /**
   * Minimal concrete {@link ProxyTransaction} that records the post-processes registered against it.
   */
  public static class RecordingProxyTransaction extends ProxyTransaction<FakeProxySession> {

    private final List<TransactionPostProcess> postProcesses = new ArrayList<>();

    public RecordingProxyTransaction () {

      super(new FakeProxySession());
    }

    public List<TransactionPostProcess> getPostProcesses () {

      return postProcesses;
    }

    @Override
    public void addPostProcess (TransactionPostProcess postProcess) {

      postProcesses.add(postProcess);
    }

    @Override
    public void flush () {

    }

    @Override
    public void commit () {

    }

    @Override
    public void rollback () {

    }

    @Override
    public boolean isCompleted () {

      return false;
    }
  }
}
