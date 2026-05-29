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
package org.smallmind.nutsnbolts.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ComponentModulatorTest {

  public void testDefaultConstructorStartsInStoppedState () {

    ComponentModulator modulator = new ComponentModulator();

    Assert.assertEquals(modulator.get(), ComponentStatus.STOPPED);
  }

  public void testExplicitInitialStatusIsReported () {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.INITIALIZING);

    Assert.assertEquals(modulator.get(), ComponentStatus.INITIALIZING);
  }

  public void testSetReplacesStatus () {

    ComponentModulator modulator = new ComponentModulator();

    modulator.set(ComponentStatus.STARTED);

    Assert.assertEquals(modulator.get(), ComponentStatus.STARTED);
  }

  public void testCompareAndSetSucceedsWhenExpectedMatches () {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STOPPED);

    Assert.assertTrue(modulator.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING));
    Assert.assertEquals(modulator.get(), ComponentStatus.STARTING);
  }

  public void testCompareAndSetFailsWhenExpectedDoesNotMatch () {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STOPPED);

    Assert.assertFalse(modulator.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STARTING));
    Assert.assertEquals(modulator.get(), ComponentStatus.STOPPED);
  }

  public void testAwaitInReturnsImmediatelyWhenAlreadyInTargetSet ()
    throws InterruptedException {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STARTED);

    ComponentStatus observed = modulator.awaitIn(ComponentStatus.STARTED, ComponentStatus.STARTING);

    Assert.assertEquals(observed, ComponentStatus.STARTED);
  }

  public void testAwaitNotInReturnsImmediatelyWhenAlreadyOutsideExclusionSet ()
    throws InterruptedException {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STARTED);

    ComponentStatus observed = modulator.awaitNotIn(ComponentStatus.STOPPED, ComponentStatus.STOPPING);

    Assert.assertEquals(observed, ComponentStatus.STARTED);
  }

  public void testAwaitInBlocksUntilSetMovesIntoTargetState ()
    throws InterruptedException {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STOPPED);
    CountDownLatch waiterEntered = new CountDownLatch(1);
    AtomicReference<ComponentStatus> observed = new AtomicReference<>();
    AtomicReference<Throwable> waiterFailure = new AtomicReference<>();

    Thread waiter = new Thread(() -> {
      try {
        waiterEntered.countDown();
        observed.set(modulator.awaitIn(ComponentStatus.STARTED));
      } catch (Throwable throwable) {
        waiterFailure.set(throwable);
      }
    });
    waiter.start();

    Assert.assertTrue(waiterEntered.await(2, TimeUnit.SECONDS));
    Thread.sleep(50);
    modulator.set(ComponentStatus.STARTED);
    waiter.join(2000);

    Assert.assertNull(waiterFailure.get());
    Assert.assertEquals(observed.get(), ComponentStatus.STARTED);
  }

  public void testAwaitNotInBlocksUntilStatusLeavesExclusionSet ()
    throws InterruptedException {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STOPPED);
    CountDownLatch waiterEntered = new CountDownLatch(1);
    AtomicReference<ComponentStatus> observed = new AtomicReference<>();

    Thread waiter = new Thread(() -> {
      try {
        waiterEntered.countDown();
        observed.set(modulator.awaitNotIn(ComponentStatus.STOPPED));
      } catch (InterruptedException ignored) {
      }
    });
    waiter.start();

    Assert.assertTrue(waiterEntered.await(2, TimeUnit.SECONDS));
    Thread.sleep(50);
    modulator.set(ComponentStatus.STARTING);
    waiter.join(2000);

    Assert.assertEquals(observed.get(), ComponentStatus.STARTING);
  }

  public void testCompareAndSetWakesAwaitersOnSuccessfulTransition ()
    throws InterruptedException {

    ComponentModulator modulator = new ComponentModulator(ComponentStatus.STOPPED);
    CountDownLatch waiterEntered = new CountDownLatch(1);
    AtomicReference<ComponentStatus> observed = new AtomicReference<>();

    Thread waiter = new Thread(() -> {
      try {
        waiterEntered.countDown();
        observed.set(modulator.awaitIn(ComponentStatus.STARTED));
      } catch (InterruptedException ignored) {
      }
    });
    waiter.start();

    Assert.assertTrue(waiterEntered.await(2, TimeUnit.SECONDS));
    Thread.sleep(50);
    Assert.assertTrue(modulator.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTED));
    waiter.join(2000);

    Assert.assertEquals(observed.get(), ComponentStatus.STARTED);
  }
}
