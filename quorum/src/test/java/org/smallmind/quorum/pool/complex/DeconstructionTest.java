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
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.PoolComponentSupport.InstanceFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the {@link DeconstructionQueue}/{@link DeconstructionFuse} machinery end-to-end through
 * the real background poller. Each fuse is configured with a one-second limit; the queue polls once
 * per second, so a fuse fires somewhere between one and two seconds after it is scheduled. The
 * waits below are padded to two and a half seconds to stay clear of that window.
 */
@Test(groups = "unit")
public class DeconstructionTest {

  private static final int LIMIT_SECONDS = 1;
  private static final long SETTLE_MILLIS = 2500L;

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation reads a PerApplicationContext, which throws when none is
    // attached to the thread; install an empty one so Instrument resolves to its no-op. The
    // deconstruction poller thread, created during startup, inherits it.
    new PerApplicationContext();
  }

  public void testIdleComponentIsReapedAfterTheIdleTimeout ()
    throws ComponentPoolException, InterruptedException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("idle", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));

    pool.startup();
    try {
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "a component left idle past the idle timeout should be reaped");
      Assert.assertTrue(factory.instance(0).isClosed());
    } finally {
      pool.shutdown();
    }
  }

  public void testComponentIsReapedAfterTheLeaseTimeout ()
    throws ComponentPoolException, InterruptedException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("lease", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxLeaseTimeSeconds(LIMIT_SECONDS));

    pool.startup();
    try {
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "a component older than the lease timeout should be reaped regardless of use");
      Assert.assertTrue(factory.instance(0).isClosed());
    } finally {
      pool.shutdown();
    }
  }

  public void testHeldComponentIsForciblyReclaimedAfterTheProcessingTimeout ()
    throws ComponentPoolException, InterruptedException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("processing", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxProcessingTimeSeconds(LIMIT_SECONDS));

    pool.startup();
    try {
      pool.getComponent();
      // The component is never returned; the prejudicial processing fuse should reclaim it anyway.
      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "a component held past the processing timeout should be forcibly reclaimed");
      Assert.assertTrue(factory.instance(0).isClosed());
    } finally {
      pool.shutdown();
    }
  }

  public void testMultipleSimultaneousIdleComponentsAreAllReapedInOrder ()
    throws ComponentPoolException, InterruptedException {

    // Pre-warming three idle components schedules three fuses at essentially the same instant, so the
    // queue's ignition keys must be compared against one another (both the equal-time tie-break and the
    // ordinary time ordering) on insertion and during the sweep. All three should still be reaped.
    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("multi-idle", factory, new ComplexPoolConfig().setInitialPoolSize(3).setMinPoolSize(0).setMaxIdleTimeSeconds(LIMIT_SECONDS));

    pool.startup();
    try {
      Assert.assertEquals(pool.getPoolSize(), 3, "all three idle components should be pre-warmed");

      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 0, "the ordered multi-fuse sweep should reap every idle component");
      Assert.assertTrue(factory.instance(0).isClosed());
      Assert.assertTrue(factory.instance(1).isClosed());
      Assert.assertTrue(factory.instance(2).isClosed());
    } finally {
      pool.shutdown();
    }
  }

  public void testReturningBeforeTheProcessingTimeoutCancelsReclamation ()
    throws ComponentPoolException, InterruptedException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("processing-cancel", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMinPoolSize(0).setMaxProcessingTimeSeconds(LIMIT_SECONDS));

    pool.startup();
    try {
      pool.getComponent();
      pool.returnInstance(factory.instance(0));

      Thread.sleep(SETTLE_MILLIS);

      Assert.assertEquals(pool.getPoolSize(), 1, "a component returned before the processing timeout should survive");
      Assert.assertEquals(pool.getFreeSize(), 1);
      Assert.assertFalse(factory.instance(0).isClosed());
    } finally {
      pool.shutdown();
    }
  }
}
