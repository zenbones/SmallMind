package org.smallmind.throng.wire.jms;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import org.smallmind.throng.wire.MetricType;
import org.smallmind.throng.wire.ResultSignal;
import org.smallmind.throng.wire.SignalCodec;
import org.smallmind.throng.wire.TransportException;
import org.smallmind.throng.wire.WireProperty;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final JmsRequestTransport requestTransport;
  private final ConnectionManager responseConnectionManager;
  private final Topic responseTopic;
  private final SignalCodec signalCodec;
  private final String selector;
  private final byte[] buffer;

  public ResponseListener (JmsRequestTransport requestTransport, ConnectionManager responseConnectionManager, Topic responseTopic, SignalCodec signalCodec, String callerId, int maximumMessageLength)
    throws JMSException {

    this.requestTransport = requestTransport;
    this.responseConnectionManager = responseConnectionManager;
    this.responseTopic = responseTopic;
    this.signalCodec = signalCodec;

    buffer = new byte[maximumMessageLength];
    selector = WireProperty.CALLER_ID.getKey() + "='" + callerId + "'";

    responseConnectionManager.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return responseTopic;
  }

  @Override
  public String getMessageSelector () {

    return selector;
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      responseConnectionManager.stop();
      responseConnectionManager.close();
    }
  }

  @Override
  public void onMessage (final Message message) {

    try {

      long timeInTopic = System.currentTimeMillis() - message.getLongProperty(WireProperty.CLOCK.getKey());

      LoggerManager.getLogger(ResponseListener.class).debug("response message received(%s) in %d ms...", message.getJMSMessageID(), timeInTopic);
      InstrumentationManager.instrumentWithChronometer(requestTransport, (timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS, new MetricProperty("queue", MetricType.RESPONSE_TOPIC_TRANSIT.getDisplay()));

      InstrumentationManager.execute(new ChronometerInstrument(requestTransport, new MetricProperty("event", MetricType.COMPLETE_CALLBACK.getDisplay())) {

        @Override
        public void withChronometer ()
          throws Exception {

          if (((BytesMessage)message).getBodyLength() > buffer.length) {
            throw new TransportException("Message length exceeds maximum capacity %d > %d", ((BytesMessage)message).getBodyLength(), buffer.length);
          }

          ((BytesMessage)message).readBytes(buffer);
          requestTransport.completeCallback(message.getJMSCorrelationID(), signalCodec.decode(buffer, 0, (int)((BytesMessage)message).getBodyLength(), ResultSignal.class));
        }
      });
    } catch (Exception exception) {
      LoggerManager.getLogger(ResponseListener.class).error(exception);
    }
  }
}