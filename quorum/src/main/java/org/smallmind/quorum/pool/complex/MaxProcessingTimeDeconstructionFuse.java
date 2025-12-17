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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Fuse that ignites when a component has been processing longer than the configured maximum.
 */
public class MaxProcessingTimeDeconstructionFuse extends DeconstructionFuse {

  private final ComponentPool<?> componentPool;
  private final AtomicInteger generation = new AtomicInteger(0);
  private final AtomicInteger generationServed = new AtomicInteger(0);

  /**
   * Creates the fuse for the specified pool and coordinator.
   *
   * @param componentPool             owning pool
   * @param deconstructionQueue       queue for scheduling ignition
   * @param deconstructionCoordinator coordinator invoked on ignition
   */
  protected MaxProcessingTimeDeconstructionFuse (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    super(deconstructionQueue, deconstructionCoordinator);

    this.componentPool = componentPool;
  }

  /**
   * Processing timeout is prejudicial because the component is hung.
   */
  @Override
  public boolean isPrejudicial () {

    return true;
  }

  /**
   * Cancels any pending ignition by advancing generation and aborting.
   */
  @Override
  public synchronized void free () {

    generation.incrementAndGet();
    abort();
  }

  /**
   * Schedules ignition based on the maximum processing time.
   */
  @Override
  public void serve () {

    generationServed.set(generation.incrementAndGet());
    setIgnitionTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(componentPool.getComplexPoolConfig().getMaxProcessingTimeSeconds()));
  }

  /**
   * Ignites only if the same generation was served, logging stack traces when available.
   */
  @Override
  public synchronized void ignite () {

    if (generationServed.get() == generation.get()) {

      StackTraceElement[] stackTraceElements = getExistentialStackTrace();

      super.ignite();

      if ((stackTraceElements != null) && (stackTraceElements.length > 0)) {
        LoggerManager.getLogger(MaxProcessingTimeDeconstructionFuse.class).warn(Arrays.toString(getExistentialStackTrace()));
      }
    }
  }
}
