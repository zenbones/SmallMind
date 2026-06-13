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
package org.smallmind.web.json.query.querydsl;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathBuilder;
import org.smallmind.persistence.Durable;
import org.smallmind.web.json.query.WherePath;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises each {@link QueryDslWhereFieldTransformer} constructor variant by resolving a field through
 * {@code transform(entity, name)} and inspecting the produced {@link QueryDslWherePath} (no database needed).
 */
@Test(groups = "unit")
public class QueryDslWhereFieldTransformerTest {

  public void testFixedDurableRootConstructor () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(root);

    WherePath<Path<?>, Path<?>> wherePath = transformer.transform("person", "age");

    Assert.assertSame(wherePath.getRoot(), root);
    Assert.assertEquals(wherePath.getPath().toString(), "person.age");
    Assert.assertEquals(wherePath.getField(), "age");
  }

  public void testFixedDurableRootConstructorIgnoresEntityArgument () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(root);

    WherePath<Path<?>, Path<?>> wherePath = transformer.transform(null, "status");

    Assert.assertSame(wherePath.getRoot(), root);
    Assert.assertEquals(wherePath.getPath().toString(), "person.status");
    Assert.assertEquals(wherePath.getField(), "status");
  }

  public void testNameOperatorConstructorTransformsFieldName () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    UnaryOperator<String> nameOperator = (name) -> "db_" + name;
    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(root, nameOperator);

    WherePath<Path<?>, Path<?>> wherePath = transformer.transform("person", "age");

    Assert.assertSame(wherePath.getRoot(), root);
    Assert.assertEquals(wherePath.getPath().toString(), "person.db_age");
    Assert.assertEquals(wherePath.getField(), "db_age");
  }

  public void testNameOperatorConstructorIdentityOperator () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(root, UnaryOperator.identity());

    WherePath<Path<?>, Path<?>> wherePath = transformer.transform("person", "name");

    Assert.assertEquals(wherePath.getPath().toString(), "person.name");
    Assert.assertEquals(wherePath.getField(), "name");
  }

  public void testPathAndNameBiFunctionConstructor () {

    PathBuilder<PersonDurable> personRoot = new PathBuilder<>(PersonDurable.class, "person");
    PathBuilder<PersonDurable> accountRoot = new PathBuilder<>(PersonDurable.class, "account");

    BiFunction<String, String, Path<? extends Durable<?>>> pathFunction = (entity, name) -> "account".equals(entity) ? accountRoot : personRoot;
    BiFunction<String, String, String> nameFunction = (entity, name) -> name.toUpperCase();

    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(pathFunction, nameFunction);

    WherePath<Path<?>, Path<?>> personPath = transformer.transform("person", "age");

    Assert.assertSame(personPath.getRoot(), personRoot);
    Assert.assertEquals(personPath.getPath().toString(), "person.AGE");
    Assert.assertEquals(personPath.getField(), "AGE");

    WherePath<Path<?>, Path<?>> accountPath = transformer.transform("account", "status");

    Assert.assertSame(accountPath.getRoot(), accountRoot);
    Assert.assertEquals(accountPath.getPath().toString(), "account.STATUS");
    Assert.assertEquals(accountPath.getField(), "STATUS");
  }

  public void testDirectPathBiFunctionConstructor () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    BiFunction<String, String, Path<?>> pathFunction = (entity, name) -> root.get(name);

    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(pathFunction);

    WherePath<Path<?>, Path<?>> wherePath = transformer.transform("person", "age");

    Assert.assertEquals(wherePath.getRoot().toString(), "person");
    Assert.assertEquals(wherePath.getPath().toString(), "person.age");
    Assert.assertEquals(wherePath.getField(), "age");
  }

  public void testDirectPathBiFunctionConstructorWithNestedField () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    BiFunction<String, String, Path<?>> pathFunction = (entity, name) -> root.get("address").getString(name);

    QueryDslWhereFieldTransformer transformer = new QueryDslWhereFieldTransformer(pathFunction);

    WherePath<Path<?>, Path<?>> wherePath = transformer.transform("person", "city");

    Assert.assertEquals(wherePath.getRoot().toString(), "person");
    Assert.assertEquals(wherePath.getPath().toString(), "person.address.city");
    Assert.assertEquals(wherePath.getField(), "address.city");
  }
}
