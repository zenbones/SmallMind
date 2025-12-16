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
package org.smallmind.persistence.sql;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;

/**
 * LRU cache of {@link PooledPreparedStatement}s keyed by the arguments used to prepare them. It
 * implements {@link StatementEventListener} to track statement lifecycle events from the pool.
 */
public class PooledPreparedStatementCache implements StatementEventListener {

  private final TreeMap<TimeKey, String> timeMap;
  private final HashMap<ArgumentKey, LinkedList<String>> argumentMap;
  private final HashMap<String, StatementWrapper> statementMap;
  private final int maxStatements;

  /**
   * Builds a cache with the supplied maximum number of prepared statements.
   *
   * @param maxStatements maximum cached statements; eviction occurs when the limit is reached
   */
  public PooledPreparedStatementCache (int maxStatements) {

    this.maxStatements = maxStatements;

    timeMap = new TreeMap<>();
    argumentMap = new HashMap<>();
    statementMap = new HashMap<>();
  }

  /**
   * Adds a new prepared statement to the cache, evicting the least-recently-used entry if the cache
   * is full. The statement is indexed by its creation arguments for later lookup.
   *
   * @param args            arguments used to create the statement (SQL plus JDBC options)
   * @param pooledStatement pooled statement wrapper to cache
   * @return the wrapped {@link PreparedStatement} ready for use
   */
  public synchronized PreparedStatement cachePreparedStatement (Object[] args, PooledPreparedStatement pooledStatement) {

    TimeKey timeKey;
    ArgumentKey argumentKey;
    LinkedList<String> statementIdList;
    StatementWrapper statementWrapper;

    if (statementMap.size() == maxStatements) {
      statementWrapper = statementMap.remove(timeMap.remove(timeMap.firstKey()));

      if ((statementIdList = argumentMap.get(statementWrapper.getArgumentKey())) != null) {
        statementIdList.remove(statementWrapper.getPooledStatement().getStatementId());
        if (statementIdList.isEmpty() && (!Arrays.equals(statementWrapper.getArgumentKey().getArgs(), args))) {
          argumentMap.remove(statementWrapper.getArgumentKey());
        }
      }
    }

    argumentKey = new ArgumentKey(args);
    if ((statementIdList = argumentMap.get(argumentKey)) == null) {
      argumentMap.put(argumentKey, statementIdList = new LinkedList<String>());
    }
    statementIdList.add(pooledStatement.getStatementId());

    statementMap.put(pooledStatement.getStatementId(), new StatementWrapper(timeKey = new TimeKey(), argumentKey, pooledStatement));
    timeMap.put(timeKey, pooledStatement.getStatementId());

    return pooledStatement.getPreparedStatement();
  }

  /**
   * Attempts to retrieve a free prepared statement matching the argument signature from the cache.
   *
   * @param args method arguments used to create the statement (SQL plus options)
   * @return cached {@link PreparedStatement} if available, otherwise {@code null}
   */
  public synchronized PreparedStatement getPreparedStatement (Object[] args) {

    ArgumentKey argumentKey;
    LinkedList<String> statementIdList;
    StatementWrapper statementWrapper;

    argumentKey = new ArgumentKey(args);
    if ((statementIdList = argumentMap.get(argumentKey)) != null) {
      for (String statementId : statementIdList) {
        if (!(statementWrapper = statementMap.get(statementId)).isInUse()) {
          return statementWrapper.acquire();
        }
      }
    }

    return null;
  }

  /**
   * Marks a statement as returned to the cache when the pool signals it has been closed.
   *
   * @param event statement close event
   */
  public synchronized void statementClosed (StatementEvent event) {

    StatementWrapper statementWrapper;

    if ((statementWrapper = statementMap.get(((PooledPreparedStatementEvent)event).getStatementId())) != null) {
      statementWrapper.free();
    }
  }

  /**
   * Removes a statement from the cache upon error, closes it, and cleans up index structures.
   *
   * @param event statement error event from the pool
   */
  public synchronized void statementErrorOccurred (StatementEvent event) {

    StatementWrapper statementWrapper;
    LinkedList<String> statementIdList;

    if ((statementWrapper = statementMap.remove(((PooledPreparedStatementEvent)event).getStatementId())) != null) {
      timeMap.remove(statementWrapper.getTimeKey());

      (statementIdList = argumentMap.get(statementWrapper.getArgumentKey())).remove(statementWrapper.getPooledStatement().getStatementId());
      if (statementIdList.isEmpty()) {
        argumentMap.remove(statementWrapper.getArgumentKey());
      }

      try {
        statementWrapper.getPooledStatement().close();
      } catch (SQLException sqlException) {
        logException(statementWrapper, sqlException);
      }
    }
  }

