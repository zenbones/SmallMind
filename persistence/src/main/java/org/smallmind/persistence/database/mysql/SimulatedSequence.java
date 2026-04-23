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
 * MySQL {@link Sequence} implementation that simulates native sequences by storing per-name counters
 * in a dedicated table and using {@code LAST_INSERT_ID} to perform atomic increments.
 * Supports block allocation ({@code incrementBy > 1}) to reduce database round-trips.
 */
public class SimulatedSequence extends Sequence {

  private final ConcurrentHashMap<String, SequenceData> DATA_MAP = new ConcurrentHashMap<>();
  private final DataSource dataSource;
  private final String tableName;
  private final int incrementBy;

  /**
   * Constructs a simulated sequence backed by the given data source and table.
   *
   * @param dataSource  data source used to execute sequence update statements
   * @param tableName   table that holds sequence names and their current values
   * @param incrementBy number of values to reserve per database update; use 1 for no block allocation
   */
  public SimulatedSequence (DataSource dataSource, String tableName, int incrementBy) {

    this.dataSource = dataSource;
    this.tableName = tableName;
    this.incrementBy = incrementBy;
  }

  /**
   * Registers this instance as the active provider in the {@link SequenceManager}.
   */
  public void register () {

    SequenceManager.register(this);
  }

  /**
   * Returns the next value for the named sequence, allocating a block from the database when the local cache is exhausted.
   *
   * @param name logical sequence name
   * @return next sequence value
   */
  @Override
  public long nextLong (String name) {

    return getSequenceData(name).nextLong();
  }

  /**
   * Returns the {@link SequenceData} for the given name, creating and inserting a new row if none exists.
   *
   * @param name logical sequence name
   * @return the sequence data holder for the given name
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

  /**
   * Per-name holder that manages block allocation and caching of sequence values obtained from the database.
   */
  private class SequenceData {

    private final AtomicLong atomicBoundary;
    private final AtomicLong atomicOffset = new AtomicLong(0);
    private final String name;
    private final String insertSql;
    private final String updateSql;

    /**
     * Creates the sequence row if absent and primes the local value cache with the current database boundary.
     *
     * @param name logical sequence name
     */
    public SequenceData (String name) {

      this.name = name;

      insertSql = "INSERT IGNORE INTO " + tableName + " (name, next_val) VALUES('" + name + "', 0)";
      updateSql = "UPDATE " + tableName + " SET next_val=LAST_INSERT_ID(next_val + " + incrementBy + ") where name='" + name + "'";

      insertName();
      atomicBoundary = new AtomicLong(getLastInsertId());
    }

    /**
     * Returns the next sequence value, drawing from the locally cached block when possible and refreshing
     * the block from the database when the cache is exhausted.
     *
     * @return next sequence value; never zero
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
     * Issues an {@code INSERT IGNORE} to ensure a row exists in the sequence table for this name.
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
     * Atomically advances the sequence counter in the database by {@code incrementBy} and returns the
     * new upper boundary via {@code LAST_INSERT_ID()}.
     *
     * @return the new upper boundary value allocated by this update
     * @throws SimulatedSequenceDisasterException if the SQL update fails, returns no generated key, or any other error occurs
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
