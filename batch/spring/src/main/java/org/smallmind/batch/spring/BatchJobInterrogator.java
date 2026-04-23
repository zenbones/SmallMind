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
package org.smallmind.batch.spring;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

/**
 * Queries Spring Batch metadata tables directly via JDBC to determine when all job executions
 * have quiesced.
 * <p>
 * Unlike {@link BatchJobWatcher}, this class bypasses the {@code JobRepository} API and reads
 * the {@code BATCH_JOB_EXECUTION} table directly, making it suitable for use outside a Spring
 * Batch application context.
 */
public class BatchJobInterrogator {

  private DataSource dataSource;
  private String schemaName;

  /**
   * Sets the data source through which the batch metadata tables are accessed.
   *
   * @param dataSource the JDBC data source to use
   */
  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  /**
   * Sets the schema that qualifies the {@code BATCH_JOB_EXECUTION} table name.
   *
   * @param schemaName the database schema name
   */
  public void setSchemaName (String schemaName) {

    this.schemaName = schemaName;
  }

  /**
   * Blocks until no non-completed job executions remain, or the timeout elapses.
   * <p>
   * Polls at an interval of at most one-tenth of the total timeout, with a minimum of one
   * second between checks.
   *
   * @param timeout  the maximum time to wait before returning {@code false}
   * @param timeUnit the unit for {@code timeout}
   * @return {@code true} if quiescence was confirmed before the deadline, {@code false} if timed out
   * @throws SQLException         if a JDBC error occurs while querying the metadata table
   * @throws InterruptedException if the polling thread is interrupted between checks
   */
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

  /**
   * Executes a COUNT query against {@code BATCH_JOB_EXECUTION} and returns the number of rows
   * whose {@code STATUS} column is not {@code 'COMPLETED'}.
   *
   * @return the count of non-completed executions
   * @throws SQLException if the query cannot be prepared or executed
   */
  private long uncompletedCount ()
    throws SQLException {

    try (Connection connection = dataSource.getConnection()) {

      connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      connection.setAutoCommit(false);
      ResultSet uncompletedJobResultSet;

      uncompletedJobResultSet = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery("select count(*) from " + schemaName + ".BATCH_JOB_EXECUTION where STATUS != 'COMPLETED'");
      if (uncompletedJobResultSet.next()) {

        return uncompletedJobResultSet.getLong(1);
      }

      return 0;
    }
  }
}
