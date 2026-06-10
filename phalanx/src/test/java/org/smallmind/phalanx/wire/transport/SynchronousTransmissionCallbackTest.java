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

import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultElement;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the already-completed callback path used when a result signal arrives before the caller
 * registers: {@link SynchronousTransmissionCallback#getResult} returns the decoded value immediately
 * for a normal result and rethrows for an error result, ignoring the timeout argument entirely.
 */
@Test(groups = "unit")
public class SynchronousTransmissionCallbackTest {

  private final SignalCodec signalCodec = new JsonSignalCodec();

  private ResultSignal roundTrip (ResultSignal resultSignal)
    throws Exception {

    byte[] bytes = signalCodec.encode(resultSignal);

    return signalCodec.decode(bytes, 0, bytes.length, ResultSignal.class);
  }

  @Test
  public void testNormalResultIsReturnedImmediately ()
    throws Throwable {

    ResultSignal resultSignal = roundTrip(new ResultSignal(false, "Ljava/lang/String;", "hello"));
    SynchronousTransmissionCallback callback = new SynchronousTransmissionCallback(resultSignal);

    Assert.assertEquals(callback.getResult(signalCodec, 0), "hello");
  }

  @Test
  public void testErrorResultRethrows ()
    throws Exception {

    Fault fault = new Fault(new FaultElement("SynchronousService", "boom"), new RuntimeException("boom"));
    ResultSignal resultSignal = roundTrip(new ResultSignal(true, "Ljava/lang/String;", fault));
    SynchronousTransmissionCallback callback = new SynchronousTransmissionCallback(resultSignal);

    Throwable thrown = null;

    try {
      callback.getResult(signalCodec, 0);
    } catch (Throwable throwable) {
      thrown = throwable;
    }

    Assert.assertNotNull(thrown, "An error result signal must cause getResult to throw");
  }

  @Test(expectedExceptions = FaultWrappingException.class)
  public void testNonNativeFaultBecomesFaultWrappingException ()
    throws Throwable {

    //  A fault that carries no native (Java-serialized) object — e.g. an error raised by a non-Java
    //  service or a non-serializable throwable — cannot be reconstituted, so handleError must surface
    //  it as a FaultWrappingException rather than NPE on the absent native object or drop it silently.
    ResultSignal resultSignal = roundTrip(new ResultSignal(true, "Ljava/lang/String;", new Fault(new FaultElement("RemoteService", "plain failure"), "plain failure")));

    new SynchronousTransmissionCallback(resultSignal).getResult(signalCodec, 0);
  }
}
