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

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link ProxyTransaction#applyPostProcesses(TransactionEndState)} on a minimal concrete
 * subclass, verifying priority ordering of execution, end-state filtering (ANY matches any state,
 * a specific state matches only itself), and aggregation of multiple failures into a single
 * {@link TransactionPostProcessException} with its first cause plus subsequent causes.
 */
@Test(groups = "unit")
public class ProxyTransactionTest {

  public void testProcessesRunInPriorityOrder ()
    throws TransactionPostProcessException {

    List<String> order = new ArrayList<>();
    FakeProxyTransaction transaction = new FakeProxyTransaction();

    transaction.addPostProcess(new RecordingPostProcess("last", TransactionEndState.ANY, ProcessPriority.LAST, order));
    transaction.addPostProcess(new RecordingPostProcess("first", TransactionEndState.ANY, ProcessPriority.FIRST, order));
    transaction.addPostProcess(new RecordingPostProcess("middle", TransactionEndState.ANY, ProcessPriority.MIDDLE, order));

    transaction.applyPostProcesses(TransactionEndState.COMMIT);

    Assert.assertEquals(order, List.of("first", "middle", "last"));
  }

  public void testAnyEndStateRunsRegardlessOfState ()
    throws TransactionPostProcessException {

    List<String> order = new ArrayList<>();
    FakeProxyTransaction transaction = new FakeProxyTransaction();

    transaction.addPostProcess(new RecordingPostProcess("any", TransactionEndState.ANY, ProcessPriority.MIDDLE, order));

    transaction.applyPostProcesses(TransactionEndState.ROLLBACK);

    Assert.assertEquals(order, List.of("any"));
  }

  public void testMatchingEndStateRunsAndNonMatchingIsSkipped ()
    throws TransactionPostProcessException {

    List<String> order = new ArrayList<>();
    FakeProxyTransaction transaction = new FakeProxyTransaction();

    transaction.addPostProcess(new RecordingPostProcess("commit", TransactionEndState.COMMIT, ProcessPriority.FIRST, order));
    transaction.addPostProcess(new RecordingPostProcess("rollback", TransactionEndState.ROLLBACK, ProcessPriority.LAST, order));

    transaction.applyPostProcesses(TransactionEndState.COMMIT);

    Assert.assertEquals(order, List.of("commit"));
  }

  public void testMultipleFailuresAggregateIntoSingleException () {

    FakeProxyTransaction transaction = new FakeProxyTransaction();

    Exception firstFailure = new IllegalStateException("first");
    Exception secondFailure = new IllegalArgumentException("second");
    Exception thirdFailure = new RuntimeException("third");

    transaction.addPostProcess(new FailingPostProcess(firstFailure, ProcessPriority.FIRST));
    transaction.addPostProcess(new FailingPostProcess(secondFailure, ProcessPriority.MIDDLE));
    transaction.addPostProcess(new FailingPostProcess(thirdFailure, ProcessPriority.LAST));

    try {
      transaction.applyPostProcesses(TransactionEndState.ANY);
      Assert.fail("Expected a TransactionPostProcessException");
    } catch (TransactionPostProcessException postProcessException) {

      Assert.assertSame(postProcessException.getFirstCause(), firstFailure);

      Throwable[] subsequentCauses = postProcessException.getSubsequentCauses();

      Assert.assertEquals(subsequentCauses.length, 2);
      Assert.assertSame(subsequentCauses[0], secondFailure);
      Assert.assertSame(subsequentCauses[1], thirdFailure);
    }
  }

  private static class FakeProxyTransaction extends ProxyTransaction<ProxySession<?, ?>> {

    private FakeProxyTransaction () {

      super(null);
    }

    @Override
    public void flush () {

      throw new UnsupportedOperationException();
    }

    @Override
    public void commit () {

      throw new UnsupportedOperationException();
    }

    @Override
    public void rollback () {

      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCompleted () {

      return false;
    }
  }

  private static class RecordingPostProcess extends TransactionPostProcess {

    private final List<String> order;
    private final String name;

    private RecordingPostProcess (String name, TransactionEndState endState, ProcessPriority priority, List<String> order) {

      super(endState, priority);

      this.name = name;
      this.order = order;
    }

    @Override
    public void process () {

      order.add(name);
    }
  }

  private static class FailingPostProcess extends TransactionPostProcess {

    private final Exception failure;

    private FailingPostProcess (Exception failure, ProcessPriority priority) {

      super(TransactionEndState.ANY, priority);

      this.failure = failure;
    }

    @Override
    public void process ()
      throws Exception {

      throw failure;
    }
  }
}
