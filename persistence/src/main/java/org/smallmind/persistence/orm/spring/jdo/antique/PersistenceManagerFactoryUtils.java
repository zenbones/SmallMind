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

import javax.jdo.JDODataStoreException;
import javax.jdo.JDOException;
import javax.jdo.JDOFatalDataStoreException;
import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOOptimisticVerificationException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public abstract class PersistenceManagerFactoryUtils {

  private static final Log logger = LogFactory.getLog(PersistenceManagerFactoryUtils.class);
  /**
   * Order value for TransactionSynchronization objects that clean up JDO
   * PersistenceManagers. Return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100
   * to execute PersistenceManager cleanup before JDBC Connection cleanup, if any.
   */
  public static final int PERSISTENCE_MANAGER_SYNCHRONIZATION_ORDER =
    DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

  /**
   * Create an appropriate SQLExceptionTranslator for the given PersistenceManagerFactory.
   * <p>If a DataSource is found, creates a SQLErrorCodeSQLExceptionTranslator for the
   * DataSource; else, falls back to a SQLStateSQLExceptionTranslator.
   * (may be {@code null})
   */
  static SQLExceptionTranslator newJdbcExceptionTranslator (Object connectionFactory) {
    // Check for PersistenceManagerFactory's DataSource.
    if (connectionFactory instanceof DataSource) {
      return new SQLErrorCodeSQLExceptionTranslator((DataSource)connectionFactory);
    } else {
      return new SQLStateSQLExceptionTranslator();
    }
  }

  /**
   * Obtain a JDO PersistenceManager via the given factory. Is aware of a
   * corresponding PersistenceManager bound to the current thread,
   * for example when using JdoTransactionManager. Will create a new
   * PersistenceManager else, if "allowCreate" is {@code true}.
   * when no transactional PersistenceManager can be found for the current thread
   * "allowCreate" is {@code false}
   */
  public static PersistenceManager getPersistenceManager (PersistenceManagerFactory pmf, boolean allowCreate)
    throws DataAccessResourceFailureException, IllegalStateException {

    try {
      return doGetPersistenceManager(pmf, allowCreate);
    } catch (JDOException ex) {
      throw new DataAccessResourceFailureException("Could not obtain JDO PersistenceManager", ex);
    }
  }

  /**
   * Obtain a JDO PersistenceManager via the given factory. Is aware of a
   * corresponding PersistenceManager bound to the current thread,
   * for example when using JdoTransactionManager. Will create a new
   * PersistenceManager else, if "allowCreate" is {@code true}.
   * <p>Same as {@code getPersistenceManager}, but throwing the original JDOException.
   * when no transactional PersistenceManager can be found for the current thread
   * "allowCreate" is {@code false}
   */
  public static PersistenceManager doGetPersistenceManager (PersistenceManagerFactory pmf, boolean allowCreate)
    throws JDOException, IllegalStateException {

    Assert.notNull(pmf, "No PersistenceManagerFactory specified");

    PersistenceManagerHolder pmHolder =
      (PersistenceManagerHolder)TransactionSynchronizationManager.getResource(pmf);
    if (pmHolder != null) {
      if (!pmHolder.isSynchronizedWithTransaction() &&
            TransactionSynchronizationManager.isSynchronizationActive()) {
        pmHolder.setSynchronizedWithTransaction(true);
        TransactionSynchronizationManager.registerSynchronization(
          new PersistenceManagerSynchronization(pmHolder, pmf, false));
      }
      return pmHolder.getPersistenceManager();
    }

    if (!allowCreate && !TransactionSynchronizationManager.isSynchronizationActive()) {
      throw new IllegalStateException("No JDO PersistenceManager bound to thread, " +
                                        "and configuration does not allow creation of non-transactional one here");
    }

    logger.debug("Opening JDO PersistenceManager");
    PersistenceManager pm = pmf.getPersistenceManager();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      logger.debug("Registering transaction synchronization for JDO PersistenceManager");
      // Use same PersistenceManager for further JDO actions within the transaction.
      // Thread object will get removed by synchronization at transaction completion.
      pmHolder = new PersistenceManagerHolder(pm);
      pmHolder.setSynchronizedWithTransaction(true);
      TransactionSynchronizationManager.registerSynchronization(
        new PersistenceManagerSynchronization(pmHolder, pmf, true));
      TransactionSynchronizationManager.bindResource(pmf, pmHolder);
    }

    return pm;
  }

  /**
   * Return whether the given JDO PersistenceManager is transactional, that is,
   * bound to the current thread by Spring's transaction facilities.
   * was created with (can be {@code null})
   */
  public static boolean isPersistenceManagerTransactional (
    PersistenceManager pm, PersistenceManagerFactory pmf) {

    if (pmf == null) {
      return false;
    }
    PersistenceManagerHolder pmHolder =
      (PersistenceManagerHolder)TransactionSynchronizationManager.getResource(pmf);
    return (pmHolder != null && pm == pmHolder.getPersistenceManager());
  }

  /**
   * Apply the current transaction timeout, if any, to the given JDO Query object.
   */
  public static void applyTransactionTimeout (Query query, PersistenceManagerFactory pmf) throws JDOException {

    Assert.notNull(query, "No Query object specified");
    PersistenceManagerHolder pmHolder =
      (PersistenceManagerHolder)TransactionSynchronizationManager.getResource(pmf);
    if (pmHolder != null && pmHolder.hasTimeout() &&
          pmf.supportedOptions().contains("javax.jdo.option.DatastoreTimeout")) {
      int timeout = (int)pmHolder.getTimeToLiveInMillis();
      query.setDatastoreReadTimeoutMillis(timeout);
      query.setDatastoreWriteTimeoutMillis(timeout);
    }
  }

  /**
   * Convert the given JDOException to an appropriate exception from the
   * {@code org.springframework.dao} hierarchy.
   * <p>The most important cases like object not found or optimistic locking failure
   * are covered here. For more fine-granular conversion, JdoTransactionManager
   * supports sophisticated translation of exceptions via a JdoDialect.
   */
  public static DataAccessException convertJdoAccessException (JDOException ex) {

    if (ex instanceof JDOObjectNotFoundException) {
      throw new JdoObjectRetrievalFailureException((JDOObjectNotFoundException)ex);
    }
    if (ex instanceof JDOOptimisticVerificationException) {
      throw new JdoOptimisticLockingFailureException((JDOOptimisticVerificationException)ex);
    }
    if (ex instanceof JDODataStoreException) {
      return new JdoResourceFailureException((JDODataStoreException)ex);
    }
    if (ex instanceof JDOFatalDataStoreException) {
      return new JdoResourceFailureException((JDOFatalDataStoreException)ex);
    }
    if (ex instanceof JDOUserException) {
      return new JdoUsageException((JDOUserException)ex);
    }
    if (ex instanceof JDOFatalUserException) {
      return new JdoUsageException((JDOFatalUserException)ex);
    }
    // fallback
    return new JdoSystemException(ex);
  }

  /**
   * Close the given PersistenceManager, created via the given factory,
   * if it is not managed externally (i.e. not bound to the thread).
   * (can be {@code null})
   */
  public static void releasePersistenceManager (PersistenceManager pm, PersistenceManagerFactory pmf) {

    try {
      doReleasePersistenceManager(pm, pmf);
    } catch (JDOException ex) {
      logger.debug("Could not close JDO PersistenceManager", ex);
    } catch (Throwable ex) {
      logger.debug("Unexpected exception on closing JDO PersistenceManager", ex);
    }
  }

  /**
   * Actually release a PersistenceManager for the given factory.
   * Same as {@code releasePersistenceManager}, but throwing the original JDOException.
   * (can be {@code null})
   */
  public static void doReleasePersistenceManager (PersistenceManager pm, PersistenceManagerFactory pmf)
    throws JDOException {

    if (pm == null) {
      return;
    }
    // Only release non-transactional PersistenceManagers.
    if (!isPersistenceManagerTransactional(pm, pmf)) {
      logger.debug("Closing JDO PersistenceManager");
      pm.close();
    }
  }

  /**
   * Callback for resource cleanup at the end of a non-JDO transaction
   * (e.g. when participating in a JtaTransactionManager transaction).
   */
  private static class PersistenceManagerSynchronization
    extends ResourceHolderSynchronization<PersistenceManagerHolder, PersistenceManagerFactory>
    implements Ordered {

    private final boolean newPersistenceManager;

    public PersistenceManagerSynchronization (
      PersistenceManagerHolder pmHolder, PersistenceManagerFactory pmf, boolean newPersistenceManager) {

      super(pmHolder, pmf);
      this.newPersistenceManager = newPersistenceManager;
    }

    @Override
    public int getOrder () {

      return PERSISTENCE_MANAGER_SYNCHRONIZATION_ORDER;
    }

    @Override
    public void flushResource (PersistenceManagerHolder resourceHolder) {

      try {
        resourceHolder.getPersistenceManager().flush();
      } catch (JDOException ex) {
        throw convertJdoAccessException(ex);
      }
    }

    @Override
    protected boolean shouldUnbindAtCompletion () {

      return this.newPersistenceManager;
    }

    @Override
    protected boolean shouldReleaseAfterCompletion (PersistenceManagerHolder resourceHolder) {

      return !resourceHolder.getPersistenceManager().isClosed();
    }

    @Override
    protected void releaseResource (PersistenceManagerHolder resourceHolder, PersistenceManagerFactory resourceKey) {

      releasePersistenceManager(resourceHolder.getPersistenceManager(), resourceKey);
    }
  }
}
