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
 * Coordinates multiple {@link DeconstructionFuse} instances attached to a {@link ComponentPin},
 * handling ignition and abort logic.
 */
public class DeconstructionCoordinator {

  private final ComponentPin<?> componentPin;
  private final List<DeconstructionFuse> fuseList;
  private final AtomicBoolean terminated = new AtomicBoolean(false);

  /**
   * Builds fuses based on configuration and attaches them to the pin.
   *
   * @param componentPool       owning pool
   * @param deconstructionQueue queue managing fuse scheduling
   * @param componentPin        pin whose lifecycle is being guarded
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
   * Returns the existential stack trace from the pin.
   *
   * @return stack trace elements or {@code null} if not tracked
   */
  public StackTraceElement[] getExistentialStackTrace () {

    return componentPin.getExistentialStackTrace();
  }

  /**
   * Frees all fuses, typically on component return.
   */
  public void free () {

    for (DeconstructionFuse fuse : fuseList) {
      fuse.free();
    }
  }

  /**
   * Serves all fuses, typically when the component is borrowed.
   */
  public void serve () {

    for (DeconstructionFuse fuse : fuseList) {
      fuse.serve();
    }
  }

  /**
   * Aborts all fuses without igniting termination.
   */
  public void abort () {

    if (terminated.compareAndSet(false, true)) {
      shutdown(null);
    }
  }

  /**
   * Called when a fuse ignites to terminate the component and cancel other fuses.
   *
   * @param ignitionFuse  fuse that triggered ignition
   * @param withPrejudice whether termination should be forced
   */
  public void ignite (DeconstructionFuse ignitionFuse, boolean withPrejudice) {

    if (terminated.compareAndSet(false, true)) {
      LoggerManager.getLogger(DeconstructionCoordinator.class).debug("ComponentPin being terminated due to fuse(%s) ignition", ignitionFuse.getClass().getSimpleName());
      shutdown(ignitionFuse);
      componentPin.kaboom(withPrejudice);
    }
  }

  /**
   * Cancels all fuses except the one that triggered ignition.
   *
   * @param ignitionFuse fuse that ignited, or {@code null} if none
   */
  private void shutdown (DeconstructionFuse ignitionFuse) {

    for (DeconstructionFuse fuse : fuseList) {
      if (!fuse.equals(ignitionFuse)) {
        fuse.abort();
      }
    }
  }
}
