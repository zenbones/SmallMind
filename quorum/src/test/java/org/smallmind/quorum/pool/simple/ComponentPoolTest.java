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
package org.smallmind.quorum.pool.simple;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ComponentPoolTest {

  public void testCreatesUpToCapThenReusesFreedComponents ()
    throws ComponentPoolException {

    CountingFactory factory = new CountingFactory();
    ComponentPool<CountingComponent> pool = new ComponentPool<>(factory, new SimplePoolConfig().setMaxPoolSize(2));

    CountingComponent first = pool.getComponent();
    CountingComponent second = pool.getComponent();

    Assert.assertEquals(factory.getCreated(), 2);
    Assert.assertEquals(pool.poolSize(), 2);

    pool.returnComponent(first);
    Assert.assertEquals(pool.freeSize(), 1);

    CountingComponent reused = pool.getComponent();

    Assert.assertSame(reused, first, "a free component should be reused rather than freshly created");
    Assert.assertEquals(factory.getCreated(), 2);

    // keep the unused reference meaningful to the reader
    Assert.assertNotSame(second, reused);
  }

  public void testReturnTerminatesWhenPoolShrunkBelowOccupancy ()
    throws ComponentPoolException {

    CountingFactory factory = new CountingFactory();
    SimplePoolConfig config = new SimplePoolConfig().setMaxPoolSize(2);
    ComponentPool<CountingComponent> pool = new ComponentPool<>(factory, config);

    CountingComponent first = pool.getComponent();
    CountingComponent second = pool.getComponent();

    // Tighten the cap at runtime; the pool should shed components on return rather than re-pool them.
    config.setMaxPoolSize(1);

    pool.returnComponent(first);
    Assert.assertTrue(first.isTerminated(), "a returned component over the tightened cap should be terminated");

    pool.returnComponent(second);
    Assert.assertFalse(second.isTerminated(), "the last returned component is within the cap and should be re-pooled");
    Assert.assertEquals(pool.freeSize(), 1);
  }

  public void testCloseTerminatesFreeComponents ()
    throws Exception {

    CountingFactory factory = new CountingFactory();
    ComponentPool<CountingComponent> pool = new ComponentPool<>(factory, new SimplePoolConfig().setMaxPoolSize(2));

    CountingComponent component = pool.getComponent();

    pool.returnComponent(component);
    pool.close();

    Assert.assertTrue(component.isTerminated(), "closing the pool should terminate components left on the free list");
  }

  @Test(expectedExceptions = ComponentPoolException.class)
  public void testClosedPoolRejectsAcquisition ()
    throws Exception {

    ComponentPool<CountingComponent> pool = new ComponentPool<>(new CountingFactory());

    pool.close();
    pool.getComponent();
  }

  @Test(expectedExceptions = ComponentPoolException.class)
  public void testAcquireTimesOutWhenFullyBookedWithAPositiveWait ()
    throws ComponentPoolException {

    ComponentPool<CountingComponent> pool = new ComponentPool<>(new CountingFactory(), new SimplePoolConfig().setMaxPoolSize(1).setAcquireWaitTimeMillis(200L));

    pool.getComponent();
    // The single component is checked out; this call blocks for the wait window and then throws.
    pool.getComponent();
  }

  @Test(expectedExceptions = ComponentPoolException.class)
  public void testZeroAcquireWaitFailsImmediatelyWhenFullyBooked ()
    throws ComponentPoolException {

    // The default acquire wait of zero means "do not block": a full pool fails fast rather than
    // waiting for a return.
    ComponentPool<CountingComponent> pool = new ComponentPool<>(new CountingFactory(), new SimplePoolConfig().setMaxPoolSize(1));

    pool.getComponent();
    pool.getComponent();
  }

  public void testBlockedAcquisitionResumesWhenAComponentIsReturned ()
    throws Exception {

    CountingFactory factory = new CountingFactory();
    // A positive acquire-wait blocks the caller until a component is returned within the window.
    final ComponentPool<CountingComponent> pool = new ComponentPool<>(factory, new SimplePoolConfig().setMaxPoolSize(1).setAcquireWaitTimeMillis(5000L));
    final CountingComponent borrowed = pool.getComponent();
    final CountDownLatch acquirerStarted = new CountDownLatch(1);
    final AtomicReference<CountingComponent> acquired = new AtomicReference<>();

    Thread acquirer = new Thread(() -> {
      acquirerStarted.countDown();
      try {
        acquired.set(pool.getComponent());
      } catch (ComponentPoolException componentPoolException) {
        // leave acquired null so the assertion below fails rather than hanging
      }
    });
    acquirer.setDaemon(true);
    acquirer.start();

    acquirerStarted.await();
    Thread.sleep(250L);

    pool.returnComponent(borrowed);
    acquirer.join(2000L);

    Assert.assertSame(acquired.get(), borrowed, "the blocked acquirer should receive the returned component once it is freed");
  }

  private static class CountingComponent implements PooledComponent {

    private boolean terminated = false;

    private boolean isTerminated () {

      return terminated;
    }

    @Override
    public void terminate () {

      terminated = true;
    }
  }

  private static class CountingFactory implements ComponentFactory<CountingComponent> {

    private int created = 0;

    private int getCreated () {

      return created;
    }

    @Override
    public CountingComponent createComponent () {

      created++;

      return new CountingComponent();
    }
  }
}
