package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class MessageRouter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicStampedReference<Channel> channelRef = new AtomicStampedReference<>(null, 0);
  private final AtomicInteger version = new AtomicInteger(0);
  private final RabbitMQConnector connector;
  private final NameConfiguration nameConfiguration;

  public MessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration) {

    this.connector = connector;
    this.nameConfiguration = nameConfiguration;
  }

  public abstract void bindQueues (Channel channel)
    throws IOException;

  public abstract void installConsumer (Channel channel)
    throws IOException;

  public void initialize ()
    throws IOException {

    ensureChannel(0);
  }

  public String getRequestExchangeName () {

    return nameConfiguration.getRequestExchange();
  }

  public String getResponseExchangeName () {

    return nameConfiguration.getResponseExchange();
  }

  public String getResponseQueueName () {

    return nameConfiguration.getResponseQueue();
  }

  public String getTalkQueueName () {

    return nameConfiguration.getTalkQueue();
  }

  public String getWhisperQueueName () {

    return nameConfiguration.getWhisperQueue();
  }

  public void ensureChannel (int stamp)
    throws IOException {

    synchronized (channelRef) {
      if (channelRef.getStamp() == stamp) {

        Channel channel;
        final int nextStamp;

        channel = connector.getConnection().createChannel();
        channel.exchangeDeclare(getRequestExchangeName(), "direct", false, false, null);
        channel.exchangeDeclare(getResponseExchangeName(), "direct", false, false, null);

        bindQueues(channel);

        channelRef.set(channel, nextStamp = version.incrementAndGet());

        channel.addShutdownListener(new ShutdownListener() {

          @Override
          public void shutdownCompleted (ShutdownSignalException cause) {

            try {
              if (!closed.get()) {
                ensureChannel(nextStamp);
              }
            } catch (IOException ioException) {
              LoggerManager.getLogger(RabbitMQConnector.class).error(ioException);
            }
          }
        });

        installConsumer(channel);
      }
    }
  }

  public void send (String routingKey, String exchangeName, AMQP.BasicProperties properties, byte[] body)
    throws IOException {

    boolean sent = false;

    do {

      int[] stampHolder = new int[1];
      Channel channel = channelRef.get(stampHolder);

      try {
        channel.basicPublish(exchangeName, routingKey, true, false, properties, body);
        sent = true;
      } catch (AlreadyClosedException exception) {
        ensureChannel(stampHolder[0]);
      }
    } while (!sent);
  }

  public long getTimestamp (AMQP.BasicProperties properties) {

    Date date;

    if ((date = properties.getTimestamp()) != null) {

      return date.getTime();
    }

    return Long.MAX_VALUE;
  }

  public void close ()
    throws IOException {

    if (closed.compareAndSet(false, true)) {
      channelRef.getReference().close();
    }
  }
}
