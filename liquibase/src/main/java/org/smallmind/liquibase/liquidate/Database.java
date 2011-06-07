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
package org.smallmind.liquibase.liquidate;

public enum Database {

   MYSQL("Mysql", com.mysql.jdbc.Driver.class) {
      public String getUrl (String host, String port, String schema) {

         return ((port == null) || (port.length() == 0)) ? "jdbc:mysql://" + host + '/' + schema : "jdbc:mysql://" + host + ':' + port + '/' + schema;
      }
   };

   private Class driver;
   private String display;

   Database (String display, Class driver) {

      this.display = display;
      this.driver = driver;
   }

   public abstract String getUrl (String host, String port, String schema);

   public String getDisplay () {

      return display;
   }

   public Class getDriver () {

      return driver;
   }

   public String toString () {

      return display;
   }
}