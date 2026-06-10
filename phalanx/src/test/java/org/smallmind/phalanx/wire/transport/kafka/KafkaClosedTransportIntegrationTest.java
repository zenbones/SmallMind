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
package org.smallmind.phalanx.wire.transport.kafka;

import java.util.HashMap;
import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.kafka.utility.KafkaServer;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.phalanx.wire.Talking;
import org.smallmind.phalanx.wire.TwoWayConversation;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies the closed-transport contract of the Kafka transports against a real broker: once a
 * transport is closed, its producer cache is emptied and any further send raises
 * {@link AlreadyClosedException} rather than silently dropping the message or reopening a producer.
 * Each test builds and closes its own throwaway transport so nothing leaks between methods.
 */
@Test(groups = "integration")
public class KafkaClosedTransportIntegrationTest extends AbstractGroundwaterTest {

  private final JsonSignalCodec signalCodec = new JsonSignalCodec();

  public KafkaClosedTransportIntegrationTest () {

    super(DockerApplication.KAFKA);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();
    super.beforeClass();

    new PerApplicationContext();
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  @Test(expectedExceptions = AlreadyClosedException.class)
  public void testRequestTransmitAfterCloseThrows ()
    throws Throwable {

    KafkaRequestTransport requestTransport = new KafkaRequestTransport("kafka-closed-client", signalCodec, 1, 30L, 30, KafkaGroupProtocol.CONSUMER, new KafkaServer("localhost", 9094));

    requestTransport.close();

    requestTransport.transmit(new Talking(new TwoWayConversation(1L), "default"), new Route(1, "ClosedService", new Function("ping")), new HashMap<>());
  }

  @Test(expectedExceptions = AlreadyClosedException.class)
  public void testResponseTransmitAfterCloseThrows ()
    throws Throwable {

    KafkaResponseTransport responseTransport = new KafkaResponseTransport("kafka-closed-worker", "default", InvocationWorker.class, signalCodec, 1, 30, KafkaGroupProtocol.CONSUMER, new KafkaServer("localhost", 9094));

    responseTransport.close();

    responseTransport.transmit("kafka-closed-client", "correlation-id", false, "V", null);
  }
}
