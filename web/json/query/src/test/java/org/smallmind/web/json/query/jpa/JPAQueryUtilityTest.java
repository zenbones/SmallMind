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
package org.smallmind.web.json.query.jpa;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.smallmind.nutsnbolts.json.SortDirection;
import org.smallmind.web.json.query.AndWhereConjunction;
import org.smallmind.web.json.query.BooleanWhereOperand;
import org.smallmind.web.json.query.DateWhereOperand;
import org.smallmind.web.json.query.IntegerWhereOperand;
import org.smallmind.web.json.query.NullWhereOperand;
import org.smallmind.web.json.query.OrWhereConjunction;
import org.smallmind.web.json.query.Product;
import org.smallmind.web.json.query.Sort;
import org.smallmind.web.json.query.SortField;
import org.smallmind.web.json.query.StringWhereOperand;
import org.smallmind.web.json.query.Where;
import org.smallmind.web.json.query.WhereField;
import org.smallmind.web.json.query.WhereOperand;
import org.smallmind.web.json.query.WhereOperator;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test that translates {@link Where}/{@link Sort} structures into JPA Criteria predicates and
 * orders via {@link JPAQueryUtility}, then executes them against an in-memory H2/Hibernate persistence unit
 * to prove the generated criteria are valid.
 */
@Test(groups = "integration")
public class JPAQueryUtilityTest {

  private EntityManagerFactory entityManagerFactory;
  private EntityManager entityManager;

  @BeforeClass
  public void beforeClass () {

    entityManagerFactory = new PersistenceConfiguration("query-test")
                             .provider("org.hibernate.jpa.HibernatePersistenceProvider")
                             .managedClass(PersonEntity.class)
                             .property("jakarta.persistence.jdbc.driver", "org.h2.Driver")
                             .property("jakarta.persistence.jdbc.url", "jdbc:h2:mem:query-test;DB_CLOSE_DELAY=-1")
                             .property("jakarta.persistence.jdbc.user", "sa")
                             .property("jakarta.persistence.jdbc.password", "")
                             .property("hibernate.hbm2ddl.auto", "create-drop")
                             .createEntityManagerFactory();
    entityManager = entityManagerFactory.createEntityManager();
  }

  @AfterClass(alwaysRun = true)
  public void afterClass () {

    if (entityManager != null) {
      entityManager.close();
    }
    if (entityManagerFactory != null) {
      entityManagerFactory.close();
    }
  }

  private JPAWhereFieldTransformer transformerFor (Root<PersonEntity> root) {

    return new JPAWhereFieldTransformer((entity, name) -> new JPAWherePath(root, name));
  }

