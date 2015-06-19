package org.smallmind.phalanx.wire.jms;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.AsynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.LocationType;
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.SynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.TransmissionCallback;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.WhisperLocation;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class JmsRequestTransport implements MetricConfigurationProvider, RequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MetricConfiguration metricConfiguration;
  private final SignalCodec signalCodec;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final LinkedBlockingQueue<MessageHandler> talkQueue;
  private final LinkedBlockingQueue<MessageHandler> whisperQueue;
  private final ConnectionManager[] talkRequestConnectionManagers;
  private final ConnectionManager[] whisperRequestConnectionManagers;
  private final ResponseListener[] responseListeners;
  private final String transportId = SnowflakeId.newInstance().generateDottedString();

  public JmsRequestTransport (MetricConfiguration metricConfiguration, RoutingFactories routingFactories, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, int maximumMessageLength, int timeoutSeconds)
    throws IOException, JMSException, TransportException {

    int talkIndex = 0;
    int whisperIndex = 0;

    this.metricConfiguration = metricConfiguration;
    this.signalCodec = signalCodec;

    callbackMap = new SelfDestructiveMap<>(new Duration(timeoutSeconds, TimeUnit.SECONDS));

    talkRequestConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < talkRequestConnectionManagers.length; index++) {
      talkRequestConnectionManagers[index] = new ConnectionManager(routingFactories.getRequestQueueFactory(), messagePolicy, reconnectionPolicy);
    }
    whisperRequestConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < whisperRequestConnectionManagers.length; index++) {
      whisperRequestConnectionManagers[index] = new ConnectionManager(routingFactories.getRequestTopicFactory(), messagePolicy, reconnectionPolicy);
    }

    talkQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      talkQueue.add(new QueueOperator(talkRequestConnectionManagers[talkIndex], (Queue)routingFactories.getRequestQueueFactory().getDestination()));
      if (++talkIndex == talkRequestConnectionManagers.length) {
        talkIndex = 0;
      }
    }
    whisperQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      whisperQueue.add(new TopicOperator(whisperRequestConnectionManagers[whisperIndex], (Topic)routingFactories.getRequestTopicFactory().getDestination()));
      if (++whisperIndex == whisperRequestConnectionManagers.length) {
        whisperIndex = 0;
      }
    }

    responseListeners = new ResponseListener[clusterSize];
    for (int index = 0; index < responseListeners.length; index++) {
      responseListeners[index] = new ResponseListener(this, new ConnectionManager(routingFactories.getResponseTopicFactory(), messagePolicy, reconnectionPolicy), (Topic)routingFactories.getResponseTopicFactory().getDestination(), signalCodec, transportId, maximumMessageLength);
    }
  }

  @Override
  public String getTransportId () {

    return transportId;
  }

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  @Override
  public void transmitInOnly (Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    transmit(true, address, arguments, contexts);
  }

  @Override
  public Object transmitInOut (Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    TransmissionCallback transmissionCallback;

    if ((transmissionCallback = transmit(false, address, arguments, contexts)) != null) {

      return transmissionCallback.getResult(signalCodec);
    }

    return null;
  }

  private TransmissionCallback transmit (boolean inOnly, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    final MessageHandler messageHandler;

    switch (address.getLocation().getType()) {
      case TALK:
        messageHandler = acquireMessageHandler(talkQueue);
        break;
      case WHISPER:
        messageHandler = acquireMessageHandler(whisperQueue);
        break;
      default:
        throw new UnknownSwitchCaseException(address.getLocation().getType().name());
    }

    try {

      AsynchronousTransmissionCallback asynchronousCallback;
      SynchronousTransmissionCallback previousCallback;
      Message requestMessage;

      messageHandler.send(requestMessage = constructMessage(messageHandler, inOnly, address, arguments, contexts));

      if (!inOnly) {
        if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(requestMessage.getJMSMessageID(), asynchronousCallback = new AsynchronousTransmissionCallback(address.getLocation().getService(), address.getLocation().getFunction().getName()))) != null) {

          return previousCallback;
        }

        return asynchronousCallback;
      } else {

        return null;
      }
    } finally {
      returnMessageHandler(address.getLocation().getType(), messageHandler);
    }
  }

  private void returnMessageHandler (LocationType locationType, MessageHandler messageHandler)
    throws InterruptedException, UnknownSwitchCaseException {

    switch (locationType) {
      case TALK:
        talkQueue.put(messageHandler);
        break;
      case WHISPER:
        whisperQueue.put(messageHandler);
        break;
      default:
        throw new UnknownSwitchCaseException(locationType.name());
    }
  }

  private MessageHandler acquireMessageHandler (final LinkedBlockingQueue<MessageHandler> messageHandlerQueue)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<MessageHandler>(this, new MetricProperty("event", MetricType.ACQUIRE_REQUEST_DESTINATION.getDisplay())) {

      @Override
      public MessageHandler withChronometer ()
        throws TransportException, InterruptedException {

        MessageHandler messageHandler;

        do {
          messageHandler = messageHandlerQueue.poll(1, TimeUnit.SECONDS);
        } while ((!closed.get()) && (messageHandler == null));

        if (messageHandler == null) {
          throw new TransportException("Message transmission has been closed");
        }

        return messageHandler;
      }
    });
  }

  private Message constructMessage (final MessageHandler messageHandler, final boolean inOnly, final Address address, final Map<String, Object> arguments, final WireContext... contexts)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<Message>(this, new MetricProperty("event", MetricType.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public Message withChronometer ()
        throws Exception {

        BytesMessage requestMessage;

        requestMessage = messageHandler.createMessage();

        requestMessage.writeBytes(signalCodec.encode(new InvocationSignal(inOnly, address, arguments, contexts)));

        if (!inOnly) {
          requestMessage.setStringProperty(WireProperty.TRANSPORT_ID.getKey(), transportId);
        }

        requestMessage.setStringProperty(WireProperty.CONTENT_TYPE.getKey(), signalCodec.getContentType());
        requestMessage.setLongProperty(WireProperty.CLOCK.getKey(), System.currentTimeMillis());

        if (address.getLocation().getType().equals(LocationType.WHISPER)) {
          requestMessage.setStringProperty(WireProperty.INSTANCE_ID.getKey(), ((WhisperLocation)address.getLocation()).getInstanceId());
        }

        return requestMessage;
      }
    });
  }

  public void completeCallback (String correlationId, ResultSignal resultSignal) {

    TransmissionCallback previousCallback;

    if ((previousCallback = callbackMap.get(correlationId)) == null) {
      if ((previousCallback = callbackMap.putIfAbsent(correlationId, new SynchronousTransmissionCallback(resultSignal))) != null) {
        if (previousCallback instanceof AsynchronousTransmissionCallback) {
          ((AsynchronousTransmissionCallback)previousCallback).setResultSignal(resultSignal);
        }
      }
    } else if (previousCallback instanceof AsynchronousTransmissionCallback) {
      ((AsynchronousTransmissionCallback)previousCallback).setResultSignal(resultSignal);
    }
  }

  @Override
  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (ConnectionManager requestConnectionManager : whisperRequestConnectionManagers) {
        requestConnectionManager.stop();
      }
      for (ConnectionManager requestConnectionManager : talkRequestConnectionManagers) {
        requestConnectionManager.stop();
      }

      for (ConnectionManager requestConnectionManager : whisperRequestConnectionManagers) {
        requestConnectionManager.close();
      }
      for (ConnectionManager requestConnectionManager : talkRequestConnectionManagers) {
        requestConnectionManager.close();
      }

      for (ResponseListener responseListener : responseListeners) {
        responseListener.close();
      }

      callbackMap.shutdown();
    }
  }
}
