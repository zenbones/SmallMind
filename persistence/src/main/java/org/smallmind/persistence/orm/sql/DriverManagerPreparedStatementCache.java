/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.orm.sql;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;

public class DriverManagerPreparedStatementCache implements StatementEventListener {

  private TreeMap<TimeKey, String> timeMap;
  private HashMap<ArgumentKey, LinkedList<String>> argumentMap;
  private HashMap<String, StatementWrapper> statementMap;
  private int maxStatements;

  public DriverManagerPreparedStatementCache (int maxStatements) {

    this.maxStatements = maxStatements;

    timeMap = new TreeMap<TimeKey, String>();
    argumentMap = new HashMap<ArgumentKey, LinkedList<String>>();
    statementMap = new HashMap<String, StatementWrapper>();
  }

  public synchronized PreparedStatement cachePreparedStatement (Object[] args, DriverManagerPooledPreparedStatement pooledStatement) {

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

  public synchronized void statementClosed (StatementEvent event) {

    StatementWrapper statementWrapper;

    if ((statementWrapper = statementMap.get(((DriverManagerStatementEvent)event).getStatementId())) != null) {
      statementWrapper.free();
    }
  }

  public synchronized void statementErrorOccurred (StatementEvent event) {

    StatementWrapper statementWrapper;
    LinkedList<String> statementIdList;

    if ((statementWrapper = statementMap.remove(((DriverManagerStatementEvent)event).getStatementId())) != null) {
      timeMap.remove(statementWrapper.getTimeKey());

      (statementIdList = argumentMap.get(statementWrapper.getArgumentKey())).remove(statementWrapper.getPooledStatement().getStatementId());
      if (statementIdList.isEmpty()) {
        argumentMap.remove(statementWrapper.getArgumentKey());
      }

      try {
        statementWrapper.getPooledStatement().close();
      }
      catch (SQLException sqlException) {
        logException(statementWrapper, sqlException);
      }
    }
  }

  public synchronized void close () {

    for (StatementWrapper statementWrapper : statementMap.values()) {
      try {
        statementWrapper.getPooledStatement().close();
      }
      catch (SQLException sqlException) {
        logException(statementWrapper, sqlException);
      }
    }
  }

  private void logException (StatementWrapper statementWrapper, SQLException sqlException) {

    PrintWriter logWriter;

    try {
      if ((logWriter = statementWrapper.getPooledStatement().getLogWriter()) != null) {
        sqlException.printStackTrace(logWriter);
      }
    }
    catch (SQLException buriedException) {
    }
  }

  private class TimeKey implements Comparable<TimeKey> {

    private long lastAccesssTime;

    public TimeKey () {

      lastAccesssTime = System.currentTimeMillis();
    }

    public long getLastAccesssTime () {

      return lastAccesssTime;
    }

    public TimeKey update () {

      lastAccesssTime = System.currentTimeMillis();

      return this;
    }

    public int compareTo (TimeKey timeKey) {

      return (int)(lastAccesssTime - timeKey.getLastAccesssTime());
    }
  }

  private class ArgumentKey {

    private Object[] args;

    public ArgumentKey (Object[] args) {

      this.args = args;
    }

    public Object[] getArgs () {

      return args;
    }

    @Override
    public int hashCode () {

      return Arrays.deepHashCode(args);
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ArgumentKey) && Arrays.equals(args, ((ArgumentKey)obj).getArgs());
    }
  }

  private class StatementWrapper {

    private TimeKey timeKey;
    private ArgumentKey argumentKey;
    private DriverManagerPooledPreparedStatement pooledStatement;
    private boolean inUse;

    public StatementWrapper (TimeKey timeKey, ArgumentKey argumentKey, DriverManagerPooledPreparedStatement pooledStatement) {

      this.timeKey = timeKey;
      this.argumentKey = argumentKey;
      this.pooledStatement = pooledStatement;

      inUse = true;
    }

    public TimeKey getTimeKey () {

      return timeKey;
    }

    public ArgumentKey getArgumentKey () {

      return argumentKey;
    }

    public DriverManagerPooledPreparedStatement getPooledStatement () {

      return pooledStatement;
    }

    public boolean isInUse () {

      return inUse;
    }

    public void free () {

      inUse = false;
    }

    public PreparedStatement acquire () {

      inUse = true;

      timeMap.remove(timeKey);
      timeMap.put(timeKey.update(), pooledStatement.getStatementId());

      return pooledStatement.getPreparedStatement();
    }
  }
}