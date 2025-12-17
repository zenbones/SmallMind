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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;
import org.smallmind.web.json.scaffold.fault.NativeLanguage;
import org.smallmind.web.json.scaffold.fault.NativeObject;

/**
 * Base callback for awaiting or processing transmission results.
 */
public abstract class TransmissionCallback {

  /**
   * Retrieves the result, potentially blocking until available or timeout.
   *
   * @param signalCodec    codec used to decode results
   * @param timeoutSeconds maximum time to wait
   * @return decoded result object
   * @throws Throwable if waiting fails or the remote side reports an error
   */
  public abstract Object getResult (SignalCodec signalCodec, long timeoutSeconds)
    throws Throwable;

  /**
   * Processes an error-bearing result signal and rethrows the underlying cause when possible.
   *
   * @param signalCodec  codec used to decode the fault
   * @param resultSignal signal containing the error payload
   * @throws Throwable the decoded throwable or a {@link FaultWrappingException}
   */
  public void handleError (SignalCodec signalCodec, ResultSignal resultSignal)
    throws Throwable {

    if (resultSignal.isError()) {

      Fault fault = signalCodec.extractObject(resultSignal.getResult(), Fault.class);
      NativeObject nativeObject;

      if (((nativeObject = fault.getNativeObject()) != null) && nativeObject.getLanguage().equals(NativeLanguage.JAVA)) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(nativeObject.getBytes()); ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
          throw (Throwable)objectInputStream.readObject();
        }
      }

      throw new FaultWrappingException(fault);
    }
  }
}
