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

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises every {@link QueryDslWherePath} constructor and accessor with {@link PathBuilder}-built paths (no
 * entity Q-types or database needed).
 */
@Test(groups = "unit")
public class QueryDslWherePathTest {

  public void testSinglePathConstructorDerivesRootAndField () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    Path<?> agePath = root.get("age");

    QueryDslWherePath wherePath = new QueryDslWherePath(agePath);

    Assert.assertEquals(wherePath.getRoot().toString(), "person");
    Assert.assertSame(wherePath.getPath(), agePath);
    Assert.assertEquals(wherePath.getField(), "age");
  }

  public void testSinglePathConstructorWithNestedField () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    Path<?> nestedPath = root.get("address").getString("city");

    QueryDslWherePath wherePath = new QueryDslWherePath(nestedPath);

    Assert.assertEquals(wherePath.getRoot().toString(), "person");
    Assert.assertSame(wherePath.getPath(), nestedPath);
    Assert.assertEquals(wherePath.getField(), "address.city");
  }

  public void testDurableRootAndFieldConstructorAppendsField () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");

    QueryDslWherePath wherePath = new QueryDslWherePath(root, "status");

    Assert.assertSame(wherePath.getRoot(), root);
    Assert.assertEquals(wherePath.getPath().toString(), "person.status");
    Assert.assertEquals(wherePath.getField(), "status");
  }

  public void testExplicitThreeComponentConstructorRetainsEachComponent () {

    PathBuilder<PersonDurable> root = new PathBuilder<>(PersonDurable.class, "person");
    Path<?> namePath = root.getString("name");

    QueryDslWherePath wherePath = new QueryDslWherePath(root, namePath, "name");

    Assert.assertSame(wherePath.getRoot(), root);
    Assert.assertSame(wherePath.getPath(), namePath);
    Assert.assertEquals(wherePath.getField(), "name");
  }
}
