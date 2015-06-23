package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.smallmind.phalanx.wire.Address;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.LocationType;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.WhisperLocation;
import org.smallmind.phalanx.wire.WireContext;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.wire.MetricType;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class RequestMessageRouter extends MessageRouter {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final RabbitMQRequestTransport requestTransport;
  private final SignalCodec signalCodec;
  private final String callerId;
  private final int index;
  private final int ttlSeconds;

  public RequestMessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration, RabbitMQRequestTransport requestTransport, SignalCodec signalCodec, String callerId, int index, int ttlSeconds) {

    super(connector, nameConfiguration);

    this.requestTransport = requestTransport;
    this.signalCodec = signalCodec;
    this.callerId = callerId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
  }

  @Override
  public final void bindQueues (Channel channel)
    throws IOException {

    String queueName;

    channel.queueDeclare(queueName = getResponseQueueName() + "-" + callerId, false, false, true, null);
    channel.queueBind(queueName, getResponseExchangeName(), callerId);
  }

  @Override
  public void installConsumer (Channel channel)
    throws IOException {

    channel.basicConsume(getResponseQueueName() + "-" + callerId, true, getResponseQueueName() + "-" + callerId + "[" + index + "]", false, false, null, new DefaultConsumer(channel) {

      @Override
      public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

        try {

          long timeInTopic = System.currentTimeMillis() - getTimestamp(properties);

          LoggerManager.getLogger(ResponseMessageRouter.class).debug("response message received(%s) in %d ms...", properties.getMessageId(), timeInTopic);
          InstrumentationManager.instrumentWithChronometer(requestTransport, (timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS, new MetricProperty("queue", MetricType.RESPONSE_TOPIC_TRANSIT.getDisplay()));

          InstrumentationManager.execute(new ChronometerInstrument(requestTransport, new MetricProperty("event", MetricType.COMPLETE_CALLBACK.getDisplay())) {

            @Override
            public void withChronometer ()
              throws Exception {

              requestTransport.completeCallback(properties.getCorrelationId(), signalCodec.decode(body, 0, body.length, ResultSignal.class));
            }
          });
        } catch (Exception exception) {
          LoggerManager.getLogger(ResponseMessageRouter.class).error(exception);
        }
      }
    });
  }

  public String publish (final boolean inOnly, final Address address, final Map<String, Object> arguments, final WireContext... contexts)
    throws Exception {

    RabbitMQMessage rabbitMQMessage = constructMessage(inOnly, address, arguments, contexts);

    send(address.getLocation().getType().equals(LocationType.WHISPER) ? ((WhisperLocation)address.getLocation()).getInstanceId() : "", getRequestExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());

    return rabbitMQMessage.getProperties().getMessageId();
  }

  private RabbitMQMessage constructMessage (final boolean inOnly, final Address address, final Map<String, Object> arguments, final WireContext... contexts)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<RabbitMQMessage>(requestTransport, new MetricProperty("event", MetricType.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public RabbitMQMessage withChronometer ()
        throws Exception {

        HashMap<String, Object> headerMap = new HashMap<>();

        if (!inOnly) {
          headerMap.put(CALLER_ID_AMQP_KEY, callerId);
        }

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                                            .contentType(signalCodec.getContentType())
                                            .messageId(SnowflakeId.newInstance().generateDottedString())
                                            .timestamp(new Date())
                                            .expiration(String.valueOf(ttlSeconds * 1000 * 3))
                                            .headers(headerMap).build();

        return new RabbitMQMessage(properties, signalCodec.encode(new InvocationSignal(inOnly, address, arguments, contexts)));
      }
    });
  }
}

