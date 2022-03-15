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

import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;

public abstract class AbstractRequestTransport implements RequestTransport {

  private final ConcurrentHashMap<String, TransmissionCallback> callbackMap;
  private final long defaultTimeoutSeconds;

  public AbstractRequestTransport (long defaultTimeoutSeconds) {

    this.defaultTimeoutSeconds = defaultTimeoutSeconds;

    callbackMap = new ConcurrentHashMap<>();
  }

  public Object acquireResult (SignalCodec signalCodec, Route route, Voice<?, ?> voice, String messageId, boolean inOnly)
    throws Throwable {

    if (!inOnly) {

      AsynchronousTransmissionCallback asynchronousCallback = new AsynchronousTransmissionCallback(route.getService(), route.getFunction().getName());
      SynchronousTransmissionCallback previousCallback;

      if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(messageId, asynchronousCallback)) != null) {

        return previousCallback.getResult(signalCodec, 0);
      }

      try {

        Object timeoutObject;
        long timeoutSeconds = (((timeoutObject = voice.getConversation().getTimeout()) == null) || ((Long)timeoutObject <= 0)) ? defaultTimeoutSeconds : (Long)timeoutObject;

        return asynchronousCallback.getResult(signalCodec, timeoutSeconds);
      } finally {
        callbackMap.remove(messageId);
      }
    }

    return null;
  }

  public void completeCallback (String correlationId, ResultSignal resultSignal) {

    TransmissionCallback previousCallback;

    if ((previousCallback = callbackMap.get(correlationId)) == null) {
      if ((previousCallback = callbackMap.putIfAbsent(correlationId, new SynchronousTransmissionCallback(resultSignal))) != null) {
        if (previousCallback instanceof AsynchronousTransmissionCallback) {
          ((AsynchronousTransmissionCallback)previousCallback).setResultSignal(resultSignal);
        }
      }
    } else if (previousCallback instanceof AsynchronousTransmissionCallback) {
      ((AsynchronousTransmissionCallback)previousCallback).setResultSignal(resultSignal);
    }
  }
}