  /**
   * Closes all cached statements, logging but ignoring secondary exceptions.
   */
  public synchronized void close () {

    for (StatementWrapper statementWrapper : statementMap.values()) {
      try {
        statementWrapper.getPooledStatement().close();
      } catch (SQLException sqlException) {
        logException(statementWrapper, sqlException);
      }
    }
  }

  /**
   * Attempts to print the exception to the pooled statement's log writer, swallowing any secondary
   * JDBC errors during logging.
   *
   * @param statementWrapper wrapper containing the failed statement
   * @param sqlException     exception to report
   */
  private void logException (StatementWrapper statementWrapper, SQLException sqlException) {

    PrintWriter logWriter;

    try {
      if ((logWriter = statementWrapper.getPooledStatement().getLogWriter()) != null) {
        sqlException.printStackTrace(logWriter);
      }
    } catch (SQLException buriedException) {
    }
  }

  private class TimeKey implements Comparable<TimeKey> {

    private long lastAccesssTime;

    /**
     * Constructs a time key using the current time.
     */
    public TimeKey () {

      lastAccesssTime = System.currentTimeMillis();
    }

    /**
     * @return timestamp when the statement was last accessed
     */
    public long getLastAccessTime () {

      return lastAccesssTime;
    }

    /**
     * Updates the access time to now.
     *
     * @return this key for chaining
     */
    public TimeKey update () {

      lastAccesssTime = System.currentTimeMillis();

      return this;
    }

    /**
     * Orders time keys by last access time for LRU eviction.
     *
     * @param timeKey other key to compare
     * @return comparison of timestamps
     */
    public int compareTo (TimeKey timeKey) {

      return Long.compare(lastAccesssTime, timeKey.getLastAccessTime());
    }
  }

  private class ArgumentKey {

    private final Object[] args;

    /**
     * Wrapper for prepared-statement arguments to support map lookups.
     *
     * @param args argument array used to create the statement
     */
    public ArgumentKey (Object[] args) {

      this.args = args;
    }

    /**
     * @return backing argument array
     */
    public Object[] getArgs () {

      return args;
    }

    /**
     * Hashes using deep array semantics so nested arrays are handled.
     *
     * @return hash code for the argument signature
     */

    @Override
    public int hashCode () {

      return Arrays.deepHashCode(args);
    }

    /**
     * Compares argument keys using deep array equality.
     *
     * @param obj candidate key
     * @return {@code true} if argument arrays match
     */

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ArgumentKey) && Arrays.equals(args, ((ArgumentKey)obj).getArgs());
    }
  }

  private class StatementWrapper {

    private final TimeKey timeKey;
    private final ArgumentKey argumentKey;
    private final PooledPreparedStatement pooledStatement;
    private boolean inUse;

    /**
     * Wraps a pooled statement along with its keys and usage flag.
     *
     * @param timeKey         eviction key tracking access time
     * @param argumentKey     lookup key based on creation arguments
     * @param pooledStatement pooled statement being tracked
     */
    public StatementWrapper (TimeKey timeKey, ArgumentKey argumentKey, PooledPreparedStatement pooledStatement) {

      this.timeKey = timeKey;
      this.argumentKey = argumentKey;
      this.pooledStatement = pooledStatement;

      inUse = true;
    }

    /**
     * @return the time key for eviction ordering
     */
    public TimeKey getTimeKey () {

      return timeKey;
    }

    /**
     * @return argument key for lookup
     */
    public ArgumentKey getArgumentKey () {

      return argumentKey;
    }

    /**
     * @return the pooled statement being cached
     */
    public PooledPreparedStatement getPooledStatement () {

      return pooledStatement;
    }

    /**
     * @return whether the statement is currently handed out
     */
    public boolean isInUse () {

      return inUse;
    }

    /**
     * Marks the statement as available for reuse.
     */
    public void free () {

      inUse = false;
    }

    /**
     * Acquires the statement for use, updating its access time and returning the JDBC handle.
     *
     * @return wrapped {@link PreparedStatement}
     */
    public PreparedStatement acquire () {

      inUse = true;

      timeMap.remove(timeKey);
      timeMap.put(timeKey.update(), pooledStatement.getStatementId());

      return pooledStatement.getPreparedStatement();
    }
  }
}
