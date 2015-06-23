package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.smallmind.phalanx.wire.ResultSignal;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.phalanx.wire.jms.QueueOperator;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseMessageRouter extends MessageRouter {

  private final RabbitMQResponseTransport responseTransport;
  private final SignalCodec signalCodec;
  private final String instanceId;
  private final int index;
  private final int ttlSeconds;

  public ResponseMessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration, RabbitMQResponseTransport responseTransport, SignalCodec signalCodec, String instanceId, int index, int ttlSeconds) {

    super(connector, nameConfiguration);

    this.responseTransport = responseTransport;
    this.signalCodec = signalCodec;
    this.instanceId = instanceId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
  }

  @Override
  public final void bindQueues (Channel channel)
    throws IOException {

    String queueName;

    channel.queueDeclare(getTalkQueueName(), false, false, false, null);
    channel.queueBind(getTalkQueueName(), getRequestExchangeName(), "");

    channel.queueDeclare(queueName = getWhisperQueueName() + "-" + instanceId, false, false, true, null);
    channel.queueBind(queueName, getRequestExchangeName(), instanceId);
  }

  @Override
  public void installConsumer (Channel channel)
    throws IOException {

    installConsumerInternal(channel, getTalkQueueName());
    installConsumerInternal(channel, getWhisperQueueName() + "-" + instanceId);
  }

  private void installConsumerInternal (Channel channel, String queueName)
    throws IOException {

    channel.basicConsume(queueName, true, queueName + "[" + index + "]", false, false, null, new DefaultConsumer(channel) {

      @Override
      public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

        try {

          long timeInQueue = System.currentTimeMillis() - getTimestamp(properties);

          LoggerManager.getLogger(QueueOperator.class).debug("request message received(%s) in %d ms...", properties.getMessageId(), timeInQueue);
          InstrumentationManager.instrumentWithChronometer(responseTransport, (timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS, new MetricProperty("queue", MetricType.REQUEST_DESTINATION_TRANSIT.getDisplay()));

          responseTransport.execute(new RabbitMQMessage(properties, body));
        } catch (Exception exception) {
          LoggerManager.getLogger(ResponseMessageRouter.class).error(exception);
        }
      }
    });
  }

  public String publish (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Exception {

    RabbitMQMessage rabbitMQMessage = constructMessage(correlationId, error, nativeType, result);

    send(callerId, getResponseExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());

    return rabbitMQMessage.getProperties().getMessageId();
  }

  private RabbitMQMessage constructMessage (final String correlationId, final boolean error, final String nativeType, final Object result)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<RabbitMQMessage>(responseTransport, new MetricProperty("event", MetricType.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public RabbitMQMessage withChronometer ()
        throws Exception {

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                                            .contentType(signalCodec.getContentType())
                                            .messageId(SnowflakeId.newInstance().generateDottedString())
                                            .correlationId(correlationId)
                                            .timestamp(new Date())
                                            .expiration(String.valueOf(ttlSeconds * 1000 * 3)).build();

        return new RabbitMQMessage(properties, signalCodec.encode(new ResultSignal(error, nativeType, result)));
      }
    });
  }
}