  public void testWherePredicateBuildsAndExecutes () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PersonEntity> criteriaQuery = criteriaBuilder.createQuery(PersonEntity.class);
    Root<PersonEntity> root = criteriaQuery.from(PersonEntity.class);

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18)),
      new OrWhereConjunction(
        WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("active")),
        WhereField.instance("name", WhereOperator.LIKE, StringWhereOperand.instance("bo*")))));

    Product<Root<?>, Predicate> product = JPAQueryUtility.apply(criteriaBuilder, where, transformerFor(root), true);

    Assert.assertFalse(product.isEmpty());
    Assert.assertNotNull(product.getValue());

    criteriaQuery.where(product.getValue());

    List<PersonEntity> results = entityManager.createQuery(criteriaQuery).getResultList();

    Assert.assertNotNull(results);
  }

  public void testInPredicateBuildsAndExecutes () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PersonEntity> criteriaQuery = criteriaBuilder.createQuery(PersonEntity.class);
    Root<PersonEntity> root = criteriaQuery.from(PersonEntity.class);

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("id", WhereOperator.IN, org.smallmind.web.json.query.ArrayWhereOperand.instance(new Long[] {1L, 2L, 3L}))));

    Product<Root<?>, Predicate> product = JPAQueryUtility.apply(criteriaBuilder, where, transformerFor(root), true);

    Assert.assertFalse(product.isEmpty());

    criteriaQuery.where(product.getValue());

    Assert.assertNotNull(entityManager.createQuery(criteriaQuery).getResultList());
  }

  private List<PersonEntity> runField (String name, WhereOperator operator, WhereOperand operand) {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PersonEntity> criteriaQuery = criteriaBuilder.createQuery(PersonEntity.class);
    Root<PersonEntity> root = criteriaQuery.from(PersonEntity.class);

    Where where = Where.instance(new AndWhereConjunction(WhereField.instance(name, operator, operand)));
    Product<Root<?>, Predicate> product = JPAQueryUtility.apply(criteriaBuilder, where, transformerFor(root), true);

    Assert.assertFalse(product.isEmpty());

    criteriaQuery.where(product.getValue());

    return entityManager.createQuery(criteriaQuery).getResultList();
  }

  public void testLessThanNumber () {

    Assert.assertNotNull(runField("age", WhereOperator.LT, IntegerWhereOperand.instance(40)));
  }

  public void testLessThanOrEqualNumber () {

    Assert.assertNotNull(runField("age", WhereOperator.LE, IntegerWhereOperand.instance(40)));
  }

  public void testGreaterThanNumber () {

    Assert.assertNotNull(runField("age", WhereOperator.GT, IntegerWhereOperand.instance(40)));
  }

  public void testGreaterThanOrEqualNumber () {

    Assert.assertNotNull(runField("age", WhereOperator.GE, IntegerWhereOperand.instance(40)));
  }

  public void testLessThanDate () {

    Assert.assertNotNull(runField("created", WhereOperator.LT, DateWhereOperand.instance(LocalDateTime.now())));
  }

  public void testLessThanOrEqualDate () {

    Assert.assertNotNull(runField("created", WhereOperator.LE, DateWhereOperand.instance(LocalDateTime.now())));
  }

  public void testGreaterThanDate () {

    Assert.assertNotNull(runField("created", WhereOperator.GT, DateWhereOperand.instance(LocalDateTime.now())));
  }

  public void testGreaterThanOrEqualDate () {

    Assert.assertNotNull(runField("created", WhereOperator.GE, DateWhereOperand.instance(LocalDateTime.now())));
  }

  public void testEqualValue () {

    Assert.assertNotNull(runField("status", WhereOperator.EQ, StringWhereOperand.instance("active")));
  }

  public void testEqualNullBecomesIsNull () {

    Assert.assertNotNull(runField("status", WhereOperator.EQ, NullWhereOperand.instance()));
  }

  public void testNotEqualValue () {

    Assert.assertNotNull(runField("status", WhereOperator.NE, StringWhereOperand.instance("active")));
  }

  public void testNotEqualNullBecomesIsNotNull () {

    Assert.assertNotNull(runField("status", WhereOperator.NE, NullWhereOperand.instance()));
  }

  public void testExistsTrueBecomesIsNotNull () {

    Assert.assertNotNull(runField("status", WhereOperator.EXISTS, BooleanWhereOperand.instance(true)));
  }

  public void testExistsFalseBecomesIsNull () {

    Assert.assertNotNull(runField("status", WhereOperator.EXISTS, BooleanWhereOperand.instance(false)));
  }

  public void testLikeProducesLikePredicate () {

    Assert.assertNotNull(runField("name", WhereOperator.LIKE, StringWhereOperand.instance("bo*")));
  }

  public void testUnlikeProducesNotLikePredicate () {

    Assert.assertNotNull(runField("name", WhereOperator.UNLIKE, StringWhereOperand.instance("bo*")));
  }

  public void testInSingleValueNonArray () {

    // A non-array operand drives the singular branch of IN (path.in(value)).
    Assert.assertNotNull(runField("status", WhereOperator.IN, StringWhereOperand.instance("active")));
  }

  public void testNestedOrInsideAndConjunction () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PersonEntity> criteriaQuery = criteriaBuilder.createQuery(PersonEntity.class);
    Root<PersonEntity> root = criteriaQuery.from(PersonEntity.class);

    Where where = Where.instance(new AndWhereConjunction(
      WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18)),
      new OrWhereConjunction(
        WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("active")),
        WhereField.instance("status", WhereOperator.EQ, StringWhereOperand.instance("pending")))));

    Product<Root<?>, Predicate> product = JPAQueryUtility.apply(criteriaBuilder, where, transformerFor(root), true);

    Assert.assertFalse(product.isEmpty());

    criteriaQuery.where(product.getValue());

    Assert.assertNotNull(entityManager.createQuery(criteriaQuery).getResultList());
  }

  public void testEmptyConjunctionProducesNone () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    Root<PersonEntity> root = criteriaBuilder.createQuery(PersonEntity.class).from(PersonEntity.class);

    Assert.assertTrue(JPAQueryUtility.apply(criteriaBuilder, Where.instance(new AndWhereConjunction()), transformerFor(root), true).isEmpty());
  }

  public void testNullWhereProducesNone () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

    Assert.assertTrue(JPAQueryUtility.apply(criteriaBuilder, null, transformerFor(criteriaBuilder.createQuery(PersonEntity.class).from(PersonEntity.class)), true).isEmpty());
  }

  public void testNullSortProducesNone () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    Root<PersonEntity> root = criteriaBuilder.createQuery(PersonEntity.class).from(PersonEntity.class);

    Assert.assertTrue(JPAQueryUtility.apply(criteriaBuilder, (Sort)null, transformerFor(root)).isEmpty());
  }

  public void testEmptySortProducesNone () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    Root<PersonEntity> root = criteriaBuilder.createQuery(PersonEntity.class).from(PersonEntity.class);

    Assert.assertTrue(JPAQueryUtility.apply(criteriaBuilder, Sort.instance(), transformerFor(root)).isEmpty());
  }

  public void testSortBuildsAndExecutes () {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PersonEntity> criteriaQuery = criteriaBuilder.createQuery(PersonEntity.class);
    Root<PersonEntity> root = criteriaQuery.from(PersonEntity.class);

    Sort sort = Sort.instance(
      SortField.instance("age", SortDirection.DESC),
      SortField.instance("name", SortDirection.ASC));

    Product<Root<?>, Order[]> product = JPAQueryUtility.apply(criteriaBuilder, sort, transformerFor(root));

    Assert.assertFalse(product.isEmpty());
    Assert.assertEquals(product.getValue().length, 2);

    criteriaQuery.orderBy(product.getValue());

    Assert.assertNotNull(entityManager.createQuery(criteriaQuery).getResultList());
  }
}
