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
package org.smallmind.phalanx.wire.transport.jms;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import jakarta.jms.MessageProducer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies that {@link MessagePolicy#apply} pushes its configured settings onto a JMS
 * {@link MessageProducer}, including the seconds-to-milliseconds conversion of the time-to-live, and
 * that an unconfigured policy applies the documented defaults (non-persistent delivery, unlimited
 * TTL, priority 4, no ID/timestamp suppression). The producer is a capturing dynamic proxy so the
 * test never needs a live JMS provider.
 */
@Test(groups = "unit")
public class MessagePolicyTest {

  private MessageProducer capturingProducer (Map<String, Object> captured) {

    return (MessageProducer)Proxy.newProxyInstance(MessagePolicyTest.class.getClassLoader(), new Class[] {MessageProducer.class}, (proxy, method, args) -> {

      if ((args != null) && (args.length == 1)) {
        captured.put(method.getName(), args[0]);
      }

      return null;
    });
  }

  @Test
  public void testDefaultsAreApplied ()
    throws Exception {

    Map<String, Object> captured = new HashMap<>();

    new MessagePolicy().apply(capturingProducer(captured));

    Assert.assertEquals(captured.get("setDeliveryMode"), DeliveryMode.NON_PERSISTENT.getJmsValue());
    Assert.assertEquals(captured.get("setTimeToLive"), 0L);
    Assert.assertEquals(captured.get("setPriority"), 4);
    Assert.assertEquals(captured.get("setDisableMessageID"), false);
    Assert.assertEquals(captured.get("setDisableMessageTimestamp"), false);
  }

  @Test
  public void testConfiguredSettingsAreApplied ()
    throws Exception {

    Map<String, Object> captured = new HashMap<>();
    MessagePolicy policy = new MessagePolicy();

    policy.setDeliveryMode(DeliveryMode.PERSISTENT);
    policy.setTimeToLiveSeconds(5);
    policy.setPriority(7);
    policy.setDisableMessageID(true);
    policy.setDisableMessageTimestamp(true);

    policy.apply(capturingProducer(captured));

    Assert.assertEquals(captured.get("setDeliveryMode"), DeliveryMode.PERSISTENT.getJmsValue());
    Assert.assertEquals(captured.get("setTimeToLive"), 5000L);
    Assert.assertEquals(captured.get("setPriority"), 7);
    Assert.assertEquals(captured.get("setDisableMessageID"), true);
    Assert.assertEquals(captured.get("setDisableMessageTimestamp"), true);
  }
}
