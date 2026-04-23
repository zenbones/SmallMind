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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Manages the set of {@link DeconstructionFuse} instances attached to a single
 * {@link ComponentPin} and arbitrates which fuse wins when multiple fuses are active.
 * <p>
 * At construction time the coordinator inspects the pool configuration and creates the
 * applicable fuses:
 * <ul>
 *   <li>{@link MaxLeaseTimeDeconstructionFuse} when {@code maxLeaseTimeSeconds > 0};</li>
 *   <li>{@link MaxIdleTimeDeconstructionFuse} when {@code maxIdleTimeSeconds > 0};</li>
 *   <li>{@link MaxProcessingTimeDeconstructionFuse} when {@code maxProcessingTimeSeconds > 0}.</li>
 * </ul>
 * When a fuse fires ({@link #ignite(DeconstructionFuse, boolean)}), the coordinator:
 * <ol>
 *   <li>Uses an {@link AtomicBoolean} to guarantee at most one ignition is acted upon.</li>
 *   <li>Cancels all other fuses via their individual {@link DeconstructionFuse#abort()}.</li>
 *   <li>Calls {@link ComponentPin#kaboom(boolean)} to remove the pin from the pool.</li>
 * </ol>
 * The {@link #free()} and {@link #serve()} calls are forwarded to all fuses so each can
 * update its own scheduling in response to the pin's state change.
 */
public class DeconstructionCoordinator {

  private final ComponentPin<?> componentPin;
  private final List<DeconstructionFuse> fuseList;
  private final AtomicBoolean terminated = new AtomicBoolean(false);

  /**
   * Constructs a coordinator for {@code componentPin} and creates the applicable fuses
   * based on the pool's configuration.
   *
   * @param componentPool       the pool whose configuration determines which fuses to create
   * @param deconstructionQueue the shared queue that schedules fuse ignitions
   * @param componentPin        the pin whose lifecycle the fuses guard
   */
  public DeconstructionCoordinator (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, ComponentPin<?> componentPin) {

    this.componentPin = componentPin;

    fuseList = new LinkedList<>();

    if (componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds() > 0) {
      fuseList.add(new MaxLeaseTimeDeconstructionFuse(componentPool, deconstructionQueue, this));
    }
    if (componentPool.getComplexPoolConfig().getMaxIdleTimeSeconds() > 0) {
      fuseList.add(new MaxIdleTimeDeconstructionFuse(componentPool, deconstructionQueue, this));
    }
    if (componentPool.getComplexPoolConfig().getMaxProcessingTimeSeconds() > 0) {
      fuseList.add(new MaxProcessingTimeDeconstructionFuse(componentPool, deconstructionQueue, this));
    }
  }

  /**
   * Returns the existential stack trace held by the associated {@link ComponentPin}, providing
   * context about where the component was acquired.
   *
   * @return the stack trace of the acquiring thread, or {@code null} if not tracked
   */
  public StackTraceElement[] getExistentialStackTrace () {

    return componentPin.getExistentialStackTrace();
  }

  /**
   * Notifies all fuses that the component has been placed on the free queue.
   * <p>
   * Fuses that track idle time will schedule ignition; fuses that track processing time
   * will cancel any pending ignition.
   */
  public void free () {

    for (DeconstructionFuse fuse : fuseList) {
      fuse.free();
    }
  }

  /**
   * Notifies all fuses that the component has been handed to a caller.
   * <p>
   * Fuses that track processing time will schedule ignition; fuses that track idle time
   * will cancel any pending ignition.
   */
  public void serve () {

    for (DeconstructionFuse fuse : fuseList) {
      fuse.serve();
    }
  }

  /**
   * Cancels all fuses without triggering pin removal.
   * <p>
   * Called by {@link ComponentPin#fizzle()} when the pool manager terminates the pin
   * through its normal path and needs to prevent a concurrent fuse from also trying to
   * remove it. Operates at most once via the {@code terminated} flag.
   */
  public void abort () {

    if (terminated.compareAndSet(false, true)) {
      shutdown(null);
    }
  }

  /**
   * Called by a {@link DeconstructionFuse} when its timer expires.
   * <p>
   * Uses an {@link AtomicBoolean} so that only the first fuse to call this method wins.
   * Cancels all other fuses, logs the igniting fuse's class name, and calls
   * {@link ComponentPin#kaboom(boolean)} to remove the pin from the pool.
   *
   * @param ignitionFuse  the fuse that triggered this ignition
   * @param withPrejudice {@code true} if the fuse is prejudicial and the pin should be
   *                      force-removed even while processing
   */
  public void ignite (DeconstructionFuse ignitionFuse, boolean withPrejudice) {

    if (terminated.compareAndSet(false, true)) {
      LoggerManager.getLogger(DeconstructionCoordinator.class).debug("ComponentPin being terminated due to fuse(%s) ignition", ignitionFuse.getClass().getSimpleName());
      shutdown(ignitionFuse);
      componentPin.kaboom(withPrejudice);
    }
  }

  /**
   * Aborts every fuse in the list except {@code ignitionFuse}.
   *
   * @param ignitionFuse the fuse that fired, which should not be aborted; {@code null} to
   *                     abort all fuses
   */
  private void shutdown (DeconstructionFuse ignitionFuse) {

    for (DeconstructionFuse fuse : fuseList) {
      if (!fuse.equals(ignitionFuse)) {
        fuse.abort();
      }
    }
  }
}
