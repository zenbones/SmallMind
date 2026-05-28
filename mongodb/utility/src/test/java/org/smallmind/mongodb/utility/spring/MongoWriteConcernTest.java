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
package org.smallmind.mongodb.utility.spring;

import com.mongodb.WriteConcern;
import org.smallmind.mongodb.utility.MongoAcknowledgment;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MongoWriteConcernTest {

  public void testIsSingletonReturnsFalse () {

    Assert.assertFalse(new MongoWriteConcern().isSingleton());
  }

  public void testGetObjectTypeReturnsWriteConcernClass () {

    Assert.assertEquals(new MongoWriteConcern().getObjectType(), WriteConcern.class);
  }

  public void testZeroWithJournaledFalseReturnsUnacknowledged () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.ZERO);
    bean.setJournaled(false);

    Assert.assertEquals(bean.getObject(), WriteConcern.UNACKNOWLEDGED);
  }

  public void testZeroWithJournaledTrueStillReturnsUnacknowledgedWithoutJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.ZERO);
    bean.setJournaled(true);

    Assert.assertEquals(bean.getObject(), WriteConcern.UNACKNOWLEDGED);
  }

  public void testOneWithJournaledFalseReturnsW1WithoutJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.ONE);
    bean.setJournaled(false);

    Assert.assertEquals(bean.getObject(), WriteConcern.W1.withJournal(false));
  }

  public void testOneWithJournaledTrueReturnsW1WithJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.ONE);
    bean.setJournaled(true);

    Assert.assertEquals(bean.getObject(), WriteConcern.W1.withJournal(true));
  }

  public void testMajorityWithJournaledFalseReturnsMajorityWithoutJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.MAJORITY);
    bean.setJournaled(false);

    Assert.assertEquals(bean.getObject(), WriteConcern.MAJORITY.withJournal(false));
  }

  public void testMajorityWithJournaledTrueReturnsMajorityWithJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.MAJORITY);
    bean.setJournaled(true);

    Assert.assertEquals(bean.getObject(), WriteConcern.MAJORITY.withJournal(true));
  }

  public void testTwoWithJournaledTrueReturnsW2WithJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.TWO);
    bean.setJournaled(true);

    Assert.assertEquals(bean.getObject(), WriteConcern.W2.withJournal(true));
  }

  public void testThreeWithJournaledTrueReturnsW3WithJournal () {

    MongoWriteConcern bean = new MongoWriteConcern();

    bean.setAcknowledgment(MongoAcknowledgment.THREE);
    bean.setJournaled(true);

    Assert.assertEquals(bean.getObject(), WriteConcern.W3.withJournal(true));
  }
}
