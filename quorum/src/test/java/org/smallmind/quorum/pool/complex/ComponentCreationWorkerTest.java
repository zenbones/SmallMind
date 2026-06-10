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

import org.smallmind.quorum.pool.complex.PoolComponentSupport.InstanceFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives the {@link ComponentCreationWorker} state machine on a single thread by invoking
 * {@link ComponentCreationWorker#run()} and {@link ComponentCreationWorker#abort()} directly in the
 * order that exercises each terminal state, so no real timing or virtual threads are involved.
 */
@Test(groups = "unit")
public class ComponentCreationWorkerTest {

  public void testCompletedBeforeAbortReturnsTheInstance ()
    throws Exception {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("worker", factory);
    ComponentCreationWorker<String> worker = new ComponentCreationWorker<>(pool);

    worker.run();

    Assert.assertFalse(worker.abort(), "abort after a completed creation should report that it was too late");
    Assert.assertSame(worker.getComponentInstance(), factory.instance(0));
    Assert.assertFalse(factory.instance(0).isClosed(), "a completed instance that was not aborted must not be closed");
  }

  public void testAbortBeforeCompletionDiscardsTheLateInstance ()
    throws Exception {

    InstanceFactory factory = new InstanceFactory();
    ComponentPool<String> pool = new ComponentPool<>("worker", factory);
    ComponentCreationWorker<String> worker = new ComponentCreationWorker<>(pool);

    Assert.assertTrue(worker.abort(), "aborting while still running should report a pre-emption");

    worker.run();

    Assert.assertTrue(factory.instance(0).isClosed(), "an instance produced after an abort should be closed and discarded");
  }

  public void testFactoryFailureIsRethrownThroughAbort () {

    InstanceFactory factory = new InstanceFactory(0L, true);
    ComponentPool<String> pool = new ComponentPool<>("worker", factory);
    ComponentCreationWorker<String> worker = new ComponentCreationWorker<>(pool);

    worker.run();

    Assert.assertThrows(IllegalStateException.class, worker::abort);
  }
}
