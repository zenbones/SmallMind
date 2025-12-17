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
 * Fuse that ignites after the maximum lease time for a component has elapsed.
 */
public class MaxLeaseTimeDeconstructionFuse extends DeconstructionFuse {

  /**
   * Constructs the fuse and immediately schedules ignition based on lease time.
   *
   * @param componentPool             owning pool
   * @param deconstructionQueue       queue for scheduling ignition
   * @param deconstructionCoordinator coordinator invoked on ignition
   */
  public MaxLeaseTimeDeconstructionFuse (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    super(deconstructionQueue, deconstructionCoordinator);

    setIgnitionTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds()));
  }

  /**
   * Lease expirations are not prejudicial; they represent natural lease ends.
   */
  @Override
  public boolean isPrejudicial () {

    return false;
  }

  /**
   * Lease fuse does not require action on free because it ignites based solely on absolute time.
   */
  @Override
  public void free () {

  }

  /**
   * Lease fuse does not change scheduling on serve because ignition was set at construction.
   */
  @Override
  public void serve () {

  }
}
