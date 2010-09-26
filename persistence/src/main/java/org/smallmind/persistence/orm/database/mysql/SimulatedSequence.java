/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
