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

@Test(groups = "unit")
public class ComponentPoolTest {

  @BeforeMethod
  public void establishPerApplicationContext () {

    // The pool's Claxon instrumentation reads a PerApplicationContext, which throws when none is
    // attached to the thread; install an empty one so Instrument resolves to its no-op. Child threads
    // (the deconstruction poller, the creation worker) inherit it.
    new PerApplicationContext();
  }

  public void testStartupPrewarmsToTheGreaterOfMinAndInitial ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("prewarm", factory, new ComplexPoolConfig().setMinPoolSize(2).setInitialPoolSize(3));

    pool.startup();
    try {
      Assert.assertEquals(pool.getPoolSize(), 3, "startup should pre-create max(minPoolSize, initialPoolSize) components");
      Assert.assertEquals(pool.getFreeSize(), 3);
      Assert.assertEquals(factory.created(), 3);
    } finally {
      pool.shutdown();
    }
  }

  public void testServeRemovesFromFreeAndReturnReplenishesIt ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("serve", factory, new ComplexPoolConfig().setInitialPoolSize(1));

    pool.startup();
    try {
      pool.getComponent();
      Assert.assertEquals(pool.getFreeSize(), 0);
      Assert.assertEquals(pool.getProcessingSize(), 1);

      pool.returnInstance(factory.instance(0));
      Assert.assertEquals(pool.getFreeSize(), 1);
      Assert.assertEquals(pool.getProcessingSize(), 0);
    } finally {
      pool.shutdown();
    }
  }

  public void testCreatesOnDemandUpToMaximumThenTimesOut ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    // No acquire wait, so the over-capacity third request fails immediately rather than blocking.
    ComponentPool<String> pool = new ComponentPool<>("ondemand", factory, new ComplexPoolConfig().setMaxPoolSize(2));

    pool.startup();
    try {
      pool.getComponent();
      pool.getComponent();

      Assert.assertEquals(pool.getPoolSize(), 2);
      Assert.assertEquals(pool.getProcessingSize(), 2);
      Assert.assertEquals(factory.created(), 2);

      Assert.assertThrows(ComponentPoolException.class, pool::getComponent);
    } finally {
      pool.shutdown();
    }
  }

  public void testInvalidComponentIsDiscardedOnAcquireAndReplaced ()
    throws ComponentPoolException {

    // The single pre-warmed instance is invalid; with testOnAcquire it must be discarded and a fresh
    // one manufactured in its place.
    InstanceFactory factory = new InstanceFactory(false);
    ComponentPool<String> pool = new ComponentPool<>("validate", factory, new ComplexPoolConfig().setInitialPoolSize(1).setMaxPoolSize(2).setTestOnAcquire(true));

    pool.startup();
    try {

      String served = pool.getComponent();

      Assert.assertEquals(served, "c1", "the invalid c0 should be skipped and a fresh c1 served");
      Assert.assertTrue(factory.instance(0).isClosed(), "the invalid instance should have been closed");
      Assert.assertEquals(pool.getPoolSize(), 1);
    } finally {
      pool.shutdown();
    }
  }

  public void testTerminateInstanceReplacesToMaintainMinimum ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("replace", factory, new ComplexPoolConfig().setMinPoolSize(1).setInitialPoolSize(1).setMaxPoolSize(5));

    pool.startup();
    try {
      pool.terminateInstance(factory.instance(0));

      Assert.assertTrue(factory.instance(0).isClosed(), "the terminated instance should be closed");
      Assert.assertEquals(pool.getPoolSize(), 1, "the pool should replace the instance to honour the minimum size");
      Assert.assertEquals(factory.created(), 2);
    } finally {
      pool.shutdown();
    }
  }

  public void testKillAllProcessingTerminatesOnlyCheckedOutComponents ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("kill", factory, new ComplexPoolConfig().setInitialPoolSize(2).setMaxPoolSize(5));

    pool.startup();
    try {
      pool.getComponent();

      pool.killAllProcessing();

      Assert.assertEquals(pool.getProcessingSize(), 0, "the checked-out component should be terminated");
      Assert.assertEquals(pool.getPoolSize(), 1, "the idle component should remain");
    } finally {
      pool.shutdown();
    }
  }

  public void testShutdownTerminatesEveryComponent ()
    throws ComponentPoolException {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("shutdown", factory, new ComplexPoolConfig().setInitialPoolSize(2));

    pool.startup();
    pool.shutdown();

    Assert.assertEquals(pool.getPoolSize(), 0);
    Assert.assertTrue(factory.instance(0).isClosed());
    Assert.assertTrue(factory.instance(1).isClosed());
  }

  public void testCreationTimeoutSurfacesAsAPoolException ()
    throws ComponentPoolException {

    // The factory takes far longer than the creation timeout, so on-demand creation must abort.
    InstanceFactory factory = new InstanceFactory(1000L, false);
    ComponentPool<String> pool = new ComponentPool<>("creation-timeout", factory, new ComplexPoolConfig().setMaxPoolSize(5).setCreationTimeoutMillis(200L));

    pool.startup();
    try {
      Assert.assertThrows(ComponentPoolException.class, pool::getComponent);
    } finally {
      pool.shutdown();
    }
  }
}
