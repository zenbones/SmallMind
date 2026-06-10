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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Locks the documented default exchange and queue name suffixes carried by {@link NameConfiguration}
 * (a changed default silently alters broker topology) and verifies that each setter round-trips.
 */
@Test(groups = "unit")
public class NameConfigurationTest {

  @Test
  public void testDefaults () {

    NameConfiguration nameConfiguration = new NameConfiguration();

    Assert.assertEquals(nameConfiguration.getRequestExchange(), "requestExchange");
    Assert.assertEquals(nameConfiguration.getResponseExchange(), "responseExchange");
    Assert.assertEquals(nameConfiguration.getResponseQueue(), "responseQueue");
    Assert.assertEquals(nameConfiguration.getShoutQueue(), "shoutQueue");
    Assert.assertEquals(nameConfiguration.getTalkQueue(), "talkQueue");
    Assert.assertEquals(nameConfiguration.getWhisperQueue(), "whisperQueue");
  }

  @Test
  public void testSettersRoundTrip () {

    NameConfiguration nameConfiguration = new NameConfiguration();

    nameConfiguration.setRequestExchange("rx");
    nameConfiguration.setResponseExchange("sx");
    nameConfiguration.setResponseQueue("rq");
    nameConfiguration.setShoutQueue("sq");
    nameConfiguration.setTalkQueue("tq");
    nameConfiguration.setWhisperQueue("wq");

    Assert.assertEquals(nameConfiguration.getRequestExchange(), "rx");
    Assert.assertEquals(nameConfiguration.getResponseExchange(), "sx");
    Assert.assertEquals(nameConfiguration.getResponseQueue(), "rq");
    Assert.assertEquals(nameConfiguration.getShoutQueue(), "sq");
    Assert.assertEquals(nameConfiguration.getTalkQueue(), "tq");
    Assert.assertEquals(nameConfiguration.getWhisperQueue(), "wq");
  }
}
