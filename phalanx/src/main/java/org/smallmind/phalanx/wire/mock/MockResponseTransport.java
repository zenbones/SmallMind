/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.ServiceDefinitionException;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.TransportState;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.wire.WiredService;
import org.smallmind.scribe.pen.LoggerManager;

public class MockResponseTransport implements ResponseTransport {

  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final MockMessageRouter messageRouter;
  private final SignalCodec signalCodec;
  private final String instanceId = UUID.randomUUID().toString();

  public MockResponseTransport (MockMessageRouter messageRouter, final SignalCodec signalCodec) {

    this.messageRouter = messageRouter;
    this.signalCodec = signalCodec;

    messageRouter.getTalkRequestQueue().addListener(new MockMessageListener() {

      @Override
      public boolean match (MockMessageProperties properties) {

        return true;
      }

      @Override
      public void handle (MockMessage message) {

        try {
          invocationCircuit.handle(MockResponseTransport.this, signalCodec, (String)message.getProperties().getHeader(WireProperty.CALLER_ID.getKey()), message.getProperties().getMessageId(), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, InvocationSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockResponseTransport.class).error(exception);
        }
      }
    });

    messageRouter.getWhisperRequestTopic().addListener(new MockMessageListener() {

      @Override
      public boolean match (MockMessageProperties properties) {

        return properties.getHeader(WireProperty.INSTANCE_ID.getKey()).equals(instanceId);
      }

      @Override
      public void handle (MockMessage message) {

        try {
          invocationCircuit.handle(MockResponseTransport.this, signalCodec, (String)message.getProperties().getHeader(WireProperty.CALLER_ID.getKey()), message.getProperties().getMessageId(), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, InvocationSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockResponseTransport.class).error(exception);
        }
      }
    });
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  @Override
  public void play () {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
        messageRouter.getTalkRequestQueue().play();
      }
    }
  }

  @Override
  public void pause () {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
        messageRouter.getTalkRequestQueue().pause();
      }
    }
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Exception {

    MockMessage message = new MockMessage(signalCodec.encode(new ResultSignal(error, nativeType, result)));

    message.getProperties().setHeader(WireProperty.CALLER_ID.getKey(), callerId);
    message.getProperties().setContentType(signalCodec.getContentType());
    message.getProperties().setMessageId(UUID.randomUUID().toString());
    message.getProperties().setTimestamp(new Date());
    message.getProperties().setCorrelationId(correlationId.getBytes());

    messageRouter.getResponseTopic().send(message);
  }

  @Override
  public void close ()
    throws Exception {

    synchronized (transportStateRef) {
      transportStateRef.set(TransportState.CLOSED);
    }
  }
}