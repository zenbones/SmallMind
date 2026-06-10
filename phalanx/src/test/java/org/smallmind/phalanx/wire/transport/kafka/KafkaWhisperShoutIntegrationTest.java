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

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.kafka.utility.KafkaServer;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.phalanx.wire.Argument;
import org.smallmind.phalanx.wire.Shout;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.Whisper;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Drives the directed ({@link Whisper}) and broadcast ({@link Shout}) voices over a real Kafka
 * broker — the per-instance whisper topic and the group shout topic that the talk-only
 * {@link KafkaTransportIntegrationTest} never exercises. The transports pre-create the topics they
 * consume and block in their constructors until the consumers are assigned and positioned, so the
 * first real call is guaranteed delivery despite {@code auto.offset.reset=latest}.
 */
@Test(groups = "integration")
public class KafkaWhisperShoutIntegrationTest extends AbstractGroundwaterTest {

  private final JsonSignalCodec signalCodec = new JsonSignalCodec();
  private ResponseTransport responseTransport;
  private KafkaRequestTransport requestTransport;
  private WhisperShoutServiceImpl serviceImpl;
  private WhisperShoutService serviceProxy;

  public interface WhisperShoutService {

    @Whisper(timeoutSeconds = 5)
    String ping (@Argument("value") String value);

    @Shout
    void broadcast (@Argument("value") String value);
  }

  public static class WhisperShoutServiceImpl implements WhisperShoutService, WiredService {

    private final AtomicReference<String> lastShout = new AtomicReference<>();

    @Override
    public int getVersion () {

      return 1;
    }

    @Override
    public String getServiceName () {

      return "WhisperShoutService";
    }

    @Override
    public void setResponseTransport (ResponseTransport responseTransport) {

    }

    @Override
    public String ping (String value) {

      return "pong:" + value;
    }

    @Override
    public void broadcast (String value) {

      lastShout.set(value);
    }
  }

  public KafkaWhisperShoutIntegrationTest () {

    super(DockerApplication.KAFKA);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();
    super.beforeClass();

    //  Required on this thread for the transports' Claxon instrumentation (see the contract base).
    new PerApplicationContext();

    responseTransport = new KafkaResponseTransport("kafka-ws-worker", "default", InvocationWorker.class, signalCodec, 1, 30, KafkaGroupProtocol.CONSUMER, new KafkaServer("localhost", 9094));
    serviceImpl = new WhisperShoutServiceImpl();

    String instanceId = responseTransport.register(WhisperShoutService.class, serviceImpl);

    requestTransport = new KafkaRequestTransport("kafka-ws-client", signalCodec, 1, 30L, 30, KafkaGroupProtocol.CONSUMER, new KafkaServer("localhost", 9094));
    serviceProxy = (WhisperShoutService)WireProxyFactory.generateProxy(requestTransport, 1, "WhisperShoutService", WhisperShoutService.class, new StaticParameterExtractor<>("default"), new StaticParameterExtractor<>(instanceId), null);

    //  No warm-up is needed: both transport constructors pre-create the topics they consume and block
    //  until their consumers are assigned and positioned, so by the time construction returns the first
    //  real call is guaranteed delivery despite auto.offset.reset=latest.
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    if (requestTransport != null) {
      requestTransport.close();
    }
    if (responseTransport != null) {
      responseTransport.close();
    }

    super.afterClass();
  }

  @Test
  public void testWhisperReachesTargetInstance () {

    Assert.assertEquals(serviceProxy.ping("hi"), "pong:hi");
  }

  /**
   * Exercises the broadcast ({@link Shout}) voice. Now that {@link KafkaResponseTransport} gives the
   * shout ingester its own per-instance consumer group ({@code wire-shout-<instanceId>}), distinct
   * from the whisper group, the shout consumer receives a clean assignment of the shared shout topic
   * (no mixed-subscription hazard) and the broadcast reaches the service. Because the consumer starts
   * at {@code auto.offset.reset=latest}, the broadcast is retried until the consumer is positioned.
   */
  @Test
  public void testShoutIsBroadcast ()
    throws InterruptedException {

    for (int attempt = 0; (attempt < 15) && (!"hey".equals(serviceImpl.lastShout.get())); attempt++) {
      serviceProxy.broadcast("hey");
      Thread.sleep(2000);
    }

    Assert.assertEquals(serviceImpl.lastShout.get(), "hey");
  }
}
