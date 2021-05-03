/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.phalanx.wire.SignatureUtility;
import org.smallmind.phalanx.wire.TransportTimeoutException;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;

public class AsynchronousTransmissionCallback extends TransmissionCallback {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<Stint> timeoutDurationRef = new AtomicReference<>();
  private final AtomicReference<ResultSignal> resultSignalRef = new AtomicReference<>();
  private final String serviceName;
  private final String functionName;

  public AsynchronousTransmissionCallback (String serviceName, String functionName) {

    this.serviceName = serviceName;
    this.functionName = functionName;
  }

  @Override
  public void destroy (Stint timeoutStint) {

    timeoutDurationRef.set(timeoutStint);

    resultLatch.countDown();
  }

  @Override
  public Object getResult (SignalCodec signalCodec)
    throws Throwable {

    ResultSignal resultSignal;

    resultLatch.await();

    if ((resultSignal = resultSignalRef.get()) == null) {

      Stint timeoutStint = timeoutDurationRef.get();

      throw new TransportTimeoutException("The timeout(%s) milliseconds was exceeded while waiting for a response(%s.%s)", (timeoutStint == null) ? "unknown" : String.valueOf(timeoutStint.toMilliseconds()), serviceName, functionName);
    }

    handleError(signalCodec, resultSignal);

    return signalCodec.extractObject(resultSignal.getResult(), SignatureUtility.nativeDecode(resultSignal.getNativeType()));
  }

  public void setResultSignal (ResultSignal resultSignal) {

    resultSignalRef.set(resultSignal);
    resultLatch.countDown();
  }
}