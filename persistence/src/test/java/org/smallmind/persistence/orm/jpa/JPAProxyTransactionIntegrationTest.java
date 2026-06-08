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
package org.smallmind.persistence.orm.jpa;

import java.util.HashMap;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcess;
import org.smallmind.persistence.orm.TransactionPostProcessException;
import org.smallmind.persistence.orm.spring.jpa.RealizedPersistenceUnitInfo;
import org.smallmind.persistence.sql.DriverManagerDataSource;
import org.smallmind.persistence.sql.testbench.DataSourceAvailableTestCondition;
import org.smallmind.testbench.condition.TestConditions;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test for {@link JPAProxyTransaction} against a real MySQL container started through the docker
 * testbench. It drives the commit/rollback state machine end to end over a genuine Hibernate
 * {@link EntityManager}, verifying that durables really land in (or are discarded from) the database, that
 * post-process callbacks fire for the correct end state, and that the rollback path's exception chaining
 * behaves as specified.
 *
 * <p>The session is built with {@code boundaryEnforced = false} so the transaction can be driven directly,
 * without the AOP {@code @Transactional}/{@code @NonTransactional} boundary stack that {@link JPAProxySession}
 * otherwise requires.
 *
 * <p>Requires a running Docker daemon; the {@code mysql:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class JPAProxyTransactionIntegrationTest extends AbstractGroundwaterTest {

  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String JDBC_URL = "jdbc:mysql://localhost:3306/groundwater?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&sslMode=DISABLED";
  private static final String USER_NAME = "root";
  private static final String PASSWORD = "secret";

  private EntityManagerFactory entityManagerFactory;

  public JPAProxyTransactionIntegrationTest () {

    super(DockerApplication.MYSQL);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    TestConditions.serial(120, new DataSourceAvailableTestCondition(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD));

    DriverManagerDataSource dataSource = new DriverManagerDataSource(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD);
    MutablePersistenceUnitInfo persistenceUnitInfo = new MutablePersistenceUnitInfo();

    persistenceUnitInfo.setPersistenceUnitName("jpa-proxy-transaction-it");
    persistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    persistenceUnitInfo.setNonJtaDataSource(dataSource);
    persistenceUnitInfo.addManagedClassName(Gizmo.class.getName());
    persistenceUnitInfo.setExcludeUnlistedClasses(true);
    persistenceUnitInfo.getProperties().setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
    persistenceUnitInfo.getProperties().setProperty("hibernate.hbm2ddl.auto", "create-drop");

    entityManagerFactory = new HibernatePersistenceProvider().createContainerEntityManagerFactory(new RealizedPersistenceUnitInfo(persistenceUnitInfo), new HashMap<>());
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    try {
      if (entityManagerFactory != null) {
        entityManagerFactory.close();
      }
    } finally {
      super.afterClass();
    }
  }

  private JPAProxySession session () {

    return new JPAProxySession("mysql", null, entityManagerFactory, false, false);
  }

  private Gizmo findGizmo (long id) {

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {

      return entityManager.find(Gizmo.class, id);
    } finally {
      entityManager.close();
    }
  }

  public void testCommitPersistsEntityAndRunsCommitPostProcess () {

    JPAProxySession session = session();
    EntityManager entityManager = session.getNativeSession();
    JPAProxyTransaction transaction = new JPAProxyTransaction(session, entityManager.getTransaction());
    boolean[] ran = new boolean[] {false};

    entityManager.persist(new Gizmo(1L, "alpha"));
    transaction.addPostProcess(new FlaggingPostProcess(ran, TransactionEndState.COMMIT, ProcessPriority.MIDDLE));

    transaction.commit();

    Assert.assertTrue(ran[0], "the COMMIT post-process should have run after a successful commit");

    Gizmo persisted = findGizmo(1L);

    Assert.assertNotNull(persisted, "a committed durable should be readable from the database");
    Assert.assertEquals(persisted.getName(), "alpha");
  }

  public void testExplicitRollbackDiscardsEntityAndRunsRollbackPostProcess () {

    JPAProxySession session = session();
    EntityManager entityManager = session.getNativeSession();
    JPAProxyTransaction transaction = new JPAProxyTransaction(session, entityManager.getTransaction());
    boolean[] ran = new boolean[] {false};

    entityManager.persist(new Gizmo(2L, "discarded"));
    transaction.addPostProcess(new FlaggingPostProcess(ran, TransactionEndState.ROLLBACK, ProcessPriority.MIDDLE));

    transaction.rollback();

    Assert.assertTrue(ran[0], "the ROLLBACK post-process should have run after a rollback");
    Assert.assertNull(findGizmo(2L), "a rolled-back durable should not be present in the database");
  }

  @Test(groups = "integration", expectedExceptions = ProxyTransactionException.class)
  public void testRollbackOnlyCommitThrowsAndDiscardsEntity () {

    JPAProxySession session = session();
    EntityManager entityManager = session.getNativeSession();
    JPAProxyTransaction transaction = new JPAProxyTransaction(session, entityManager.getTransaction());

    entityManager.persist(new Gizmo(3L, "rollback-only"));
    transaction.setRollbackOnly();

    try {
      transaction.commit();
    } finally {
      Assert.assertNull(findGizmo(3L), "committing a rollback-only transaction must not persist the durable");
    }
  }

  public void testCommitPostProcessFailureWrapsInProxyTransactionExceptionButKeepsCommit () {

    JPAProxySession session = session();
    EntityManager entityManager = session.getNativeSession();
    JPAProxyTransaction transaction = new JPAProxyTransaction(session, entityManager.getTransaction());

    entityManager.persist(new Gizmo(4L, "committed-then-failed"));
    transaction.addPostProcess(new FailingPostProcess(new IllegalStateException("commit post-process boom"), TransactionEndState.COMMIT, ProcessPriority.MIDDLE));

    try {
      transaction.commit();
      Assert.fail("a failing COMMIT post-process should surface as a ProxyTransactionException");
    } catch (ProxyTransactionException proxyTransactionException) {
      Assert.assertTrue(containsThrowable(proxyTransactionException, TransactionPostProcessException.class), "the wrapped post-process failure should be in the cause chain");
    }

    // The commit itself completed before post-processing ran, so the durable must still be present.
    Assert.assertNotNull(findGizmo(4L), "the durable was committed before the post-process failed and should remain persisted");
  }

  public void testRollbackPostProcessFailureChainsWithRollbackOnlyCause () {

    JPAProxySession session = session();
    EntityManager entityManager = session.getNativeSession();
    JPAProxyTransaction transaction = new JPAProxyTransaction(session, entityManager.getTransaction());

    entityManager.persist(new Gizmo(5L, "chained"));
    transaction.setRollbackOnly();
    transaction.addPostProcess(new FailingPostProcess(new IllegalStateException("rollback post-process boom"), TransactionEndState.ROLLBACK, ProcessPriority.MIDDLE));

    try {
      transaction.commit();
      Assert.fail("a rollback-only commit whose ROLLBACK post-process fails should throw a ProxyTransactionException");
    } catch (ProxyTransactionException proxyTransactionException) {

      // The thrown exception must chain BOTH causes: the post-process failure (wrapped in a
      // TransactionPostProcessException) and the rollback-only marker that initiated the rollback.
      Assert.assertTrue(containsThrowable(proxyTransactionException, TransactionPostProcessException.class), "the failing ROLLBACK post-process should be present in the cause chain");
      Assert.assertTrue(containsRollbackOnlyCause(proxyTransactionException), "the rollback-only ProxyTransactionException that triggered the rollback should be chained in");
    }

    Assert.assertNull(findGizmo(5L), "the rollback-only durable should not be persisted");
  }

  private static boolean containsThrowable (Throwable throwable, Class<? extends Throwable> type) {

    for (Throwable cursor = throwable; cursor != null; cursor = cursor.getCause()) {
      if (type.isInstance(cursor)) {

        return true;
      }
    }

    return false;
  }

  private static boolean containsRollbackOnlyCause (Throwable throwable) {

    for (Throwable cursor = throwable; cursor != null; cursor = cursor.getCause()) {
      if ((cursor instanceof ProxyTransactionException) && (cursor.getMessage() != null) && cursor.getMessage().contains("rollback only")) {

        return true;
      }
    }

    return false;
  }

  @Entity
  public static class Gizmo {

    @Id
    private Long id;
    private String name;

    public Gizmo () {

    }

    public Gizmo (Long id, String name) {

      this.id = id;
      this.name = name;
    }

    public Long getId () {

      return id;
    }

    public void setId (Long id) {

      this.id = id;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }
  }

  private static class FlaggingPostProcess extends TransactionPostProcess {

    private final boolean[] ran;

    private FlaggingPostProcess (boolean[] ran, TransactionEndState endState, ProcessPriority priority) {

      super(endState, priority);

      this.ran = ran;
    }

    @Override
    public void process () {

      ran[0] = true;
    }
  }

  private static class FailingPostProcess extends TransactionPostProcess {

    private final Exception failure;

    private FailingPostProcess (Exception failure, TransactionEndState endState, ProcessPriority priority) {

      super(endState, priority);

      this.failure = failure;
    }

    @Override
    public void process ()
      throws Exception {

      throw failure;
    }
  }
}
