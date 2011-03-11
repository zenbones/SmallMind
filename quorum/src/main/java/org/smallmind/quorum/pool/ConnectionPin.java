/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.quorum.pool;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.Existential;

public class ConnectionPin<C> implements Existential {

  private enum State {

    FREE, SERVED, CLOSED
  }

  private ConnectionPool connectionPool;
  private DeconstructionWorker deconstructionWorker;
  private ConnectionInstance<C> connectionInstance;
  private State state;
  private boolean commissioned = true;
  private boolean reportLeaseTimeNanos;
  private long leaseStartNanos;
  private Integer originatingIndex;

  public ConnectionPin (ConnectionPool<C> connectionPool, Integer originatingIndex, ConnectionInstance<C> connectionInstance, boolean reportLeaseTimeNanos, int maxIdleTimeSeconds, int maxLeaseTimeSeconds, int unreturnedConnectionTimeoutSeconds) {

    Thread workerThread;
    LinkedList<DeconstructionFuse> fuseList;

    this.connectionPool = connectionPool;
    this.originatingIndex = originatingIndex;
    this.connectionInstance = connectionInstance;
    this.reportLeaseTimeNanos = reportLeaseTimeNanos;

    state = State.FREE;

    connectionInstance.addConnectionInstanceEventListener(connectionPool);

    fuseList = new LinkedList<DeconstructionFuse>();

    if (maxLeaseTimeSeconds > 0) {
      fuseList.add(new MaxLeaseTimeDeconstructionFuse(maxLeaseTimeSeconds));
    }

    if (maxIdleTimeSeconds > 0) {
      fuseList.add(new MaxIdleTimeDeconstructionFuse(maxIdleTimeSeconds));
    }

    if (unreturnedConnectionTimeoutSeconds > 0) {
      fuseList.add(new UnreturnedConnectionTimeoutDeconstructionFuse(unreturnedConnectionTimeoutSeconds));
    }

    if (!fuseList.isEmpty()) {
      deconstructionWorker = new DeconstructionWorker<C>(connectionPool, this, fuseList);
      workerThread = new Thread(deconstructionWorker);
      workerThread.setDaemon(true);
      workerThread.start();

      deconstructionWorker.free();
    }
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return ((Existential)connectionInstance).getExistentialStackTrace();
  }

  public Integer getOriginatingIndex () {

    return originatingIndex;
  }

  protected void abort () {

    if (deconstructionWorker != null) {
      deconstructionWorker.abort();
    }
  }

  public void decommission () {

    commissioned = false;
  }

  public boolean isCommissioned () {

    return commissioned;
  }

  public boolean isFree () {

    return state.equals(State.FREE);
  }

  public boolean isServed () {

    return state.equals(State.SERVED);
  }

  public boolean isClosed () {

    return state.equals(State.CLOSED);
  }

  public boolean contains (ConnectionInstance connectionInstance) {

    return this.connectionInstance == connectionInstance;
  }

  public boolean validate () {

    return connectionInstance.validate();
  }

  public C serve ()
    throws Exception {

    C connection;

    if (!state.equals(State.FREE)) {
      throw new ConnectionPoolException("An attempt to serve this connection while in state(%s)", state);
    }

    connection = connectionInstance.serve();
    state = State.SERVED;

    if (deconstructionWorker != null) {
      deconstructionWorker.serve();
    }

    if (reportLeaseTimeNanos) {
      leaseStartNanos = System.nanoTime();
    }

    return connection;
  }

  public void free () {

    state = State.FREE;

    if (reportLeaseTimeNanos) {
      connectionPool.reportConnectionLeaseTimeNanos(System.nanoTime() - leaseStartNanos);
    }

    if (deconstructionWorker != null) {
      deconstructionWorker.free();
    }
  }

  public void close ()
    throws Exception {

    try {
      if (!state.equals(State.CLOSED)) {
        state = State.CLOSED;
        connectionInstance.close();

        if (reportLeaseTimeNanos) {
          connectionPool.reportConnectionLeaseTimeNanos(System.nanoTime() - leaseStartNanos);
        }
      }
    }
    finally {
      connectionInstance.removeConnectionInstanceEventListener(connectionPool);
    }
  }

  public void finalize () {

    try {
      close();
    }
    catch (Exception exception) {
      ConnectionPoolManager.logError(exception);
    }
    finally {
      abort();
    }
  }
}