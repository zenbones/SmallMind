/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
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

public class DeconstructionCoordinator {

  private final ComponentPin<?> componentPin;
  private final List<DeconstructionFuse> fuseList;
  private final AtomicBoolean terminated = new AtomicBoolean(false);

  public DeconstructionCoordinator (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, ComponentPin<?> componentPin) {

    this.componentPin = componentPin;

    fuseList = new LinkedList<DeconstructionFuse>();

    if (componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds() > 0) {
      fuseList.add(new MaxLeaseTimeDeconstructionFuse(componentPool, deconstructionQueue, this));
    }
    if (componentPool.getComplexPoolConfig().getMaxIdleTimeSeconds() > 0) {
      fuseList.add(new MaxIdleTimeDeconstructionFuse(componentPool, deconstructionQueue, this));
    }
    if (componentPool.getComplexPoolConfig().getUnReturnedElementTimeoutSeconds() > 0) {
      fuseList.add(new UnReturnedElementTimeoutDeconstructionFuse(componentPool, deconstructionQueue, this));
    }
  }

  public void free () {

    for (DeconstructionFuse fuse : fuseList) {
      fuse.free();
    }
  }

  public void serve () {

    for (DeconstructionFuse fuse : fuseList) {
      fuse.serve();
    }
  }

  public void abort () {

    if (terminated.compareAndSet(false, true)) {
      shutdown(null);
    }
  }

  public void ignite (DeconstructionFuse ignitionFuse, boolean withPrejudice) {

    if (terminated.compareAndSet(false, true)) {
      LoggerManager.getLogger(DeconstructionCoordinator.class).info("ComponentPin being terminated due to fuse(%s) ignition", ignitionFuse.getClass().getSimpleName());
      shutdown(ignitionFuse);
      componentPin.kaboom(withPrejudice);
    }
  }

  private void shutdown (DeconstructionFuse ignitionFuse) {

    for (DeconstructionFuse fuse : fuseList) {
      if (!fuse.equals(ignitionFuse)) {
        fuse.abort();
      }
    }
  }
}
