/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.phalanx.wire;

import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;

public abstract class AbstractRequestTransport implements RequestTransport {

  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;

  public AbstractRequestTransport (int defaultTimeoutSeconds) {

    callbackMap = new SelfDestructiveMap<>(new Duration(defaultTimeoutSeconds, TimeUnit.SECONDS));
  }

  public SelfDestructiveMap<String, TransmissionCallback> getCallbackMap () {

    return callbackMap;
  }

  public Object acquireResult (SignalCodec signalCodec, Address address, Voice voice, String messageId, boolean inOnly)
    throws Throwable {

    if (!inOnly) {

      AsynchronousTransmissionCallback asynchronousCallback = new AsynchronousTransmissionCallback(address.getService(), address.getFunction().getName());
      SynchronousTransmissionCallback previousCallback;
      Object timeoutObject;
      int timeoutSeconds = (timeoutObject = voice.getConversation().getTimeout()) == null ? 0 : (Integer)timeoutObject;

      if ((previousCallback = (SynchronousTransmissionCallback)getCallbackMap().putIfAbsent(messageId, asynchronousCallback, (timeoutSeconds > 0) ? new Duration(timeoutSeconds, TimeUnit.SECONDS) : null)) != null) {

        return previousCallback.getResult(signalCodec);
      }

      return asynchronousCallback.getResult(signalCodec);
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
