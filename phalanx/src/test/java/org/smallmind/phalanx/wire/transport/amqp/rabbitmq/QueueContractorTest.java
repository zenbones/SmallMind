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

import java.lang.reflect.Proxy;
import java.util.Map;
import com.rabbitmq.client.Channel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the queue-declaration arguments each {@link QueueContractor} hands to the broker: a classic
 * queue is durable, non-exclusive, honours the caller's auto-delete flag, and carries no extra
 * arguments, while a quorum queue forces auto-delete off and supplies the quorum type and initial
 * group-size arguments. The channel is a capturing dynamic proxy, so no live broker is required.
 */
@Test(groups = "unit")
public class QueueContractorTest {

  private Channel capturingChannel (Object[] capturedArguments) {

    return (Channel)Proxy.newProxyInstance(QueueContractorTest.class.getClassLoader(), new Class[] {Channel.class}, (proxy, method, args) -> {

      if (method.getName().equals("queueDeclare") && (args != null) && (args.length == 5)) {
        System.arraycopy(args, 0, capturedArguments, 0, 5);
      }

      return null;
    });
  }

  @Test
  public void testClassicQueueType () {

    Assert.assertEquals(new ClassicQueueContractor().getQueueType(), QueueType.CLASSIC);
  }

  @Test
  public void testQuorumQueueType () {

    Assert.assertEquals(new QuorumQueueContractor(3).getQueueType(), QueueType.QUORUM);
  }

  @Test
  public void testClassicDeclareArguments ()
    throws Exception {

    Object[] captured = new Object[5];

    new ClassicQueueContractor().declare(capturingChannel(captured), "classic-queue", true);

    Assert.assertEquals(captured[0], "classic-queue");
    Assert.assertEquals(captured[1], true);
    Assert.assertEquals(captured[2], false);
    Assert.assertEquals(captured[3], true);
    Assert.assertNull(captured[4]);
  }

  @Test
  public void testQuorumDeclareArguments ()
    throws Exception {

    Object[] captured = new Object[5];

    new QuorumQueueContractor(3).declare(capturingChannel(captured), "quorum-queue", true);

    Assert.assertEquals(captured[0], "quorum-queue");
    Assert.assertEquals(captured[1], true);
    Assert.assertEquals(captured[2], false);
    Assert.assertEquals(captured[3], false);
    Assert.assertTrue(captured[4] instanceof Map);

    Map<?, ?> arguments = (Map<?, ?>)captured[4];

    Assert.assertEquals(arguments.get("x-queue-type"), "quorum");
    Assert.assertEquals(arguments.get("x-quorum-initial-group-size"), 3);
  }

  @Test
  public void testQueueTypeValues () {

    Assert.assertEquals(QueueType.values().length, 2);
    Assert.assertEquals(QueueType.valueOf("CLASSIC"), QueueType.CLASSIC);
    Assert.assertEquals(QueueType.valueOf("QUORUM"), QueueType.QUORUM);
  }
}
