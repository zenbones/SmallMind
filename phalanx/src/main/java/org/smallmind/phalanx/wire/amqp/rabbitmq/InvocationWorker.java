package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.util.Map;
import java.util.concurrent.TransferQueue;
import javax.jms.JMSException;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.worker.Worker;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;

public class InvocationWorker extends Worker<RabbitMQMessage> {

  private static final String TRANSPORT_ID_AMQP_KEY = "x-opt-" + WireProperty.TRANSPORT_ID.getKey();

  private final ResponseTransport responseTransport;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  public InvocationWorker (MetricConfiguration metricConfiguration, TransferQueue<RabbitMQMessage> workTransferQueue, ResponseTransport responseTransport, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec) {

    super(metricConfiguration, workTransferQueue);

    this.responseTransport = responseTransport;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;
  }

  @Override
  public void engageWork (final RabbitMQMessage message)
    throws Exception {

    final InvocationSignal invocationSignal;

    invocationSignal = signalCodec.decode(message.getBody(), 0, message.getBody().length, InvocationSignal.class);
    InstrumentationManager.execute(new ChronometerInstrument(this, new MetricProperty("operation", "invoke"), new MetricProperty("service", invocationSignal.getAddress().getLocation().getService()), new MetricProperty("method", invocationSignal.getAddress().getLocation().getFunction().getName())) {

      @Override
      public void withChronometer ()
        throws JMSException {

        invocationCircuit.handle(responseTransport, signalCodec, getTransportId(message.getProperties().getHeaders()), message.getProperties().getMessageId(), invocationSignal);
      }
    });
  }

  private String getTransportId (Map<String, Object> headers) {

    if ((headers != null) && (headers.containsKey(TRANSPORT_ID_AMQP_KEY))) {

      return headers.get(TRANSPORT_ID_AMQP_KEY).toString();
    }

    return null;
  }

  @Override
  public void close () {

  }
}
