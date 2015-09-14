package org.smallmind.phalanx.wire.jms;

import java.util.concurrent.TransferQueue;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.worker.Worker;

public class InvocationWorker extends Worker<Message> {

  private final ResponseTransport responseTransport;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  private final byte[] buffer;

  public InvocationWorker (MetricConfiguration metricConfiguration, TransferQueue<Message> workTransferQueue, ResponseTransport responseTransport, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec, int maximumMessageLength) {

    super(metricConfiguration, workTransferQueue);

    this.responseTransport = responseTransport;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;

    buffer = new byte[maximumMessageLength];
  }

  @Override
  public void engageWork (final Message message)
    throws Exception {

    if (((BytesMessage)message).getBodyLength() > buffer.length) {
      throw new TransportException("Message length exceeds maximum capacity %d > %d", ((BytesMessage)message).getBodyLength(), buffer.length);
    } else {

      final InvocationSignal invocationSignal;

      ((BytesMessage)message).readBytes(buffer);
      invocationSignal = signalCodec.decode(buffer, 0, (int)((BytesMessage)message).getBodyLength(), InvocationSignal.class);
      InstrumentationManager.execute(new ChronometerInstrument(this, new MetricProperty("operation", "invoke"), new MetricProperty("service", invocationSignal.getAddress().getService()), new MetricProperty("method", invocationSignal.getAddress().getFunction().getName())) {

        @Override
        public void withChronometer ()
          throws JMSException {

          invocationCircuit.handle(responseTransport, signalCodec, message.getStringProperty(WireProperty.CALLER_ID.getKey()), message.getJMSMessageID(), invocationSignal);
        }
      });
    }
  }

  @Override
  public void close () {

  }
}
