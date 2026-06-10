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
package org.smallmind.phalanx.wire.transport.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.phalanx.wire.Argument;
import org.smallmind.phalanx.wire.Shout;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.Whisper;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Component test for the directed ({@link Whisper}) and broadcast ({@link Shout}) voices over the
 * in-process mock transport — paths the existing end-to-end {@code MockWireTest} (talk/in-out only)
 * never exercises. Wires {@link MockRequestTransport} and {@link MockResponseTransport} through a
 * shared {@link MockMessageRouter} with no Spring and no broker.
 */
@Test(groups = "unit")
public class MockWhisperShoutTest {

  private MockRequestTransport requestTransport;
  private MockResponseTransport responseTransport;
  private WhisperShoutServiceImpl serviceImpl;
  private WhisperShoutService serviceProxy;

  public interface WhisperShoutService {

    @Whisper(timeoutSeconds = 5)
    String ping (@Argument("value") String value);

    @Shout
    void broadcast (@Argument("value") String value);
  }

  public static class WhisperShoutServiceImpl implements WhisperShoutService, WiredService {

    private final CountDownLatch shoutLatch = new CountDownLatch(1);
    private volatile String lastShout;

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

      lastShout = value;
      shoutLatch.countDown();
    }
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    MockMessageRouter messageRouter = new MockMessageRouter();
    JsonSignalCodec signalCodec = new JsonSignalCodec();

    responseTransport = new MockResponseTransport(messageRouter, signalCodec);
    serviceImpl = new WhisperShoutServiceImpl();

    String instanceId = responseTransport.register(WhisperShoutService.class, serviceImpl);

    requestTransport = new MockRequestTransport(messageRouter, signalCodec, 5);
    serviceProxy = (WhisperShoutService)WireProxyFactory.generateProxy(requestTransport, 1, "WhisperShoutService", WhisperShoutService.class, new StaticParameterExtractor<>("group"), new StaticParameterExtractor<>(instanceId), null);
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    requestTransport.close();
    responseTransport.close();
  }

  @Test
  public void testWhisperReachesTargetInstanceAndReturns () {

    Assert.assertEquals(serviceProxy.ping("hi"), "pong:hi");
  }

  @Test
  public void testShoutIsDeliveredFireAndForget ()
    throws InterruptedException {

    serviceProxy.broadcast("hey");

    Assert.assertTrue(serviceImpl.shoutLatch.await(5, TimeUnit.SECONDS), "shout was not delivered in time");
    Assert.assertEquals(serviceImpl.lastShout, "hey");
  }
}
