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