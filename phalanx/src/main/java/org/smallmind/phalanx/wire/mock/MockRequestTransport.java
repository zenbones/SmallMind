package org.smallmind.phalanx.wire.mock;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.AsynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.SynchronousTransmissionCallback;
import org.smallmind.phalanx.wire.TransmissionCallback;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class MockRequestTransport implements RequestTransport {

  private final MockMessageRouter messageRouter;
  private final SignalCodec signalCodec;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final String callerId = UUID.randomUUID().toString();

  public MockRequestTransport (MockMessageRouter messageRouter, final SignalCodec signalCodec, int timeoutSeconds) {

    this.messageRouter = messageRouter;
    this.signalCodec = signalCodec;

    callbackMap = new SelfDestructiveMap<>(new Duration(timeoutSeconds, TimeUnit.SECONDS));

    messageRouter.getResponseTopic().addListener(new MockMessageListener() {

      @Override
      public boolean match (MockMessageProperties properties) {

        return properties.getHeader(WireProperty.CALLER_ID.getKey()).equals(callerId);
      }

      @Override
      public void handle (MockMessage message) {

        try {
          completeCallback(new String(message.getProperties().getCorrelationId()), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, ResultSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockRequestTransport.class).error(exception);
        }
      }
    });
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  @Override
  public void transmitInOnly (String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    transmit(true, serviceGroup, instanceId, address, arguments, contexts);
  }

  @Override
  public Object transmitInOut (String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    return transmit(false, serviceGroup, instanceId, address, arguments, contexts).getResult(signalCodec);
  }

  private TransmissionCallback transmit (boolean inOnly, String serviceGroup, String instanceId, Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Exception {

    MockMessage message = new MockMessage(signalCodec.encode(new InvocationSignal(inOnly, address, arguments, contexts)));
    AsynchronousTransmissionCallback asynchronousCallback;
    SynchronousTransmissionCallback previousCallback;
    String messageId = UUID.randomUUID().toString();

    if (!inOnly) {
      message.getProperties().setHeader(WireProperty.CALLER_ID.getKey(), callerId);
    }

    message.getProperties().setMessageId(messageId);
    message.getProperties().setTimestamp(new Date());
    message.getProperties().setContentType(signalCodec.getContentType());
    message.getProperties().setHeader(WireProperty.CLOCK.getKey(), System.currentTimeMillis());
    message.getProperties().setHeader(WireProperty.SERVICE_GROUP.getKey(), serviceGroup);

    if (instanceId != null) {
      message.getProperties().setHeader(WireProperty.INSTANCE_ID.getKey(), instanceId);
      messageRouter.getWhisperRequestTopic().send(message);
    } else {
      messageRouter.getTalkRequestQueue().send(message);
    }

    if (!inOnly) {
      if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(messageId, asynchronousCallback = new AsynchronousTransmissionCallback(address.getService(), address.getFunction().getName()))) != null) {

        return previousCallback;
      }

      return asynchronousCallback;
    } else {

      return null;
    }
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
  public void close () throws Exception {

  }
}
