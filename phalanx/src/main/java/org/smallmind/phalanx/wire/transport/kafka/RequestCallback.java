/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.amqp.rabbitmq.RequestMessageRouter;
import org.smallmind.scribe.pen.LoggerManager;

public class RequestCallback implements Consumer<ConsumerRecord<Long, byte[]>> {

  private final KafkaRequestTransport transport;
  private final SignalCodec signalCodec;

  public RequestCallback (KafkaRequestTransport transport, SignalCodec signalCodec) {

    this.transport = transport;
    this.signalCodec = signalCodec;
  }

  @Override
  public void accept (ConsumerRecord<Long, byte[]> record) {

    try {

      long timeInTopic = System.currentTimeMillis() - record.timestamp();

      LoggerManager.getLogger(RequestCallback.class).debug("response message received(%s) in %d ms...", HeaderUtility.getHeader(record, HeaderUtility.MESSAGE_ID), timeInTopic);
      Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.RESPONSE_TRANSIT_TIME.getDisplay())).update((timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS);

      Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.COMPLETE_CALLBACK.getDisplay())).on(
        () -> transport.completeCallback(HeaderUtility.getHeader(record, HeaderUtility.CORRELATION_ID), signalCodec.decode(record.value(), 0, record.value().length, ResultSignal.class))
      );
    } catch (Throwable throwable) {
      LoggerManager.getLogger(RequestCallback.class).error(throwable.getMessage(), throwable);
    }
  }
}
