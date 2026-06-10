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

import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.kafka.utility.KafkaServer;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.transport.AbstractWireTransportContractTest;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.testbench.docker.DockerApplication;
import org.testng.annotations.Test;

/**
 * Runs the wire transport contract against a Kafka broker started by the Docker test harness
 * ({@link DockerApplication#KAFKA}, KRaft broker advertising {@code localhost:9094}). The
 * transports gate broker readiness via the kafka-utility {@code check(startupGracePeriodSeconds)}.
 */
@Test(groups = "integration")
public class KafkaTransportIntegrationTest extends AbstractWireTransportContractTest {

  private final JsonSignalCodec signalCodec = new JsonSignalCodec();

  public KafkaTransportIntegrationTest () {

    super(DockerApplication.KAFKA);
  }

  @Override
  protected ResponseTransport createResponseTransport ()
    throws Exception {

    return new KafkaResponseTransport("kafka-worker", "default", InvocationWorker.class, signalCodec, 1, 30, KafkaGroupProtocol.CONSUMER, new KafkaServer("localhost", 9094));
  }

  @Override
  protected RequestTransport createRequestTransport ()
    throws Exception {

    return new KafkaRequestTransport("kafka-client", signalCodec, 1, 30L, 30, KafkaGroupProtocol.CONSUMER, new KafkaServer("localhost", 9094));
  }

  //  No warmUp() override is needed: the Kafka transports pre-create the topics they consume and their
  //  constructors block until the consumers are assigned and positioned, so the contract's test methods
  //  already run against ready consumers despite auto.offset.reset=latest.

  /**
   * {@link KafkaResponseTransport} drives a single-threaded {@code KafkaConsumer} from its ingester
   * poll loop, so {@code pause()}/{@code play()} arrive on a foreign thread. The ingester now routes
   * every consumer touch (poll, subscribe, unsubscribe) through a shared {@code ReentrantLock}, so a
   * control call blocks until the worker's {@code poll()} returns and the consumer is released —
   * Kafka permits cross-thread access under external synchronization, so the pause/resume contract
   * now holds for Kafka exactly as for RabbitMQ and JMS. Delegates to the shared contract assertion.
   */
  @Test
  @Override
  public void testPauseAndResumeLifecycle ()
    throws Exception {

    super.testPauseAndResumeLifecycle();
  }
}
