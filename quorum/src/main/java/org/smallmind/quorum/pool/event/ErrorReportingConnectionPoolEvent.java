package org.smallmind.quorum.pool.event;

import org.smallmind.quorum.pool.ConnectionPool;

public class ErrorReportingConnectionPoolEvent extends ConnectionPoolEvent {

   private Exception exception;

   public ErrorReportingConnectionPoolEvent (ConnectionPool connectionPool, Exception exception) {

      super(connectionPool);

      this.exception = exception;
   }

   public Exception getException () {

      return exception;
   }
}
