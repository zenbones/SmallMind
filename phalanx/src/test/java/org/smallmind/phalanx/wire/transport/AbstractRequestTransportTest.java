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
package org.smallmind.phalanx.wire.transport;

import java.util.Map;
import org.smallmind.phalanx.wire.TransportTimeoutException;
import org.smallmind.phalanx.wire.TwoWayConversation;
import org.smallmind.phalanx.wire.Talking;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives the callback-correlation logic of {@link AbstractRequestTransport} without a broker: in-only
 * calls skip waiting, a result that arrives before registration is read immediately, an
 * asynchronously-delivered result unblocks the waiter, and the per-call timeout is resolved from the
 * conversation (explicit positive value) or the transport default (null or non-positive value).
 */
@Test(groups = "unit")
public class AbstractRequestTransportTest {

  private final SignalCodec signalCodec = new JsonSignalCodec();

  private ResultSignal stringResult (String value)
    throws Exception {

    byte[] bytes = signalCodec.encode(new ResultSignal(false, "Ljava/lang/String;", value));

    return signalCodec.decode(bytes, 0, bytes.length, ResultSignal.class);
  }

  private Route route () {

    return new Route(1, "TestService", new Function("echoString"));
  }

  private Voice<?, ?> voice (Long timeout) {

    return new Talking(new TwoWayConversation(timeout), "default");
  }

  @Test
  public void testInOnlyReturnsNull ()
    throws Throwable {

    TestRequestTransport transport = new TestRequestTransport(30L);

    Assert.assertNull(transport.acquireResult(signalCodec, route(), voice(5L), "in-only-message", true));
  }

  @Test
  public void testResultArrivingBeforeRegistrationIsReturned ()
    throws Throwable {

    TestRequestTransport transport = new TestRequestTransport(30L);

    transport.completeCallback("early-message", stringResult("early"));

    Assert.assertEquals(transport.acquireResult(signalCodec, route(), voice(5L), "early-message", false), "early");
  }

  @Test
  public void testAsynchronouslyDeliveredResultUnblocksWaiter ()
    throws Throwable {

    TestRequestTransport transport = new TestRequestTransport(30L);
    ResultSignal resultSignal = stringResult("late");

    Thread deliverer = new Thread(() -> {

      try {
        Thread.sleep(300);
        transport.completeCallback("late-message", resultSignal);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }
    });
    deliverer.setName("test-deliverer");
    deliverer.setDaemon(true);
    deliverer.start();

    Assert.assertEquals(transport.acquireResult(signalCodec, route(), voice(10L), "late-message", false), "late");
  }

  @Test
  public void testExplicitTimeoutIsHonoredOverDefault () {

    TestRequestTransport transport = new TestRequestTransport(30L);
    long start = System.nanoTime();

    try {
      transport.acquireResult(signalCodec, route(), voice(1L), "explicit-timeout-message", false);
      Assert.fail("Expected a TransportTimeoutException");
    } catch (TransportTimeoutException transportTimeoutException) {

      long elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000L;

      Assert.assertTrue(elapsedSeconds < 15, "The explicit 1s timeout should fire well before the 30s default; waited " + elapsedSeconds + "s");
    } catch (Throwable throwable) {
      Assert.fail("Expected a TransportTimeoutException, got " + throwable.getClass().getName());
    }
  }

  @Test
  public void testNullTimeoutFallsBackToDefault () {

    TestRequestTransport transport = new TestRequestTransport(1L);

    try {
      transport.acquireResult(signalCodec, route(), voice(null), "null-timeout-message", false);
      Assert.fail("Expected a TransportTimeoutException");
    } catch (TransportTimeoutException transportTimeoutException) {
      // expected: a null conversation timeout defers to the 1s transport default
    } catch (Throwable throwable) {
      Assert.fail("Expected a TransportTimeoutException, got " + throwable.getClass().getName());
    }
  }

  @Test
  public void testNonPositiveTimeoutFallsBackToDefault () {

    TestRequestTransport transport = new TestRequestTransport(1L);

    try {
      transport.acquireResult(signalCodec, route(), voice(0L), "zero-timeout-message", false);
      Assert.fail("Expected a TransportTimeoutException");
    } catch (TransportTimeoutException transportTimeoutException) {
      // expected: a zero conversation timeout defers to the 1s transport default
    } catch (Throwable throwable) {
      Assert.fail("Expected a TransportTimeoutException, got " + throwable.getClass().getName());
    }
  }

  @Test
  public void testDuplicateDeliveryBeforeRegistrationIsIdempotent ()
    throws Throwable {

    TestRequestTransport transport = new TestRequestTransport(30L);

    //  At-least-once redelivery: the response arrives twice before the caller registers. The first
    //  stores a SynchronousTransmissionCallback; the second must be a safe no-op (it is not an
    //  AsynchronousTransmissionCallback), and the caller still reads the result exactly once rather
    //  than double-firing or hanging.
    transport.completeCallback("duplicate-message", stringResult("once"));
    transport.completeCallback("duplicate-message", stringResult("once"));

    Assert.assertEquals(transport.acquireResult(signalCodec, route(), voice(5L), "duplicate-message", false), "once");
  }

  private static class TestRequestTransport extends AbstractRequestTransport {

    public TestRequestTransport (long defaultTimeoutSeconds) {

      super(defaultTimeoutSeconds);
    }

    @Override
    public String getCallerId () {

      return "test-caller";
    }

    @Override
    public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts) {

      return null;
    }

    @Override
    public void close () {

    }
  }
}
