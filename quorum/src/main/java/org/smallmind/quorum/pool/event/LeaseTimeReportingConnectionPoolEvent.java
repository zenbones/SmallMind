package org.smallmind.quorum.pool.event;

import org.smallmind.quorum.pool.ConnectionPool;

public class LeaseTimeReportingConnectionPoolEvent extends ConnectionPoolEvent {

   private long leaseTimeNanos;

   public LeaseTimeReportingConnectionPoolEvent (ConnectionPool connectionPool, long leaseTimeNanos) {

      super(connectionPool);

      this.leaseTimeNanos = leaseTimeNanos;
   }

   public long getLeaseTimeNanos () {

      return leaseTimeNanos;
   }
}
