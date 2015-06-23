package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.AsynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.SynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.TransmissionCallback;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class RabbitMQRequestTransport implements MetricConfigurationProvider, RequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MetricConfiguration metricConfiguration;
  private final SignalCodec signalCodec;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final LinkedBlockingQueue<RequestMessageRouter> routerQueue;
  private final RequestMessageRouter[] requestMessageRouters;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  public RabbitMQRequestTransport (MetricConfiguration metricConfiguration, RabbitMQConnector rabbitMQConnector, NameConfiguration nameConfiguration, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, int timeoutSeconds)
    throws IOException, InterruptedException {

    int routerIndex = 0;

    this.metricConfiguration = metricConfiguration;
    this.signalCodec = signalCodec;

    callbackMap = new SelfDestructiveMap<>(new Duration(timeoutSeconds, TimeUnit.SECONDS));

    requestMessageRouters = new RequestMessageRouter[clusterSize];
    for (int index = 0; index < requestMessageRouters.length; index++) {
      requestMessageRouters[index] = new RequestMessageRouter(rabbitMQConnector, nameConfiguration, this, signalCodec, callerId, index, timeoutSeconds * 3);
      requestMessageRouters[index].initialize();
    }

    routerQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      routerQueue.add(requestMessageRouters[routerIndex]);
      if (++routerIndex == requestMessageRouters.length) {
        routerIndex = 0;
      }
    }
  }

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  @Override
  public String getCallerId () {

    return callerId;
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

    final RequestMessageRouter requestMessageRouter = acquireRequestMessageRouter();

    try {

      AsynchronousTransmissionCallback asynchronousCallback;
      SynchronousTransmissionCallback previousCallback;
      String messageId;

      messageId = requestMessageRouter.publish(inOnly, address, arguments, contexts);

      if (!inOnly) {
        if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(messageId, asynchronousCallback = new AsynchronousTransmissionCallback(address.getLocation().getService(), address.getLocation().getFunction().getName()))) != null) {

          return previousCallback;
        }

        return asynchronousCallback;
      } else {

        return null;
      }
    } finally {
      routerQueue.put(requestMessageRouter);
    }
  }

  private RequestMessageRouter acquireRequestMessageRouter ()
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<RequestMessageRouter>(this, new MetricProperty("event", MetricType.ACQUIRE_REQUEST_DESTINATION.getDisplay())) {

      @Override
      public RequestMessageRouter withChronometer ()
        throws TransportException, InterruptedException {

        RequestMessageRouter messageTransmitter;

        do {
          messageTransmitter = routerQueue.poll(1, TimeUnit.SECONDS);
        } while ((!closed.get()) && (messageTransmitter == null));

        if (messageTransmitter == null) {
          throw new TransportException("Message transmission has been closed");
        }

        return messageTransmitter;
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
    throws IOException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (RequestMessageRouter requestMessageRouter : requestMessageRouters) {
        requestMessageRouter.close();
      }

      callbackMap.shutdown();
    }
  }
}
