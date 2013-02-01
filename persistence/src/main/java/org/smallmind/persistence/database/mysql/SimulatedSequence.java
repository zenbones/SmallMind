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
package org.smallmind.persistence.database.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.smallmind.persistence.database.Sequence;
import org.smallmind.scribe.pen.LoggerManager;

public class SimulatedSequence extends Sequence {

  private final ConcurrentHashMap<String, SequenceData> DATA_MAP = new ConcurrentHashMap<String, SequenceData>();
  private final DataSource dataSource;
  private final String tableName;
  private final int incrementBy;

  public SimulatedSequence (DataSource dataSource, String tableName, int incrementBy) {

    this.dataSource = dataSource;
    this.tableName = tableName;
    this.incrementBy = incrementBy;
  }

  public void register () {

    SequenceManager.register(this);
  }

  @Override
  public long nextLong (String name) {

    return getSequenceData(name).nextLong();
  }

  private SequenceData getSequenceData (String name) {

    SequenceData sequenceData;

    if ((sequenceData = DATA_MAP.get(name)) == null) {
      synchronized (DATA_MAP) {
        if ((sequenceData = DATA_MAP.get(name)) == null) {
          DATA_MAP.put(name, sequenceData = new SequenceData(name));
        }
      }
    }

    return sequenceData;
  }

  private class SequenceData {

    private final AtomicLong atomicBoundary;
    private final AtomicLong atomicOffset = new AtomicLong(0);
    private final String name;
    private final String sequenceSql;

    public SequenceData (String name) {

      this.name = name;

      sequenceSql = "UPDATE " + tableName + " SET next_val=LAST_INSERT_ID(next_val + " + incrementBy + ") where name = '" + name + "'";
      atomicBoundary = new AtomicLong(getLastInsertId());
    }

    public long nextLong () {

      long nextValue = 0;

      do {
        if (incrementBy == 1) {

          nextValue = getLastInsertId();
        }
        else {

          long currentOffset;

          do {
            if ((currentOffset = atomicOffset.incrementAndGet()) < incrementBy) {
              nextValue = atomicBoundary.get() + currentOffset;
            }
            else if (currentOffset == incrementBy) {
              try {
                atomicBoundary.set(nextValue = getLastInsertId());
                atomicOffset.set(0);
              }
              catch (SimulatedSequenceDisasterException simulatedSequenceDisasterException) {
                LoggerManager.getLogger(SimulatedSequence.class).error(simulatedSequenceDisasterException);
                atomicOffset.set(incrementBy - 1);
              }
            }
          } while (currentOffset > incrementBy);
        }
      } while (nextValue == 0);

      return nextValue;
    }

    private long getLastInsertId () {

      Connection connection = null;
      Statement statement = null;
      ResultSet resultSet = null;

      try {
        try {
          connection = dataSource.getConnection();
          statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
          statement.executeUpdate(sequenceSql, Statement.RETURN_GENERATED_KEYS);

          resultSet = statement.getGeneratedKeys();
          if (resultSet.next()) {
            return resultSet.getLong(1);
          }
          else {
            throw new SimulatedSequenceDisasterException("No sequence(%s) has been generated", name);
          }
        }
        finally {
          if (resultSet != null) {
            resultSet.close();
          }
          if (statement != null) {
            statement.close();
          }
          if (connection != null) {
            connection.close();
          }
        }
      }
      catch (SimulatedSequenceDisasterException simulatedSequenceDisasterException) {
        throw simulatedSequenceDisasterException;
      }
      catch (SQLException sqlException) {
        throw new SimulatedSequenceDisasterException(sqlException, "Unable to update sequence(%s)", name);
      }
      catch (Throwable throwable) {
        throw new SimulatedSequenceDisasterException(throwable, "Unknown exception encountered in sequence(%s) update", name);
      }
    }
  }
}
