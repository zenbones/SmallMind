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

import java.util.concurrent.TimeUnit;

/**
 * A {@link DeconstructionFuse} that fires once the total time since a component was created
 * exceeds {@link ComplexPoolConfig#getMaxLeaseTimeSeconds()}.
 * <p>
 * The ignition time is calculated and registered with the {@link DeconstructionQueue} once
 * at construction, using an absolute wall-clock deadline. Because the limit is absolute, the
 * fuse does nothing in response to the component being returned to the free queue ({@link #free()})
 * or handed to a caller ({@link #serve()}); the deadline ticks regardless of whether the
 * component is in use or idle.
 * <p>
 * Lease-timeout removal is <em>non-prejudicial</em>: when the fuse fires the pin is retired
 * normally and the pool will replace it, but a caller that already holds the component
 * completes its use normally.
 */
public class MaxLeaseTimeDeconstructionFuse extends DeconstructionFuse {

  /**
   * Creates the fuse and immediately schedules its ignition at
   * {@code now + maxLeaseTimeSeconds}.
   *
   * @param componentPool             the pool whose configuration supplies the lease timeout
   * @param deconstructionQueue       the queue that will fire this fuse when the deadline
   *                                  elapses
   * @param deconstructionCoordinator the coordinator that acts when this fuse ignites
   */
  public MaxLeaseTimeDeconstructionFuse (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    super(deconstructionQueue, deconstructionCoordinator);

    setIgnitionTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds()));
  }

  /**
   * Returns {@code false} because a lease-timeout ignition should not force-remove a
   * component that is currently being used by a caller.
   *
   * @return {@code false}
   */
  @Override
  public boolean isPrejudicial () {

    return false;
  }

  /**
   * No-op; the ignition time is absolute and is not affected by whether the component is
   * on the free queue.
   */
  @Override
  public void free () {

  }

  /**
   * No-op; the ignition time is absolute and is not affected by whether the component is
   * being served to a caller.
   */
  @Override
  public void serve () {

  }
}
