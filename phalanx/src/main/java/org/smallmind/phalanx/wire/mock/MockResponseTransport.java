package org.smallmind.phalanx.wire.mock;

import java.util.Date;
import java.util.UUID;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.ServiceDefinitionException;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.wire.WiredService;
import org.smallmind.scribe.pen.LoggerManager;

public class MockResponseTransport implements ResponseTransport {

  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final MockMessageRouter messageRouter;
  private final SignalCodec signalCodec;
  private final String instanceId = UUID.randomUUID().toString();

  public MockResponseTransport (MockMessageRouter messageRouter, final SignalCodec signalCodec) {

    this.messageRouter = messageRouter;
    this.signalCodec = signalCodec;

    messageRouter.getTalkRequestQueue().addListener(new MockMessageListener() {
      @Override
      public boolean match (MockMessageProperties properties) {

        return true;
      }

      @Override
      public void handle (MockMessage message) {

        try {
          invocationCircuit.handle(MockResponseTransport.this, signalCodec, (String)message.getProperties().getHeader(WireProperty.CALLER_ID.getKey()), message.getProperties().getMessageId(), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, InvocationSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockResponseTransport.class).error(exception);
        }
      }
    });

    messageRouter.getWhisperRequestTopic().addListener(new MockMessageListener() {
      @Override
      public boolean match (MockMessageProperties properties) {

        return properties.getHeader(WireProperty.INSTANCE_ID.getKey()).equals(instanceId);
      }

      @Override
      public void handle (MockMessage message) {

        try {
          invocationCircuit.handle(MockResponseTransport.this, signalCodec, (String)message.getProperties().getHeader(WireProperty.CALLER_ID.getKey()), message.getProperties().getMessageId(), signalCodec.decode(message.getBytes(), 0, message.getBytes().length, InvocationSignal.class));
        } catch (Exception exception) {
          LoggerManager.getLogger(MockResponseTransport.class).error(exception);
        }
      }
    });
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Exception {

    MockMessage message = new MockMessage(signalCodec.encode(new ResultSignal(error, nativeType, result)));

    message.getProperties().setHeader(WireProperty.CALLER_ID.getKey(), callerId);
    message.getProperties().setContentType(signalCodec.getContentType());
    message.getProperties().setMessageId(UUID.randomUUID().toString());
    message.getProperties().setTimestamp(new Date());
    message.getProperties().setCorrelationId(correlationId.getBytes());

    messageRouter.getResponseTopic().send(message);
  }

  @Override
  public void close ()
    throws Exception {

  }
}