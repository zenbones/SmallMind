/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.pool.connection;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeconstructionCoordinator {

  private final ConnectionPin<?> connectionPin;
  private final List<DeconstructionFuse> fuseList;
  private final AtomicBoolean terminated = new AtomicBoolean(false);

  public DeconstructionCoordinator (ConnectionPool<?> connectionPool, DeconstructionQueue deconstructionQueue, ConnectionPin<?> connectionPin) {

    this.connectionPin = connectionPin;

    fuseList = new LinkedList<DeconstructionFuse>();

    if (connectionPool.getConnectionPoolConfig().getMaxLeaseTimeSeconds() > 0) {
      fuseList.add(new MaxLeaseTimeDeconstructionFuse(connectionPool, deconstructionQueue, this));
    }
    if (connectionPool.getConnectionPoolConfig().getMaxIdleTimeSeconds() > 0) {
      fuseList.add(new MaxIdleTimeDeconstructionFuse(connectionPool, deconstructionQueue, this));
    }
    if (connectionPool.getConnectionPoolConfig().getUnreturnedConnectionTimeoutSeconds() > 0) {
      fuseList.add(new UnreturnedConnectionTimeoutDeconstructionFuse(connectionPool, deconstructionQueue, this));
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
      shutdown(ignitionFuse);
      connectionPin.kaboom(withPrejudice);
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
