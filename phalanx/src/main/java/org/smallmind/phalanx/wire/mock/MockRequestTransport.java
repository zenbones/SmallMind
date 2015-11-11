/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.phalanx.wire.mock;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.AsynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.SynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.TransmissionCallback;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class MockRequestTransport implements RequestTransport {

  private final MockMessageRouter messageRouter;
  private final SignalCodec signalCodec;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final String callerId = UUID.randomUUID().toString();

  public MockRequestTransport (MockMessageRouter messageRouter, final SignalCodec signalCodec, int timeoutSeconds) {

    this.messageRouter = messageRouter;
    this.signalCodec = signalCodec;

    callbackMap = new SelfDestructiveMap<>(new Duration(timeoutSeconds, TimeUnit.SECONDS));

    messageRouter.getResponseTopic().addListener(new MockMessageListener() {

      @Override
      public boolean match (MockMessageProperties properties) {

        return properties.getHeader(WireProperty.CALLER_ID.getKey()).equals(callerId);
      }

      @Override
      public void handle (MockMessage message) {

        try {
          completeCallback(new String(message.getProperties().getCorrelationId()), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, ResultSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockRequestTransport.class).error(exception);
        }
      }
    });
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  @Override
  public void transmitInOnly (String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    transmit(true, serviceGroup, instanceId, address, arguments, contexts);
  }

  @Override
  public Object transmitInOut (String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    return transmit(false, serviceGroup, instanceId, address, arguments, contexts).getResult(signalCodec);
  }

  private TransmissionCallback transmit (boolean inOnly, String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    MockMessage message = new MockMessage(signalCodec.encode(new InvocationSignal(inOnly, address, arguments, contexts)));
    AsynchronousTransmissionCallback asynchronousCallback;
    SynchronousTransmissionCallback previousCallback;
    String messageId = UUID.randomUUID().toString();

    if (!inOnly) {
      message.getProperties().setHeader(WireProperty.CALLER_ID.getKey(), callerId);
    }

    message.getProperties().setMessageId(messageId);
    message.getProperties().setTimestamp(new Date());
    message.getProperties().setContentType(signalCodec.getContentType());
    message.getProperties().setHeader(WireProperty.CLOCK.getKey(), System.currentTimeMillis());
    message.getProperties().setHeader(WireProperty.SERVICE_GROUP.getKey(), serviceGroup);

    if (instanceId != null) {
      message.getProperties().setHeader(WireProperty.INSTANCE_ID.getKey(), instanceId);
      messageRouter.getWhisperRequestTopic().send(message);
    } else {
      messageRouter.getTalkRequestQueue().send(message);
    }

    if (!inOnly) {
      if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(messageId, asynchronousCallback = new AsynchronousTransmissionCallback(address.getService(), address.getFunction().getName()))) != null) {

        return previousCallback;
      }

      return asynchronousCallback;
    } else {

      return null;
    }
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

  @Override
  public void close () throws Exception {

  }
}
