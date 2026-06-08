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
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
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
 * Integration test for the JPQL- and criteria-driven query and mutation surface of {@link JPADao} against a real
 * MySQL container started through the docker testbench. Where {@link JPADaoIntegrationTest} covers the native CRUD
 * and id-based {@code list} methods, this test exercises the {@code QueryDetails}/{@code CriteriaQueryDetails}
 * read path ({@code findByQuery}, {@code findByCriteria}, {@code listByQuery}, {@code listByCriteria}) and the
 * bulk-mutation path ({@code executeWithQuery}, {@code executeWithCriteria}, {@code deleteWithCriteria}),
 * including both the managed-type overloads and the explicit return-/criteria-type overloads.
 *
 * <p>The session is built with {@code boundaryEnforced = false} so the DAO can be driven directly. Bulk JPQL and
 * criteria update/delete statements flush through the entity manager and so are wrapped in an explicit
 * {@link JPAProxyTransaction}; their effects are then verified through a fresh entity manager so the first-level
 * cache of the DAO's own session cannot mask a missing database change. Each test starts from an empty table.
 *
 * <p>Requires a running Docker daemon; the {@code mysql:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class JPADaoCriteriaIntegrationTest extends AbstractGroundwaterTest {

  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String JDBC_URL = "jdbc:mysql://localhost:3306/groundwater?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&sslMode=DISABLED";
  private static final String USER_NAME = "root";
  private static final String PASSWORD = "secret";

  private EntityManagerFactory entityManagerFactory;

  public JPADaoCriteriaIntegrationTest () {

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

    persistenceUnitInfo.setPersistenceUnitName("jpa-dao-criteria-it");
    persistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    persistenceUnitInfo.setNonJtaDataSource(dataSource);
    persistenceUnitInfo.addManagedClassName(Widget.class.getName());
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
      entityManager.createQuery("delete from Widget").executeUpdate();
      entityManager.getTransaction().commit();
    } finally {
      entityManager.close();
    }
  }

  private JPAProxySession session () {

    return new JPAProxySession("mysql", null, entityManagerFactory, false, false);
  }

  private void persistInTransaction (JPAProxySession session, WidgetDao dao, Widget... widgets) {

    JPAProxyTransaction transaction = session.beginTransaction();

    for (Widget widget : widgets) {
      dao.persist(widget);
    }

    transaction.commit();
  }

  private Widget findWidget (long id) {

    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {

      return entityManager.find(Widget.class, id);
    } finally {
      entityManager.close();
    }
  }

  public void testFindByQueryReturnsSingleManagedDurable () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "alpha", 5), new Widget(2L, "beta", 9));

      Widget found = dao.findByQuery(new QueryDetails() {

        @Override
        public String getQueryString () {

          return "select w from Widget w where w.name = :name";
        }

        @Override
        public Query completeQuery (Query query) {

          return query.setParameter("name", "beta");
        }
      });

      Assert.assertNotNull(found, "the single matching durable should be returned");
      Assert.assertEquals(found.getId(), Long.valueOf(2L));
      Assert.assertEquals(found.getQuantity(), Integer.valueOf(9));
    } finally {
      session.close();
    }
  }

  public void testFindByQueryWithReturnTypeProjectsScalar () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "alpha", 5));

      String name = dao.findByQuery(String.class, new QueryDetails() {

        @Override
        public String getQueryString () {

          return "select w.name from Widget w where w.id = :id";
        }

        @Override
        public Query completeQuery (Query query) {

          return query.setParameter("id", 1L);
        }
      });

      Assert.assertEquals(name, "alpha", "the projected scalar column should be returned as the requested type");
    } finally {
      session.close();
    }
  }

  public void testListByQueryReturnsManagedDurables () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "a", 1), new Widget(2L, "b", 2), new Widget(3L, "c", 3));

      List<Widget> widgets = dao.listByQuery(new QueryDetails() {

        @Override
        public String getQueryString () {

          return "select w from Widget w where w.quantity >= :floor order by w.id";
        }

        @Override
        public Query completeQuery (Query query) {

          return query.setParameter("floor", 2);
        }
      });

      Assert.assertEquals(widgets.size(), 2, "only durables at or above the quantity floor should be listed");
      Assert.assertEquals(widgets.get(0).getId(), Long.valueOf(2L));
      Assert.assertEquals(widgets.get(1).getId(), Long.valueOf(3L));
    } finally {
      session.close();
    }
  }

  public void testListByQueryWithReturnTypeProjectsScalars () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "a", 1), new Widget(2L, "b", 2));

      List<String> names = dao.listByQuery(String.class, new QueryDetails() {

        @Override
        public String getQueryString () {

          return "select w.name from Widget w order by w.id";
        }

        @Override
        public Query completeQuery (Query query) {

          return query;
        }
      });

      Assert.assertEquals(names, List.of("a", "b"), "scalar projections should be listed in query order");
    } finally {
      session.close();
    }
  }

  public void testFindByCriteriaReturnsSingleManagedDurable () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "alpha", 5), new Widget(2L, "beta", 9));

      Widget found = dao.findByCriteria(new CriteriaQueryDetails<Widget>() {

        @Override
        public CriteriaQuery<Widget> completeCriteria (Class<Widget> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaQuery<Widget> query = criteriaBuilder.createQuery(Widget.class);
          Root<Widget> root = query.from(Widget.class);

          return query.select(root).where(criteriaBuilder.equal(root.get("id"), 2L));
        }
      });

      Assert.assertNotNull(found, "the single durable matching the criteria should be returned");
      Assert.assertEquals(found.getName(), "beta");
    } finally {
      session.close();
    }
  }

  public void testFindByCriteriaWithReturnTypeProjectsCount () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "a", 1), new Widget(2L, "b", 2), new Widget(3L, "c", 3));

      Long count = dao.findByCriteria(Long.class, new CriteriaQueryDetails<Long>() {

        @Override
        public CriteriaQuery<Long> completeCriteria (Class<Long> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
          Root<Widget> root = query.from(Widget.class);

          return query.select(criteriaBuilder.count(root));
        }
      });

      Assert.assertEquals(count, Long.valueOf(3L), "the aggregate count should reflect every persisted durable");
    } finally {
      session.close();
    }
  }

  public void testListByCriteriaReturnsManagedDurables () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "a", 1), new Widget(2L, "b", 8), new Widget(3L, "c", 9));

      List<Widget> widgets = dao.listByCriteria(new CriteriaQueryDetails<Widget>() {

        @Override
        public CriteriaQuery<Widget> completeCriteria (Class<Widget> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaQuery<Widget> query = criteriaBuilder.createQuery(Widget.class);
          Root<Widget> root = query.from(Widget.class);

          return query.select(root).where(criteriaBuilder.gt(root.<Integer>get("quantity"), 5)).orderBy(criteriaBuilder.asc(root.get("id")));
        }
      });

      Assert.assertEquals(widgets.size(), 2, "only durables above the quantity bound should be listed");
      Assert.assertEquals(widgets.get(0).getId(), Long.valueOf(2L));
      Assert.assertEquals(widgets.get(1).getId(), Long.valueOf(3L));
    } finally {
      session.close();
    }
  }

  public void testListByCriteriaWithReturnTypeProjectsScalars () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "a", 1), new Widget(2L, "b", 2));

      List<String> names = dao.listByCriteria(String.class, new CriteriaQueryDetails<String>() {

        @Override
        public CriteriaQuery<String> completeCriteria (Class<String> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
          Root<Widget> root = query.from(Widget.class);

          return query.select(root.<String>get("name")).orderBy(criteriaBuilder.asc(root.get("id")));
        }
      });

      Assert.assertEquals(names, List.of("a", "b"), "scalar projections should be listed in criteria order");
    } finally {
      session.close();
    }
  }

  public void testExecuteWithQueryRunsBulkJpqlUpdate () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "before", 5));

      JPAProxyTransaction transaction = session.beginTransaction();
      int affected = dao.executeWithQuery(new QueryDetails() {

        @Override
        public String getQueryString () {

          return "update Widget w set w.name = :name where w.id = :id";
        }

        @Override
        public Query completeQuery (Query query) {

          return query.setParameter("name", "after").setParameter("id", 1L);
        }
      });

      transaction.commit();

      Assert.assertEquals(affected, 1, "the bulk update should report a single affected row");
      Assert.assertEquals(findWidget(1L).getName(), "after", "the bulk update should be visible to a fresh entity manager");
    } finally {
      session.close();
    }
  }

  public void testExecuteWithCriteriaRunsBulkCriteriaUpdate () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "before", 5), new Widget(2L, "keep", 5));

      JPAProxyTransaction transaction = session.beginTransaction();
      int affected = dao.executeWithCriteria(new CriteriaUpdateDetails<Widget>() {

        @Override
        public CriteriaUpdate<Widget> completeCriteria (Class<Widget> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaUpdate<Widget> update = criteriaBuilder.createCriteriaUpdate(Widget.class);
          Root<Widget> root = update.from(Widget.class);

          return update.set("name", "after").where(criteriaBuilder.equal(root.get("id"), 1L));
        }
      });

      transaction.commit();

      Assert.assertEquals(affected, 1, "only the targeted row should be updated");
      Assert.assertEquals(findWidget(1L).getName(), "after");
      Assert.assertEquals(findWidget(2L).getName(), "keep", "the untargeted row should be left untouched");
    } finally {
      session.close();
    }
  }

  public void testExecuteWithCriteriaForExplicitTypeRunsBulkCriteriaUpdate () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "before", 5));

      JPAProxyTransaction transaction = session.beginTransaction();
      int affected = dao.executeWithCriteria(Widget.class, new CriteriaUpdateDetails<Widget>() {

        @Override
        public CriteriaUpdate<Widget> completeCriteria (Class<Widget> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaUpdate<Widget> update = criteriaBuilder.createCriteriaUpdate(criteriaClass);
          Root<Widget> root = update.from(criteriaClass);

          return update.set("quantity", 42).where(criteriaBuilder.equal(root.get("id"), 1L));
        }
      });

      transaction.commit();

      Assert.assertEquals(affected, 1, "the explicit-type overload should update the targeted row");
      Assert.assertEquals(findWidget(1L).getQuantity(), Integer.valueOf(42));
    } finally {
      session.close();
    }
  }

  public void testDeleteWithCriteriaRemovesMatchingRows () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "doomed", 1), new Widget(2L, "doomed", 2), new Widget(3L, "spared", 3));

      JPAProxyTransaction transaction = session.beginTransaction();
      int affected = dao.deleteWithCriteria(new CriteriaDeleteDetails<Widget>() {

        @Override
        public CriteriaDelete<Widget> completeCriteria (Class<Widget> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaDelete<Widget> delete = criteriaBuilder.createCriteriaDelete(Widget.class);
          Root<Widget> root = delete.from(Widget.class);

          return delete.where(criteriaBuilder.equal(root.get("name"), "doomed"));
        }
      });

      transaction.commit();

      Assert.assertEquals(affected, 2, "both matching rows should be deleted");
      Assert.assertNull(findWidget(1L));
      Assert.assertNull(findWidget(2L));
      Assert.assertNotNull(findWidget(3L), "the non-matching row should survive");
    } finally {
      session.close();
    }
  }

  public void testDeleteWithCriteriaForExplicitTypeRemovesMatchingRows () {

    JPAProxySession session = session();

    try {

      WidgetDao dao = new WidgetDao(session);

      persistInTransaction(session, dao, new Widget(1L, "a", 1), new Widget(2L, "b", 2));

      JPAProxyTransaction transaction = session.beginTransaction();
      int affected = dao.deleteWithCriteria(Widget.class, new CriteriaDeleteDetails<Widget>() {

        @Override
        public CriteriaDelete<Widget> completeCriteria (Class<Widget> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaDelete<Widget> delete = criteriaBuilder.createCriteriaDelete(criteriaClass);
          Root<Widget> root = delete.from(criteriaClass);

          return delete.where(criteriaBuilder.equal(root.get("id"), 2L));
        }
      });

      transaction.commit();

      Assert.assertEquals(affected, 1, "the explicit-type overload should delete the targeted row");
      Assert.assertNotNull(findWidget(1L));
      Assert.assertNull(findWidget(2L));
    } finally {
      session.close();
    }
  }

  @Entity(name = "Widget")
  public static class Widget extends AbstractDurable<Long, Widget> {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;
    private String name;
    private Integer quantity;

    public Widget () {

    }

    public Widget (Long id, String name, Integer quantity) {

      this.id = id;
      this.name = name;
      this.quantity = quantity;
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

    public Integer getQuantity () {

      return quantity;
    }

    public void setQuantity (Integer quantity) {

      this.quantity = quantity;
    }
  }

  private static class WidgetDao extends JPADao<Long, Widget> {

    private WidgetDao (JPAProxySession proxySession) {

      super(proxySession);
    }
  }
}
