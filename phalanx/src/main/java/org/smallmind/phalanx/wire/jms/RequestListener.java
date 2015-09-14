package org.smallmind.phalanx.wire.jms;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class RequestListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final JmsResponseTransport jmsResponseTransport;
  private final ConnectionManager requestConnectionManager;
  private final Destination requestDestination;
  private final String selector;

  public RequestListener (JmsResponseTransport jmsResponseTransport, ConnectionManager requestConnectionManager, Destination requestDestination, String serviceGroup, String instanceId)
    throws JMSException {

    this.jmsResponseTransport = jmsResponseTransport;
    this.requestConnectionManager = requestConnectionManager;
    this.requestDestination = requestDestination;

    selector = (instanceId == null) ? WireProperty.SERVICE_GROUP.getKey() + "='" + serviceGroup + "'" : WireProperty.SERVICE_GROUP.getKey() + "='" + serviceGroup + "' AND " + WireProperty.INSTANCE_ID.getKey() + "='" + instanceId + "'";

    requestConnectionManager.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return requestDestination;
  }

  @Override
  public String getMessageSelector () {

    return selector;
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestConnectionManager.stop();
      requestConnectionManager.close();
    }
  }

  @Override
  public void onMessage (final Message message) {

    try {

      long timeInQueue = System.currentTimeMillis() - message.getLongProperty(WireProperty.CLOCK.getKey());

      LoggerManager.getLogger(QueueOperator.class).debug("request message received(%s) in %d ms...", message.getJMSMessageID(), timeInQueue);
      InstrumentationManager.instrumentWithChronometer(jmsResponseTransport, (timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS, new MetricProperty("queue", MetricType.REQUEST_DESTINATION_TRANSIT.getDisplay()));

      jmsResponseTransport.execute(message);
    } catch (Exception exception) {
      LoggerManager.getLogger(RequestListener.class).error(exception);
    }
  }
}
