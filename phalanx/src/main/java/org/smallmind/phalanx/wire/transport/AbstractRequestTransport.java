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

import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;

/**
 * Abstract base implementation of {@link RequestTransport} that manages in-flight callback correlation
 * for both asynchronous and already-completed (synchronous) result signals, applying a configurable
 * default timeout when no per-call timeout is present in the conversation.
 */
public abstract class AbstractRequestTransport implements RequestTransport {

  private final ConcurrentHashMap<String, TransmissionCallback> callbackMap;
  private final long defaultTimeoutSeconds;

  /**
   * Constructs the transport with the given fallback timeout.
   *
   * @param defaultTimeoutSeconds timeout in seconds used when the conversation carries no explicit timeout
   */
  public AbstractRequestTransport (long defaultTimeoutSeconds) {

    this.defaultTimeoutSeconds = defaultTimeoutSeconds;

    callbackMap = new ConcurrentHashMap<>();
  }

  /**
   * Acquires the result for an outbound request, blocking asynchronously until the response arrives
   * or the timeout expires; returns immediately for in-only (fire-and-forget) calls.
   *
   * @param signalCodec codec used to decode the result payload
   * @param route       route identifying the target service and function, used in timeout messages
   * @param voice       voice that carries the conversation style and optional per-call timeout
   * @param messageId   correlation id that links this call to its response
   * @param inOnly      {@code true} if no response is expected; skips waiting and returns {@code null}
   * @return the decoded return value of the remote invocation, or {@code null} for in-only calls
   * @throws Throwable if the wait times out, the callback is interrupted, or the remote side reports an error
   */
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

  /**
   * Delivers an inbound result signal to the callback registered for the given correlation id.
   * If the callback has not yet been registered (result arrived before the caller had a chance to
   * register), a {@link SynchronousTransmissionCallback} is stored so the caller can read it immediately
   * on registration.
   *
   * @param correlationId correlation id that identifies the original request
   * @param resultSignal  result signal received from the remote service
   */
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
