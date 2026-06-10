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
package org.smallmind.quorum.pool.complex;

import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.PoolComponentSupport.InstanceFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Targets the {@link ComponentPinManager} branches the higher-level {@link ComponentPoolTest} does
 * not reach: acquisition against a not-yet-started pool, the validate-on-create failure path, a raw
 * factory exception during on-demand creation, the blocking-acquire wait that is satisfied by a
 * concurrent return, and existential stack-trace reporting that distinguishes checked-out from idle
 * components. Everything is driven through the public {@link ComponentPool} surface, since the
 * manager is an internal collaborator.
 */
@Test(groups = "unit")
public class ComponentPinManagerTest {

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation reads a PerApplicationContext; install an empty one so
    // Instrument resolves to its no-op. Child threads inherit it.
    new PerApplicationContext();
  }

  public void testAcquiringBeforeStartupIsRejected () {

    ComponentPool<String> pool = new ComponentPool<>("not-started", new InstanceFactory(), new ComplexPoolConfig().setMaxPoolSize(1));

    // serve() guards on the STARTED state and throws before any component is manufactured.
    Assert.assertThrows(ComponentPoolException.class, pool::getComponent);
  }

  public void testValidateOnCreateFailureSurfacesAsAPoolException ()
    throws ComponentPoolException {

    // The first on-demand instance (c0) is invalid; with testOnCreate the manager must reject it
    // rather than hand back an unhealthy component. Nothing is pre-warmed, so the failure happens
    // on the first acquire rather than at startup.
    InstanceFactory factory = new InstanceFactory(false);
    ComponentPool<String> pool = new ComponentPool<>("test-on-create", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setMaxPoolSize(2).setTestOnCreate(true));

    pool.startup();
    try {
      Assert.assertThrows(ComponentPoolException.class, pool::getComponent);
    } finally {
      pool.shutdown();
    }
  }

  public void testFactoryExceptionDuringOnDemandCreationSurfaces ()
    throws ComponentPoolException {

    // A factory that always throws produces a ComponentCreationException (distinct from the timeout
    // path), wrapped by getComponent as a ComponentPoolException.
    InstanceFactory factory = new InstanceFactory(0L, true);
    ComponentPool<String> pool = new ComponentPool<>("factory-throws", factory, new ComplexPoolConfig().setInitialPoolSize(0).setMinPoolSize(0).setMaxPoolSize(2));

    pool.startup();
    try {
      Assert.assertThrows(ComponentPoolException.class, pool::getComponent);
    } finally {
      pool.shutdown();
    }
  }

  public void testBlockingAcquireSucceedsWhenAComponentIsReturned ()
    throws ComponentPoolException, InterruptedException {

    // The capped, single-instance pool is fully checked out, forcing the second acquire into the
    // timed poll loop; a concurrent return wakes it well within the generous wait budget.
    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("blocking-acquire", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMaxPoolSize(1).setAcquireWaitTimeMillis(5000L));

    pool.startup();
    try {

      String first = pool.getComponent();

      Assert.assertEquals(first, "c0");
      Assert.assertEquals(pool.getFreeSize(), 0, "the only instance is now checked out");

      Thread releaser = new Thread(() -> {

        try {
          Thread.sleep(200L);
          pool.returnInstance(factory.instance(0));
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        }
      });

      releaser.start();

      // No instance is free, so this blocks in the timed poll until the releaser returns c0.
      String second = pool.getComponent();

      releaser.join();
      Assert.assertEquals(second, "c0", "the timed acquire should receive the concurrently returned instance");
    } finally {
      pool.shutdown();
    }
  }

  public void testExistentialStackTracesReportOnlyCheckedOutComponents ()
    throws ComponentPoolException {

    ComponentPool<String> pool = new ComponentPool<>("existential", new TracedInstanceFactory(), new ComplexPoolConfig().setInitialPoolSize(2).setMaxPoolSize(2).setExistentiallyAware(true));

    pool.startup();
    try {
      Assert.assertEquals(pool.getExistentialStackTraces().length, 0, "no checked-out components means no recorded traces");

      pool.getComponent();

      StackTrace[] traces = pool.getExistentialStackTraces();

      Assert.assertEquals(traces.length, 1, "only the single checked-out component should contribute a trace");
      Assert.assertNotNull(traces[0]);
    } finally {
      pool.shutdown();
    }
  }

  public void testBlockingAcquireDiscardsAnInvalidReturnAndHonoursTheWaitBudget ()
    throws ComponentPoolException, InterruptedException {

    // The single capped instance validates true on the first acquire, then is flipped invalid and
    // returned while a second caller is already blocked in the timed poll. That caller must discard the
    // poisoned instance, recompute its remaining wait, and ultimately time out — not hang or spin or
    // hand back the unusable component.
    FlakyValidationFactory factory = new FlakyValidationFactory();
    ComponentPool<String> pool = new ComponentPool<>("flaky-validate", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMaxPoolSize(1).setTestOnAcquire(true).setAcquireWaitTimeMillis(2000L));

    pool.startup();
    try {

      String first = pool.getComponent();

      Assert.assertEquals(first, "flaky", "the initially valid instance should be served");
      Assert.assertEquals(pool.getProcessingSize(), 1, "the only instance is now checked out, so the next acquire must block");

      Thread releaser = new Thread(() -> {

        try {
          Thread.sleep(200L);
          factory.instance().setValid(false);
          pool.returnInstance(factory.instance());
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        }
      });

      releaser.start();

      long start = System.currentTimeMillis();

      Assert.assertThrows(ComponentPoolException.class, pool::getComponent);

      long elapsed = System.currentTimeMillis() - start;

      releaser.join();
      Assert.assertTrue(elapsed >= 1000L, "the blocked acquire should have honoured most of its wait budget rather than failing instantly, but took " + elapsed + "ms");
      Assert.assertEquals(pool.getPoolSize(), 0, "the invalid instance discarded during the blocking poll should be gone");
    } finally {
      pool.shutdown();
    }
  }

  // A factory whose instances always report a (fixed, non-null) existential stack trace, so the
  // manager's trace-collecting loop has something to gather for checked-out components.
  private static class TracedInstanceFactory extends AbstractComponentInstanceFactory<String> {

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool) {

      return new ComponentInstance<>() {

        @Override
        public boolean validate () {

          return true;
        }

        @Override
        public String serve () {

          return "component";
        }

        @Override
        public void close () {

        }

        @Override
        public StackTraceElement[] getExistentialStackTrace () {

          return Thread.currentThread().getStackTrace();
        }
      };
    }
  }

  // Hands out a single, reused instance whose validity can be flipped at runtime, so a test can make a
  // returned component fail validation on its next acquire.
  private static class FlakyValidationFactory extends AbstractComponentInstanceFactory<String> {

    private final FlakyInstance instance = new FlakyInstance();

    private FlakyInstance instance () {

      return instance;
    }

    @Override
    public ComponentInstance<String> createInstance (ComponentPool<String> componentPool) {

      return instance;
    }
  }

  private static class FlakyInstance implements ComponentInstance<String> {

    private volatile boolean valid = true;

    private void setValid (boolean valid) {

      this.valid = valid;
    }

    @Override
    public boolean validate () {

      return valid;
    }

    @Override
    public String serve () {

      return "flaky";
    }

    @Override
    public void close () {

    }

    @Override
    public StackTraceElement[] getExistentialStackTrace () {

      return null;
    }
  }
}
