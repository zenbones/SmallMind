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
package org.smallmind.phalanx.wire.transport.mock;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.phalanx.wire.ServiceDefinitionException;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * In-memory response transport for use in tests that do not require a real message broker.
 * Listens on the shared talk request queue and whisper request topic provided by
 * {@link MockMessageRouter}, dispatches each invocation through {@link WireInvocationCircuit},
 * and publishes {@link org.smallmind.phalanx.wire.signal.ResultSignal}s back to the response topic.
 */
public class MockResponseTransport implements ResponseTransport, ResponseTransmitter {

  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final MockMessageRouter messageRouter;
  private final SignalCodec signalCodec;
  private final String instanceId = UUID.randomUUID().toString();

  /**
   * Constructs the response transport and registers listeners on the talk request queue and
   * the whisper request topic so that inbound invocations are immediately dispatched through
   * the invocation circuit.
   *
   * @param messageRouter shared router providing the talk queue, whisper topic, and response topic
   * @param signalCodec   codec used to deserialize invocation signals and serialize result signals
   */
  public MockResponseTransport (MockMessageRouter messageRouter, final SignalCodec signalCodec) {

    this.messageRouter = messageRouter;
    this.signalCodec = signalCodec;

    messageRouter.getTalkRequestQueue().addListener(new MockMessageListener() {

      /**
       * Accepts all talk-mode requests regardless of their properties.
       *
       * @param properties metadata of the candidate message (not evaluated)
       * @return always {@code true}
       */
      @Override
      public boolean match (MockMessageProperties properties) {

        return true;
      }

      /**
       * Decodes the {@link InvocationSignal} from {@code message} and dispatches it to the
       * invocation circuit.  Any error is logged and swallowed.
       *
       * @param message the talk-mode request message to process
       */
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

      /**
       * Accepts whisper-mode requests whose {@code INSTANCE_ID} header matches this transport's
       * instance ID.
       *
       * @param properties metadata of the candidate message
       * @return {@code true} if the message is targeted at this transport instance
       */
      @Override
      public boolean match (MockMessageProperties properties) {

        return properties.getHeader(WireProperty.INSTANCE_ID.getKey()).equals(instanceId);
      }

      /**
       * Decodes the {@link InvocationSignal} from {@code message} and dispatches it to the
       * invocation circuit.  Any error is logged and swallowed.
       *
       * @param message the whisper-mode request message to process
       */
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

  /**
   * Returns the unique instance identifier assigned to this transport at creation time.
   * Callers use this value to address whisper-mode requests directly to this node.
   *
   * @return instance ID string
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }

  /**
   * Registers a service implementation with the invocation circuit so its methods can be
   * resolved and invoked when invocation signals arrive.
   *
   * @param serviceInterface the interface declaring the remotely callable methods
   * @param targetService    the concrete service instance and its associated metadata
   * @return this transport's instance ID; callers must supply this value when directing
   * whisper-mode requests to this specific node
   * @throws NoSuchMethodException      if a declared method cannot be located on the target
   * @throws ServiceDefinitionException if the service registration is malformed or conflicts
   */
  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  /**
   * Returns the current lifecycle state of this transport.
   *
   * @return one of {@link TransportState#PLAYING}, {@link TransportState#PAUSED},
   * or {@link TransportState#CLOSED}
   */
  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  /**
   * Resumes talk-request delivery if the transport is currently {@link TransportState#PAUSED}.
   * Does nothing when the transport is in any other state.
   */
  @Override
  public void play () {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
        messageRouter.getTalkRequestQueue().play();
      }
    }
  }

  /**
   * Suspends talk-request delivery if the transport is currently {@link TransportState#PLAYING}.
   * Does nothing when the transport is in any other state.
   */
  @Override
  public void pause () {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
        messageRouter.getTalkRequestQueue().pause();
      }
    }
  }

  /**
   * Encodes a {@link org.smallmind.phalanx.wire.signal.ResultSignal} and publishes it to the
   * mock response topic so the originating request transport can complete its pending callback.
   *
   * @param callerId      identifier of the originating caller; set as the {@code CALLER_ID} header
   *                      so the response listener on that transport can match the message
   * @param correlationId correlation ID from the originating request; set as the message
   *                      correlation ID for pairing with the pending callback
   * @param error         {@code true} when the result payload represents a service-side error
   * @param nativeType    Java type name of the result payload, used by the caller for deserialization
   * @param result        the return value or error object to encode in the signal
   * @throws Exception if signal encoding fails
   */
  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Exception {

    MockMessage message = new MockMessage(signalCodec.encode(new ResultSignal(error, nativeType, result)));

    message.getProperties().setHeader(WireProperty.CALLER_ID.getKey(), callerId);
    message.getProperties().setContentType(signalCodec.getContentType());
    message.getProperties().setMessageId(UUID.randomUUID().toString());
    message.getProperties().setTimestamp(LocalDateTime.now());
    message.getProperties().setCorrelationId(correlationId.getBytes(StandardCharsets.UTF_8));

    messageRouter.getResponseTopic().send(message);
  }

  /**
   * Transitions the transport to the {@link TransportState#CLOSED} state.  No additional
   * resources need to be released.
   *
   * @throws Exception never thrown
   */
  @Override
  public void close ()
    throws Exception {

    synchronized (transportStateRef) {
      transportStateRef.set(TransportState.CLOSED);
    }
  }
}
