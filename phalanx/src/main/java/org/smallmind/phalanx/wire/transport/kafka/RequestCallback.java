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
