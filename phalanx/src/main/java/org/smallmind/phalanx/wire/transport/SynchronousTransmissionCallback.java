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

import org.smallmind.phalanx.wire.SignatureUtility;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;

/**
 * {@link TransmissionCallback} implementation for results that arrive before the caller registers
 * its callback; holds the already-completed {@link ResultSignal} and returns it immediately without
 * blocking.
 */
public class SynchronousTransmissionCallback extends TransmissionCallback {

  private final ResultSignal resultSignal;

  /**
   * Constructs a callback that wraps an already-available result signal.
   *
   * @param resultSignal the completed result signal to return on the next {@link #getResult} call
   */
  public SynchronousTransmissionCallback (ResultSignal resultSignal) {

    this.resultSignal = resultSignal;
  }

  /**
   * Returns the result immediately from the pre-stored signal without waiting.
   *
   * @param signalCodec   codec used to decode the result payload
   * @param timoueSeconds ignored; no waiting occurs
   * @return the decoded return value of the remote invocation
   * @throws Throwable if the result signal carries an error or decoding fails
   */
  @Override
  public synchronized Object getResult (SignalCodec signalCodec, long timoueSeconds)
    throws Throwable {

    handleError(signalCodec, resultSignal);

    return signalCodec.extractObject(resultSignal.getResult(), SignatureUtility.nativeDecode(resultSignal.getNativeType()));
  }
}
