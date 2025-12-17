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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.phalanx.wire.SignatureUtility;
import org.smallmind.phalanx.wire.TransportTimeoutException;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;

/**
 * Callback that waits asynchronously for a response signal, enforcing a timeout.
 */
public class AsynchronousTransmissionCallback extends TransmissionCallback {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<ResultSignal> resultSignalRef = new AtomicReference<>();
  private final String serviceName;
  private final String functionName;

  /**
   * Constructs a callback for the given service/function, used in timeout messaging.
   *
   * @param serviceName  service name associated with the call
   * @param functionName function name associated with the call
   */
  public AsynchronousTransmissionCallback (String serviceName, String functionName) {

    this.serviceName = serviceName;
    this.functionName = functionName;
  }

  /**
   * Waits for the result or throws when the timeout expires.
   */
  public Object getResult (SignalCodec signalCodec, long timeoutSeconds)
    throws Throwable {

    ResultSignal resultSignal;

    if (!resultLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
      throw new TransportTimeoutException("The timeout(%d) seconds was exceeded while waiting for a response(%s.%s)", timeoutSeconds, serviceName, functionName);
    } else {

      if ((resultSignal = resultSignalRef.get()) == null) {
        throw new IllegalStateException("Missing signal result");
      } else {

        handleError(signalCodec, resultSignal);

        return signalCodec.extractObject(resultSignal.getResult(), SignatureUtility.nativeDecode(resultSignal.getNativeType()));
      }
    }
  }

  /**
   * Supplies the received result signal and releases any waiting callers.
   *
   * @param resultSignal result to provide
   */
  public void setResultSignal (ResultSignal resultSignal) {

    resultSignalRef.set(resultSignal);
    resultLatch.countDown();
  }
}
