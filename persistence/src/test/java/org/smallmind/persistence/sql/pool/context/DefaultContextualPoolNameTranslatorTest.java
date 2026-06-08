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
package org.smallmind.persistence.sql.pool.context;

import org.smallmind.quorum.pool.ComponentPoolException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DefaultContextualPoolNameTranslatorTest {

  public void testGetPoolNameConcatenatesBaseSeparatorAndContext () {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", '_');

    Assert.assertEquals(translator.getBaseName(), "pool");
    Assert.assertEquals(translator.getPoolName("ctxA"), "pool_ctxA");
  }

  public void testParseRoundTripsWithFormat ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", '_');

    Assert.assertEquals(translator.getContextualPartFromPoolName(translator.getPoolName("ctxA")), "ctxA");
  }

  public void testParseReturnsNullWhenNameEqualsBase ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", '_');

    Assert.assertNull(translator.getContextualPartFromPoolName("pool"));
  }

  public void testNullContextYieldsBareBaseNameAndRoundTrips ()
    throws ComponentPoolException {

    DefaultContextualPoolNameTranslator translator = new DefaultContextualPoolNameTranslator("pool", '_');

    Assert.assertEquals(translator.getPoolName(null), "pool");
    Assert.assertNull(translator.getContextualPartFromPoolName(translator.getPoolName(null)));
  }

  @Test(groups = "unit", expectedExceptions = ComponentPoolException.class)
  public void testParseRejectsNameNotStartingWithBase ()
    throws ComponentPoolException {

    new DefaultContextualPoolNameTranslator("pool", '_').getContextualPartFromPoolName("other_ctxA");
  }

  @Test(groups = "unit", expectedExceptions = ComponentPoolException.class)
  public void testParseRejectsNameMissingSeparatorAfterBase ()
    throws ComponentPoolException {

    new DefaultContextualPoolNameTranslator("pool", '_').getContextualPartFromPoolName("poolXctxA");
  }
}
