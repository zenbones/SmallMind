package org.smallmind.quorum.pool2;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionPin<C> {

  private final ConnectionPool<C> connectionPool;
  private final ConnectionInstance<C> connectionInstance;
  private final AtomicBoolean terminated = new AtomicBoolean(false);

  private DeconstructionCoordinator<C> deconstructionCoordinator;
  private long leaseStartNanos;

  protected ConnectionPin (ConnectionPool<C> connectionPool, ConnectionInstance<C> connectionInstance) {

    LinkedList<DeconstructionFuse<C>> fuseList;

    this.connectionPool = connectionPool;
    this.connectionInstance = connectionInstance;

    fuseList = new LinkedList<DeconstructionFuse<C>>();

    if (connectionPool.getConnectionPoolConfig().getMaxLeaseTimeSeconds() > 0) {
      fuseList.add(new MaxLeaseTimeDeconstructionFuse<C>(connectionPool));
    }

    if (connectionPool.getConnectionPoolConfig().getMaxIdleTimeSeconds() > 0) {
      fuseList.add(new MaxIdleTimeDeconstructionFuse<C>(connectionPool));
    }

    if (connectionPool.getConnectionPoolConfig().getUnreturnedConnectionTimeoutSeconds() > 0) {
      fuseList.add(new UnreturnedConnectionTimeoutDeconstructionFuse<C>(connectionPool));
    }

    if (!fuseList.isEmpty()) {
      deconstructionCoordinator = new DeconstructionCoordinator<C>(this, fuseList);
      deconstructionCoordinator.free();
    }
  }

  protected ConnectionInstance<C> getConnectionInstance () {

    return connectionInstance;
  }

  protected C serve ()
    throws Exception {

    try {

      return connectionInstance.serve();
    }
    finally {
      if (deconstructionCoordinator != null) {
        deconstructionCoordinator.serve();
      }

      if (connectionPool.getConnectionPoolConfig().isReportLeaseTimeNanos()) {
        leaseStartNanos = System.nanoTime();
      }
    }
  }

  protected void free () {

    if (connectionPool.getConnectionPoolConfig().isReportLeaseTimeNanos()) {
      connectionPool.reportConnectionLeaseTimeNanos(System.nanoTime() - leaseStartNanos);
    }

    if (deconstructionCoordinator != null) {
      deconstructionCoordinator.free();
    }
  }

  protected boolean isTerminated () {

    return terminated.get();
  }

  protected void fizzle () {

    if (terminated.compareAndSet(false, true)) {
      if (deconstructionCoordinator != null) {
        deconstructionCoordinator.abort();
      }
    }
  }

  protected void kaboom () {

    if (terminated.compareAndSet(false, true)) {
      connectionPool.removePin(this);
    }
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return connectionInstance.getExistentialStackTrace();
  }
}
