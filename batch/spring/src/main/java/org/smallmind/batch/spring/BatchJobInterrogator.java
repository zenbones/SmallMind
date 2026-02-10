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
