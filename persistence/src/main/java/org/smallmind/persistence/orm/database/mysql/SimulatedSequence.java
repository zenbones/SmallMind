package org.smallmind.persistence.orm.database.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sql.DataSource;
import org.smallmind.persistence.DataIntegrityException;

public class SimulatedSequence {

   private static final ConcurrentHashMap<String, String> SQL_MAP = new ConcurrentHashMap<String, String>();
   private static final ConcurrentLinkedQueue<Long> SEQUENCE_QUEUE = new ConcurrentLinkedQueue<Long>();

   private DataSource dataSource;
   private String tableName;
   private int incrementBy;

   public SimulatedSequence (DataSource dataSource, String tableName, int incrementBy) {

      this.dataSource = dataSource;
      this.tableName = tableName;
      this.incrementBy = incrementBy;
   }

   public long nextLong (String name) {

      if (incrementBy == 1) {
         return getLastInsertId(name);
      }

      Long nextValue;

      if ((nextValue = SEQUENCE_QUEUE.poll()) == null) {
         synchronized (SEQUENCE_QUEUE) {
            if ((nextValue = SEQUENCE_QUEUE.poll()) == null) {
               nextValue = getLastInsertId(name);
               for (int count = 1; count < incrementBy; count++) {
                  SEQUENCE_QUEUE.add(nextValue + count);
               }
            }
         }
      }

      return nextValue;
   }

   private long getLastInsertId (String name) {

      Connection connection = null;
      Statement statement = null;
      ResultSet resultSet = null;

      try {
         try {
            connection = dataSource.getConnection();
            statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.executeUpdate(getSql(name), Statement.RETURN_GENERATED_KEYS);

            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
               return resultSet.getLong(1);
            }
            else {
               throw new DataIntegrityException("No sequence(%s) has been generated", name);
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
      catch (SQLException sqlException) {
         throw new DataIntegrityException(sqlException);
      }
   }

   private String getSql (String name) {

      String sql;

      if ((sql = SQL_MAP.get(name)) == null) {
         SQL_MAP.put(name, sql = "UPDATE " + tableName + " SET next_val=LAST_INSERT_ID(next_val + " + incrementBy + ") where name = '" + name + "'");
      }

      return sql;
   }
}
