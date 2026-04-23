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
 * A {@link DeconstructionFuse} that fires when a component has been processing (checked out
 * by a caller) longer than {@link ComplexPoolConfig#getMaxProcessingTimeSeconds()}.
 * <p>
 * Unlike the idle and lease fuses, the processing fuse is <em>prejudicial</em>: it
 * force-terminates a component even while it is held by a caller.
 * <p>
 * A generation counter prevents stale ignitions. Each call to {@link #serve()} increments
 * both the global generation and the {@code generationServed} snapshot. Each call to
 * {@link #free()} increments only the global generation. When the background timer fires,
 * {@link #ignite()} compares the two: if they match, the component is still on the same
 * serve cycle and ignition proceeds; if they differ, the component was returned and
 * re-acquired between registration and firing, so the ignition is silently skipped.
 * <p>
 * When existential awareness is enabled and a valid stack trace is available, the trace is
 * logged as a warning to aid diagnosis of the hung caller.
 */
public class MaxProcessingTimeDeconstructionFuse extends DeconstructionFuse {

  private final ComponentPool<?> componentPool;
  private final AtomicInteger generation = new AtomicInteger(0);
  private final AtomicInteger generationServed = new AtomicInteger(0);

  /**
   * Creates the fuse for processing-timeout tracking.
   *
   * @param componentPool             the pool whose configuration supplies the processing
   *                                  timeout
   * @param deconstructionQueue       the queue that will fire this fuse when the deadline
   *                                  elapses
   * @param deconstructionCoordinator the coordinator that acts when this fuse ignites
   */
  protected MaxProcessingTimeDeconstructionFuse (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    super(deconstructionQueue, deconstructionCoordinator);

    this.componentPool = componentPool;
  }

  /**
   * Returns {@code true} because a processing-timeout ignition should force-terminate the
   * component even if it is still held by a caller.
   *
   * @return {@code true}
   */
  @Override
  public boolean isPrejudicial () {

    return true;
  }

  /**
   * Called when the component is returned to the free queue.
   * <p>
   * Advances the generation counter so that any pending ignition from the previous serve
   * cycle will be silently discarded, then cancels the scheduled ignition.
   */
  @Override
  public synchronized void free () {

    generation.incrementAndGet();
    abort();
  }

  /**
   * Called when the component is handed to a caller.
   * <p>
   * Advances the generation counter and snapshots the new value in {@code generationServed},
   * then schedules ignition at {@code now + maxProcessingTimeSeconds}.
   */
  @Override
  public void serve () {

    generationServed.set(generation.incrementAndGet());
    setIgnitionTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(componentPool.getComplexPoolConfig().getMaxProcessingTimeSeconds()));
  }

  /**
   * Fires the fuse only when the generation recorded at schedule time matches the current
   * generation, confirming that the component is still on the same uninterrupted serve cycle.
   * <p>
   * When a valid existential stack trace is available, it is logged at {@code WARN} level
   * to identify the thread that is holding the component.
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
