package org.smallmind.phalanx.wire.transport.kafka;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseCallback implements Consumer<ConsumerRecord<Long, byte[]>> {

  private final KafkaResponseTransport transport;

  public ResponseCallback (KafkaResponseTransport transport) {

    this.transport = transport;
  }

  @Override
  public void accept (ConsumerRecord<Long, byte[]> record) {

    try {

      long timeInQueue = System.currentTimeMillis() - record.timestamp();

      LoggerManager.getLogger(ResponseCallback.class).debug("request message received(%s) in %d ms...", HeaderUtility.getHeader(record, HeaderUtility.MESSAGE_ID), timeInQueue);
      Instrument.with(ResponseCallback.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.REQUEST_TRANSIT_TIME.getDisplay())).update((timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS);

      transport.execute(record);
    } catch (Throwable throwable) {
      LoggerManager.getLogger(ResponseCallback.class).error(throwable.getMessage(), throwable);
    }
  }
}
