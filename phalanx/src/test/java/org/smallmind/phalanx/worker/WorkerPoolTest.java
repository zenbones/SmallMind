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
package org.smallmind.phalanx.worker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the reusable worker-pool primitives: the {@link WorkManager} lifecycle guard and
 * start/dispatch path, and the differing hand-off semantics of the two {@link WorkQueue}
 * implementations ({@link BlockingWorkQueue} buffers, {@link TransferringWorkQueue} requires a
 * waiting consumer).
 */
@Test(groups = "unit")
public class WorkerPoolTest {

  @BeforeClass
  public void beforeClass () {

    //  WorkManager.execute and Worker.run record Claxon metrics, which resolve the registry from the
    //  per-application context; establish one on this thread (worker threads forked later inherit it).
    new PerApplicationContext();
  }

  @Test(expectedExceptions = WorkManagerException.class)
  public void testExecuteBeforeStartUpIsRejected ()
    throws Throwable {

    new WorkManager<>(CollectingWorker.class, 1).execute("work");
  }

  @Test
  public void testConcurrencyLimitIsExposed () {

    Assert.assertEquals(new WorkManager<>(CollectingWorker.class, 3).getConcurrencyLimit(), 3);
  }

  @Test
  public void testStartedPoolProcessesSubmittedWork ()
    throws Throwable {

    List<String> collected = new CopyOnWriteArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    WorkManager<CollectingWorker, String> manager = new WorkManager<>(CollectingWorker.class, 2);

    manager.startUp(workQueue -> new CollectingWorker(workQueue, collected, latch));
    try {
      manager.execute("payload");

      Assert.assertTrue(latch.await(5, TimeUnit.SECONDS), "worker did not process the submitted item in time");
      Assert.assertTrue(collected.contains("payload"));
    } finally {
      manager.shutDown();
    }
  }

  @Test
  public void testBlockingWorkQueueBuffersWithoutAConsumer ()
    throws InterruptedException {

    BlockingWorkQueue<String> workQueue = new BlockingWorkQueue<>();

    Assert.assertTrue(workQueue.offer("a", 1, TimeUnit.SECONDS));
    Assert.assertEquals(workQueue.poll(1, TimeUnit.SECONDS), "a");
    Assert.assertNull(workQueue.poll(50, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testTransferringWorkQueueRequiresAConsumer ()
    throws InterruptedException {

    TransferringWorkQueue<String> workQueue = new TransferringWorkQueue<>();

    Assert.assertFalse(workQueue.offer("a", 100, TimeUnit.MILLISECONDS));
    Assert.assertNull(workQueue.poll(50, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testBoundedBlockingWorkQueueRejectsWhenFull ()
    throws InterruptedException {

    BlockingWorkQueue<String> workQueue = new BlockingWorkQueue<>(1);

    Assert.assertTrue(workQueue.offer("a", 1, TimeUnit.SECONDS));
    Assert.assertFalse(workQueue.offer("b", 100, TimeUnit.MILLISECONDS));
    Assert.assertEquals(workQueue.poll(1, TimeUnit.SECONDS), "a");
  }

  @Test
  public void testDoubleStartUpIsIdempotent ()
    throws Throwable {

    List<String> collected = new CopyOnWriteArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    WorkManager<CollectingWorker, String> manager = new WorkManager<>(CollectingWorker.class, 1);

    manager.startUp(workQueue -> new CollectingWorker(workQueue, collected, latch));
    try {
      manager.startUp(workQueue -> new CollectingWorker(workQueue, collected, latch));
      manager.execute("payload");

      Assert.assertTrue(latch.await(5, TimeUnit.SECONDS), "worker did not process the submitted item in time");
    } finally {
      manager.shutDown();
    }
  }

  @Test
  public void testShutDownWithoutStartUpIsSafe ()
    throws InterruptedException {

    new WorkManager<>(CollectingWorker.class, 1).shutDown();
  }

  @Test
  public void testExecuteRetriesUntilQueueAccepts ()
    throws Throwable {

    RejectOnceWorkQueue<String> workQueue = new RejectOnceWorkQueue<>();
    WorkManager<CollectingWorker, String> manager = new WorkManager<>(CollectingWorker.class, 1, workQueue);

    manager.startUp(queue -> new CollectingWorker(queue, new CopyOnWriteArrayList<>(), new CountDownLatch(1)));
    try {
      manager.execute("payload");

      Assert.assertTrue(workQueue.getOfferCount() >= 2, "execute should have retried the rejected first offer");
    } finally {
      manager.shutDown();
    }
  }

  public static class CollectingWorker extends Worker<String> {

    private final List<String> collected;
    private final CountDownLatch latch;

    public CollectingWorker (WorkQueue<String> workQueue, List<String> collected, CountDownLatch latch) {

      super(workQueue);

      this.collected = collected;
      this.latch = latch;
    }

    @Override
    public void engageWork (String transfer) {

      collected.add(transfer);
      latch.countDown();
    }

    @Override
    public void close () {

    }
  }

  private static class RejectOnceWorkQueue<E> implements WorkQueue<E> {

    private final AtomicBoolean firstOffer = new AtomicBoolean(true);
    private final AtomicInteger offerCount = new AtomicInteger(0);

    public int getOfferCount () {

      return offerCount.get();
    }

    @Override
    public boolean offer (E item, long timeout, TimeUnit unit) {

      offerCount.incrementAndGet();

      return !firstOffer.compareAndSet(true, false);
    }

    @Override
    public E poll (long timeout, TimeUnit unit)
      throws InterruptedException {

      Thread.sleep(unit.toMillis(timeout));

      return null;
    }
  }
}
