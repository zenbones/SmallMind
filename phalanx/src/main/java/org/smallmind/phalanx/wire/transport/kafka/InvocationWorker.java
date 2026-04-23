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
package org.smallmind.phalanx.wire.transport.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.Worker;

/**
 * Work-queue consumer that deserializes inbound Kafka records into {@link InvocationSignal}s
 * and dispatches each one through the {@link WireInvocationCircuit}.  Each invocation is wrapped
 * in a speedometer instrument tagged by operation, service, method name, and version to support
 * throughput monitoring.
 */
public class InvocationWorker extends Worker<ConsumerRecord<Long, byte[]>> {

  private final ResponseTransmitter responseTransmitter;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  /**
   * Constructs a worker bound to the given work queue and shared infrastructure.
   *
   * @param workQueue           queue from which Kafka records are drained
   * @param responseTransmitter transmitter used to publish results back to the originating caller
   * @param invocationCircuit   circuit that resolves and invokes the target service method
   * @param signalCodec         codec used to deserialize record bytes into an {@link InvocationSignal}
   */
  public InvocationWorker (WorkQueue<ConsumerRecord<Long, byte[]>> workQueue, ResponseTransmitter responseTransmitter, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec) {

    super(workQueue);

    this.responseTransmitter = responseTransmitter;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;
  }

  /**
   * Decodes an {@link InvocationSignal} from {@code record} and delegates execution to the
   * invocation circuit.  The entire operation is measured by a speedometer instrument tagged
   * with the route's service name, method name, and version.
   *
   * @param record the Kafka record whose value bytes encode an {@link InvocationSignal}
   * @throws Throwable if signal decoding fails or the invocation circuit raises an error
   */
  @Override
  public void engageWork (final ConsumerRecord<Long, byte[]> record)
    throws Throwable {

    InvocationSignal invocationSignal = signalCodec.decode(record.value(), 0, record.value().length, InvocationSignal.class);

    Instrument.with(InvocationWorker.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName()), new Tag("version", Integer.toString(invocationSignal.getRoute().getVersion()))).on(
      () -> invocationCircuit.handle(responseTransmitter, signalCodec, HeaderUtility.getHeader(record, "callerId"), HeaderUtility.getHeader(record, HeaderUtility.MESSAGE_ID), invocationSignal)
    );
  }

  /**
   * No-op implementation; all resources referenced by this worker are owned and closed
   * externally by the enclosing transport.
   */
  @Override
  public void close () {

  }
}
