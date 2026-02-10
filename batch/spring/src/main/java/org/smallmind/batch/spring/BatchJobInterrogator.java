package org.smallmind.batch.spring;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

public class BatchJobInterrogator {

  private DataSource dataSource;
  private String schemaName;

  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  public void setSchemaName (String schemaName) {

    this.schemaName = schemaName;
  }

  public boolean awaitQuiescence (long timeout, TimeUnit timeUnit)
    throws SQLException, InterruptedException {

    long total = timeUnit.toMillis(timeout);
    long end = System.currentTimeMillis() + total;
    long pulse = Math.max(1000, total / 10);
    long remaining;

    while ((remaining = end - System.currentTimeMillis()) > 0) {
      if (uncompletedCount() == 0) {

        return true;
      } else {

        Thread.sleep(Math.min(pulse, remaining));
      }
    }

    return false;
  }

  private long uncompletedCount ()
    throws SQLException {

    try (Connection connection = dataSource.getConnection()) {

      connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      connection.setAutoCommit(false);
      ResultSet uncompletedJobResultSet;

      uncompletedJobResultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery("select cpount(*) from " + schemaName + ".BATCH_JOB_EXECUTION where STATUS != 'COMPLETED'");
      if (uncompletedJobResultSet.next()) {

        return uncompletedJobResultSet.getLong(1);
      }

      return 0;
    }
  }
}
