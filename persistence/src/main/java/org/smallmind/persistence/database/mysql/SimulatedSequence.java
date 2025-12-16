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
package org.smallmind.persistence.database.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.smallmind.persistence.database.Sequence;
import org.smallmind.persistence.database.SequenceManager;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Sequence implementation that simulates database sequences for MySQL by maintaining a row per
 * sequence name in a table and using {@code LAST_INSERT_ID} to obtain atomic increments.
 */
public class SimulatedSequence extends Sequence {

  private final ConcurrentHashMap<String, SequenceData> DATA_MAP = new ConcurrentHashMap<>();
  private final DataSource dataSource;
  private final String tableName;
  private final int incrementBy;

  /**
   * @param dataSource  data source used to execute sequence update statements
   * @param tableName   table that holds sequence names and next values
   * @param incrementBy step size to reserve per update (supports allocation blocks > 1)
   */
  public SimulatedSequence (DataSource dataSource, String tableName, int incrementBy) {

    this.dataSource = dataSource;
    this.tableName = tableName;
    this.incrementBy = incrementBy;
  }

  /**
   * Registers this sequence implementation with the {@link SequenceManager}.
   */
  public void register () {

    SequenceManager.register(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long nextLong (String name) {

    return getSequenceData(name).nextLong();
  }

  /**
   * Retrieves or initializes the sequence data for the given name.
   *
   * @param name sequence identifier
   * @return data holder used to allocate sequence values
   */
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
    private final String insertSql;
    private final String updateSql;

    /**
     * Initializes the sequence row for the given name and primes the local boundary cache.
     *
     * @param name sequence name
     */
    public SequenceData (String name) {

      this.name = name;

      insertSql = "INSERT IGNORE INTO " + tableName + " (name, next_val) VALUES('" + name + "', 0)";
      updateSql = "UPDATE " + tableName + " SET next_val=LAST_INSERT_ID(next_val + " + incrementBy + ") where name='" + name + "'";

      insertName();
      atomicBoundary = new AtomicLong(getLastInsertId());
    }

    /**
     * Returns the next sequence value, optionally using a cached block of values when incrementing by more than one.
     *
     * @return next sequence value
     */
    public long nextLong () {

      long nextValue = 0;

      do {
        if (incrementBy == 1) {

          nextValue = getLastInsertId();
        } else {

          long currentOffset;

          do {
            if ((currentOffset = atomicOffset.incrementAndGet()) < incrementBy) {
              nextValue = atomicBoundary.get() + currentOffset;
            } else if (currentOffset == incrementBy) {
              try {
                atomicBoundary.set(nextValue = getLastInsertId());
                atomicOffset.set(0);
              } catch (SimulatedSequenceDisasterException simulatedSequenceDisasterException) {
                LoggerManager.getLogger(SimulatedSequence.class).error(simulatedSequenceDisasterException);

                currentOffset = incrementBy + 1;
                atomicOffset.set(incrementBy - 1);
              }
            }
          } while (currentOffset > incrementBy);
        }
      } while (nextValue == 0);

      return nextValue;
    }

    /**
     * Executes an insert to ensure a row exists for the sequence name.
     *
     * @throws SimulatedSequenceDisasterException if the insert fails
     */
    private void insertName () {

      try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        connection.setAutoCommit(true);
        statement.executeUpdate(insertSql);
      } catch (SQLException sqlException) {
        throw new SimulatedSequenceDisasterException(sqlException, "Unable to create sequence(%s)", name);
      }
    }

    /**
     * Issues an update against the backing table to atomically advance the sequence and returns the
     * new boundary value using {@code LAST_INSERT_ID()}.
     *
     * @return newly allocated upper boundary for the sequence
     * @throws SimulatedSequenceDisasterException if SQL execution fails or no key is returned
     */
    private long getLastInsertId () {

      try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        connection.setAutoCommit(true);
        statement.executeUpdate(updateSql, Statement.RETURN_GENERATED_KEYS);

        try (ResultSet resultSet = statement.getGeneratedKeys()) {
          if (resultSet.next()) {
            return resultSet.getLong(1);
          } else {
            throw new SimulatedSequenceDisasterException("No sequence(%s) has been generated", name);
          }
        }
      } catch (SimulatedSequenceDisasterException simulatedSequenceDisasterException) {
        throw simulatedSequenceDisasterException;
      } catch (SQLException sqlException) {
        throw new SimulatedSequenceDisasterException(sqlException, "Unable to update sequence(%s)", name);
      } catch (Throwable throwable) {
        throw new SimulatedSequenceDisasterException(throwable, "Unknown exception encountered in sequence(%s) update", name);
      }
    }
  }
}
