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
package org.smallmind.mongodb.throng.query;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class QueryTest {

  public void testEmptyFactoryCreatesQueryPreConfiguredWithEmptyFilter () {

    Query query = Query.empty();

    Assert.assertNotNull(query);
  }

  public void testFluentBuilderReturnsSameInstanceForChaining () {

    Query query = Query.with();

    Assert.assertSame(query.filter(Filter.empty()), query);
    Assert.assertSame(query.sort(Sort.on()), query);
    Assert.assertSame(query.projection(Projections.with()), query);
    Assert.assertSame(query.skip(10), query);
    Assert.assertSame(query.limit(5), query);
    Assert.assertSame(query.batchSize(50), query);
  }

  public void testNegativeSkipIsIgnoredByApplyButDoesNotThrow () {

    Query query = Query.with().skip(-5);

    Assert.assertNotNull(query);
  }
}
