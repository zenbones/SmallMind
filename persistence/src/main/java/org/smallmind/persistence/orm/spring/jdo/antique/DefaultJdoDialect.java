/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.orm.spring.jdo.antique;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

import javax.jdo.Constants;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import java.sql.Connection;
import java.sql.SQLException;

public class DefaultJdoDialect implements JdoDialect, PersistenceExceptionTranslator {

  private SQLExceptionTranslator jdbcExceptionTranslator;


  /**
   * Create a new DefaultJdoDialect.
   */
  public DefaultJdoDialect() {
  }

  /**
   * Create a new DefaultJdoDialect.
   * which is used to initialize the default JDBC exception translator
   */
  public DefaultJdoDialect(Object connectionFactory) {
    this.jdbcExceptionTranslator = PersistenceManagerFactoryUtils.newJdbcExceptionTranslator(connectionFactory);
  }

  /**
   * Return the JDBC exception translator for this dialect, if any.
   */
  public SQLExceptionTranslator getJdbcExceptionTranslator() {
    return this.jdbcExceptionTranslator;
  }

  /**
   * Set the JDBC exception translator for this dialect.
   * <p>Applied to any SQLException root cause of a JDOException, if specified.
   * The default is to rely on the JDO provider's native exception translation.
   */
  public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
    this.jdbcExceptionTranslator = jdbcExceptionTranslator;
  }


  //-------------------------------------------------------------------------
  // Hooks for transaction management (used by JdoTransactionManager)
  //-------------------------------------------------------------------------

  @Override
  public Object beginTransaction(Transaction transaction, TransactionDefinition definition)
    throws JDOException, SQLException, TransactionException {

    String jdoIsolationLevel = getJdoIsolationLevel(definition);
    if (jdoIsolationLevel != null) {
      transaction.setIsolationLevel(jdoIsolationLevel);
    }
    transaction.begin();
    return null;
  }

  /**
   * Determine the JDO isolation level String to use for the given
   * Spring transaction definition.
   * to indicate that no isolation level should be set explicitly
   */
  protected String getJdoIsolationLevel(TransactionDefinition definition) {
    switch (definition.getIsolationLevel()) {
      case TransactionDefinition.ISOLATION_SERIALIZABLE:
        return Constants.TX_SERIALIZABLE;
      case TransactionDefinition.ISOLATION_REPEATABLE_READ:
        return Constants.TX_REPEATABLE_READ;
      case TransactionDefinition.ISOLATION_READ_COMMITTED:
        return Constants.TX_READ_COMMITTED;
      case TransactionDefinition.ISOLATION_READ_UNCOMMITTED:
        return Constants.TX_READ_UNCOMMITTED;
      default:
        return null;
    }
  }

  /**
   * This implementation does nothing, as the default beginTransaction implementation
   * does not require any cleanup.
   */
  @Override
  public void cleanupTransaction(Object transactionData) {
  }

  /**
   * This implementation returns a DataStoreConnectionHandle for JDO.
   * <p><b>NOTE:</b> A JDO DataStoreConnection is always a wrapper,
   * never the native JDBC Connection. If you need access to the native JDBC
   * Connection (or the connection pool handle, to be unwrapped via a Spring
   * NativeJdbcExtractor), override this method to return the native
   * Connection through the corresponding vendor-specific mechanism.
   * <p>A JDO DataStoreConnection is only "borrowed" from the PersistenceManager:
   * it needs to be returned as early as possible. Effectively, JDO requires the
   * fetched Connection to be closed before continuing PersistenceManager work.
   * For this reason, the exposed ConnectionHandle eagerly releases its JDBC
   * Connection at the end of each JDBC data access operation (that is, on
   * {@code DataSourceUtils.releaseConnection}).
   */
  @Override
  public ConnectionHandle getJdbcConnection(PersistenceManager pm, boolean readOnly)
    throws JDOException, SQLException {

    return new DataStoreConnectionHandle(pm);
  }

  /**
   * This implementation does nothing, assuming that the Connection
   * will implicitly be closed with the PersistenceManager.
   * <p>If the JDO provider returns a Connection handle that it
   * expects the application to close, the dialect needs to invoke
   * {@code Connection.close} here.
   */
  @Override
  public void releaseJdbcConnection(ConnectionHandle conHandle, PersistenceManager pm)
    throws JDOException, SQLException {
  }


  //-----------------------------------------------------------------------------------
  // Hook for exception translation (used by JdoTransactionManager)
  //-----------------------------------------------------------------------------------

  /**
   * Implementation of the PersistenceExceptionTranslator interface,
   * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
   * <p>Converts the exception if it is a JDOException, using this JdoDialect.
   * Else returns {@code null} to indicate an unknown exception.
   */
  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    if (ex instanceof JDOException) {
      return translateException((JDOException) ex);
    }
    return null;
  }

  /**
   * This implementation delegates to PersistenceManagerFactoryUtils.
   */
  @Override
  public DataAccessException translateException(JDOException ex) {
    if (getJdbcExceptionTranslator() != null && ex.getCause() instanceof SQLException) {
      return getJdbcExceptionTranslator().translate("JDO operation: " + ex.getMessage(),
        extractSqlStringFromException(ex), (SQLException) ex.getCause());
    }
    return PersistenceManagerFactoryUtils.convertJdoAccessException(ex);
  }

  /**
   * Template method for extracting a SQL String from the given exception.
   * <p>Default implementation always returns {@code null}. Can be overridden in
   * subclasses to extract SQL Strings for vendor-specific exception classes.
   */
  protected String extractSqlStringFromException(JDOException ex) {
    return null;
  }


  /**
   * ConnectionHandle implementation that fetches a new JDO DataStoreConnection
   * for every {@code getConnection} call and closes the Connection on
   * {@code releaseConnection}. This is necessary because JDO requires the
   * fetched Connection to be closed before continuing PersistenceManager work.
   */
  private static class DataStoreConnectionHandle implements ConnectionHandle {

    private final PersistenceManager persistenceManager;

    public DataStoreConnectionHandle(PersistenceManager persistenceManager) {
      this.persistenceManager = persistenceManager;
    }

    @Override
    public Connection getConnection() {
      return (Connection) this.persistenceManager.getDataStoreConnection();
    }

    @Override
    public void releaseConnection(Connection con) {
      JdbcUtils.closeConnection(con);
    }
  }

}
