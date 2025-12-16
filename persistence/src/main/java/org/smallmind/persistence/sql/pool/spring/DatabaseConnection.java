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
package org.smallmind.persistence.sql.pool.spring;

/**
 * Spring-friendly bean capturing JDBC connection details.
 */
public class DatabaseConnection {

  private String jdbcUrl;
  private String user;
  private String password;

  /**
   * Creates an empty connection descriptor to be populated via setters.
   */
  public DatabaseConnection () {

  }

  /**
   * @param jdbcUrl  JDBC URL
   * @param user     user name
   * @param password password
   */
  public DatabaseConnection (String jdbcUrl, String user, String password) {

    this.jdbcUrl = jdbcUrl;
    this.user = user;
    this.password = password;
  }

  /**
   * @return configured JDBC URL
   */
  public String getJdbcUrl () {

    return jdbcUrl;
  }

  /**
   * @param jdbcUrl JDBC URL to set
   */
  public void setJdbcUrl (String jdbcUrl) {

    this.jdbcUrl = jdbcUrl;
  }

  /**
   * @return configured user name
   */
  public String getUser () {

    return user;
  }

  /**
   * @param user user name to set
   */
  public void setUser (String user) {

    this.user = user;
  }

  /**
   * @return configured password
   */
  public String getPassword () {

    return password;
  }

  /**
   * @param password password to set
   */
  public void setPassword (String password) {

    this.password = password;
  }

  /**
   * @return {@code true} when URL, user, and password are all populated
   */
  public boolean isComplete () {

    return (jdbcUrl != null) && (user != null) && (password != null);
  }
}
