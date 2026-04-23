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
package org.smallmind.phalanx.wire.transport.jms;

import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.Worker;

/**
 * Worker that dequeues incoming JMS {@link BytesMessage} payloads, decodes them as
 * {@link InvocationSignal} objects, and dispatches them through the {@link WireInvocationCircuit}.
 *
 * <p>One worker instance is allocated per slot in the worker pool managed by
 * {@link JmsResponseTransport}.  Each call to {@link #engageWork(Message)} handles exactly
 * one request message.
 */
public class InvocationWorker extends Worker<Message> {

  private final ResponseTransmitter responseTransmitter;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  private final byte[] buffer;

  /**
   * Constructs a worker that can decode and dispatch invocation messages.
   *
   * @param workQueue            work queue from which this worker draws messages
   * @param responseTransmitter  transmitter used to send results back to callers
   * @param invocationCircuit    circuit that routes decoded invocations to registered services
   * @param signalCodec          codec used to deserialise {@link InvocationSignal} payloads
   * @param maximumMessageLength maximum byte length of any single inbound message payload
   */
  public InvocationWorker (WorkQueue<Message> workQueue, ResponseTransmitter responseTransmitter, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec, int maximumMessageLength) {

    super(workQueue);

    this.responseTransmitter = responseTransmitter;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;

    buffer = new byte[maximumMessageLength];
  }

  /**
   * Reads the byte payload from {@code message}, decodes an {@link InvocationSignal},
   * and executes the invocation through the circuit.  Throws {@link TransportException}
   * if the payload exceeds the configured maximum.
   *
   * @param message inbound JMS {@link BytesMessage} containing an encoded {@link InvocationSignal}
   * @throws Throwable if decoding or invocation dispatch fails
   */
  @Override
  public void engageWork (final Message message)
    throws Throwable {

    if (((BytesMessage)message).getBodyLength() > buffer.length) {
      throw new TransportException("Message length exceeds maximum capacity %d > %d", ((BytesMessage)message).getBodyLength(), buffer.length);
    } else {

      final InvocationSignal invocationSignal;

      ((BytesMessage)message).readBytes(buffer);
      invocationSignal = signalCodec.decode(buffer, 0, (int)((BytesMessage)message).getBodyLength(), InvocationSignal.class);

      Instrument.with(InvocationWorker.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName())).on(
        () -> invocationCircuit.handle(responseTransmitter, signalCodec, message.getStringProperty(WireProperty.CALLER_ID.getKey()), message.getJMSMessageID(), invocationSignal)
      );
    }
  }

  /**
   * Performs no action; this worker holds no resources that require explicit release.
   */
  @Override
  public void close () {

  }
}
