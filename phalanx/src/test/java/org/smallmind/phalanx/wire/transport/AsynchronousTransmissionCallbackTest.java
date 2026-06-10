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

import org.smallmind.phalanx.wire.TransportTimeoutException;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultElement;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the blocking callback path: {@link AsynchronousTransmissionCallback#getResult} returns the
 * decoded value once {@link AsynchronousTransmissionCallback#setResultSignal} releases the latch,
 * rethrows for an error signal, and throws {@link TransportTimeoutException} when no signal arrives
 * within the timeout.
 */
@Test(groups = "unit")
public class AsynchronousTransmissionCallbackTest {

  private final SignalCodec signalCodec = new JsonSignalCodec();

  private ResultSignal roundTrip (ResultSignal resultSignal)
    throws Exception {

    byte[] bytes = signalCodec.encode(resultSignal);

    return signalCodec.decode(bytes, 0, bytes.length, ResultSignal.class);
  }

  @Test
  public void testDeliveredResultIsReturned ()
    throws Throwable {

    AsynchronousTransmissionCallback callback = new AsynchronousTransmissionCallback("AsyncService", "echo");

    callback.setResultSignal(roundTrip(new ResultSignal(false, "Ljava/lang/String;", "hello")));

    Assert.assertEquals(callback.getResult(signalCodec, 5), "hello");
  }

  @Test
  public void testDeliveredErrorRethrows ()
    throws Exception {

    AsynchronousTransmissionCallback callback = new AsynchronousTransmissionCallback("AsyncService", "boom");

    callback.setResultSignal(roundTrip(new ResultSignal(true, "Ljava/lang/String;", new Fault(new FaultElement("AsyncService", "boom"), new RuntimeException("boom")))));

    Throwable thrown = null;

    try {
      callback.getResult(signalCodec, 5);
    } catch (Throwable throwable) {
      thrown = throwable;
    }

    Assert.assertNotNull(thrown, "A delivered error signal must cause getResult to throw");
  }

  @Test
  public void testTimeoutWhenNoResultArrives () {

    AsynchronousTransmissionCallback callback = new AsynchronousTransmissionCallback("AsyncService", "neverAnswers");

    try {
      callback.getResult(signalCodec, 1);
      Assert.fail("Expected a TransportTimeoutException when no result is delivered");
    } catch (TransportTimeoutException transportTimeoutException) {
      // expected
    } catch (Throwable throwable) {
      Assert.fail("Expected a TransportTimeoutException, got " + throwable.getClass().getName());
    }
  }
}
