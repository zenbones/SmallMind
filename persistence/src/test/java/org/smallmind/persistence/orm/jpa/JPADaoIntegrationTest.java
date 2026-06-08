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
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.smallmind.persistence.AbstractDurable;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration test for {@link JPADao} against a real MySQL container started through the docker testbench.
 * It exercises the DAO's native CRUD and criteria-query surface end to end over a genuine Hibernate
 * {@link EntityManager}: insert via {@code merge}, id lookup ({@code acquire}/{@code get}), update,
 * delete, and the {@code list} variants (all, by id collection, and by id lower bound).
 *
 * <p>The session is built with {@code boundaryEnforced = false} so the DAO can be driven directly; writes
 * are wrapped in an explicit {@link JPAProxyTransaction} because {@code persist}/{@code delete} flush the
 * entity manager, which requires an active transaction. Each test starts from an empty table so the
 * {@code list} assertions are deterministic.
 *
 * <p>Requires a running Docker daemon; the {@code mysql:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class JPADaoIntegrationTest extends AbstractGroundwaterTest {

  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String JDBC_URL = "jdbc:mysql://localhost:3306/groundwater?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&sslMode=DISABLED";
  private static final String USER_NAME = "root";
  private static final String PASSWORD = "secret";

  private EntityManagerFactory entityManagerFactory;

  public JPADaoIntegrationTest () {

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

    persistenceUnitInfo.setPersistenceUnitName("jpa-dao-it");
    persistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    persistenceUnitInfo.setNonJtaDataSource(dataSource);
    persistenceUnitInfo.addManagedClassName(Gadget.class.getName());
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

  @BeforeMethod
  public void clearTable () {

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      entityManager.createQuery("delete from Gadget").executeUpdate();
      entityManager.getTransaction().commit();
    } finally {
      entityManager.close();
    }
  }

  private JPAProxySession session () {

    return new JPAProxySession("mysql", null, entityManagerFactory, false, false);
  }

  private void persistInTransaction (JPAProxySession session, GadgetDao dao, Gadget... gadgets) {

    JPAProxyTransaction transaction = session.beginTransaction();

    for (Gadget gadget : gadgets) {
      dao.persist(gadget);
    }

    transaction.commit();
  }

  public void testPersistInsertsAndGetRoundTripsThroughDatabase () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(1L, "alpha"));

      Gadget fetched = dao.get(1L);

      Assert.assertNotNull(fetched, "a persisted durable should be retrievable by id");
      Assert.assertEquals(fetched.getId(), Long.valueOf(1L));
      Assert.assertEquals(fetched.getName(), "alpha");
    } finally {
      session.close();
    }
  }

  public void testAcquireReadsDirectlyFromTheEntityManager () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(2L, "beta"));

      Gadget acquired = dao.acquire(Gadget.class, 2L);

      Assert.assertNotNull(acquired, "acquire should load the durable straight from the entity manager");
      Assert.assertEquals(acquired.getName(), "beta");
    } finally {
      session.close();
    }
  }

  public void testGetReturnsNullForMissingId () {

    JPAProxySession session = session();

    try {
      Assert.assertNull(new GadgetDao(session).get(999L), "an id with no row should resolve to null");
    } finally {
      session.close();
    }
  }

  public void testPersistMergesUpdatesOntoExistingRow () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(3L, "first"));
      persistInTransaction(session, dao, new Gadget(3L, "second"));

      Assert.assertEquals(dao.get(3L).getName(), "second", "re-persisting the same id should update the row, not duplicate it");
      Assert.assertEquals(dao.list().size(), 1);
    } finally {
      session.close();
    }
  }

  public void testDeleteRemovesDurable () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(4L, "doomed"));

      JPAProxyTransaction transaction = session.beginTransaction();

      dao.delete(new Gadget(4L, "doomed"));
      transaction.commit();

      Assert.assertNull(dao.get(4L), "a deleted durable should no longer be present");
    } finally {
      session.close();
    }
  }

  public void testListReturnsEveryPersistedDurable () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(10L, "a"), new Gadget(11L, "b"), new Gadget(12L, "c"));

      Assert.assertEquals(dao.list().size(), 3);
    } finally {
      session.close();
    }
  }

  public void testListByIdCollectionReturnsOnlyTheRequestedSubset () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(20L, "a"), new Gadget(21L, "b"), new Gadget(22L, "c"));

      List<Gadget> subset = dao.list(List.of(20L, 22L));

      Assert.assertEquals(subset.size(), 2);
      Assert.assertTrue(subset.contains(new Gadget(20L, "a")), "the subset should contain the requested id 20");
      Assert.assertTrue(subset.contains(new Gadget(22L, "c")), "the subset should contain the requested id 22");
      Assert.assertFalse(subset.contains(new Gadget(21L, "b")), "the unrequested id 21 should be absent");
    } finally {
      session.close();
    }
  }

  public void testListGreaterThanReturnsHigherIdsUpToFetchSize () {

    JPAProxySession session = session();

    try {

      GadgetDao dao = new GadgetDao(session);

      persistInTransaction(session, dao, new Gadget(30L, "a"), new Gadget(31L, "b"), new Gadget(32L, "c"));

      List<Gadget> higher = dao.list(30L, 10);

      Assert.assertEquals(higher.size(), 2, "only ids strictly greater than 30 should be returned");
      Assert.assertFalse(higher.contains(new Gadget(30L, "a")), "the lower-bound id itself should be excluded");
      Assert.assertTrue(higher.contains(new Gadget(31L, "b")));
      Assert.assertTrue(higher.contains(new Gadget(32L, "c")));
    } finally {
      session.close();
    }
  }

  @Entity(name = "Gadget")
  public static class Gadget extends AbstractDurable<Long, Gadget> {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;
    private String name;

    public Gadget () {

    }

    public Gadget (Long id, String name) {

      this.id = id;
      this.name = name;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
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

  private static class GadgetDao extends JPADao<Long, Gadget> {

    private GadgetDao (JPAProxySession proxySession) {

      super(proxySession);
    }
  }
}
