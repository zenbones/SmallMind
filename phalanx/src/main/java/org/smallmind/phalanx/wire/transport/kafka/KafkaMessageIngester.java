package org.smallmind.phalanx.wire.transport.kafka;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.glassfish.jersey.internal.util.collection.Value;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.scribe.pen.LoggerManager;

public class KafkaMessageIngester {
/*
  private final ExecutorService executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final KafkaConnector connector;
  private final Producer<Long, byte[]> producer;
  private final String nodeName;
  private final String topicName;
  private final String groupId;
  private final int concurrencyLimit;
  private ConsumerWorker<V>[] workers;

  public KafkaMessageIngester (String nodeName, VocalMode vocalMode, String serviceGroup, String instanceId, int concurrencyLimit, int startupGracePeriodSeconds, KafkaServer... servers) {

    long startTimestamp = System.currentTimeMillis();

    this.nodeName = nodeName;
    this.concurrencyLimit = concurrencyLimit;


    connector = new KafkaConnector(servers);

    if (!connector.invokeAdminClient(adminClient -> {
        while (true) {
          try {
            Collection<Node> nodes = adminClient.describeCluster().nodes().get();

            return (nodes != null) && (!nodes.isEmpty());
          } catch (ExecutionException | InterruptedException exception) {
            if ((System.currentTimeMillis() - startTimestamp) < (startupGracePeriodSeconds * 1000L)) {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException interruptedException) {
                LoggerManager.getLogger(KafkaMessageIngester.class).error(interruptedException);

                return false;
              }
            } else {
              LoggerManager.getLogger(KafkaMessageIngester.class).error(exception);

              return false;
            }
          }
        }
      }
    )) {
      throw new OumuamuaException("Unable to start the kafka backbone service");
    }
  }

  private Consumer<Long, byte[]> createConsumer (int index) {

    return connector.createConsumer("oumuamua-consumer-" + index + "-" + topicName + "-" + nodeName, groupId, prefixedTopicName);
  }

  @Override
  public void startUp (Server<V> server)
    throws Exception {

    if (statusRef.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      workers = new ConsumerWorker[concurrencyLimit];

      for (int index = 0; index < concurrencyLimit; index++) {
        new Thread(workers[index] = new ConsumerWorker<V>(server, nodeName, index)).start();
      }
      statusRef.set(ComponentStatus.STARTED);
    } else {
      while (ComponentStatus.STARTING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  @Override
  public void shutDown ()
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      for (ConsumerWorker<V> worker : workers) {
        worker.stop();
      }
      statusRef.set(ComponentStatus.STOPPED);
    } else {
      while (ComponentStatus.STOPPING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  private class ConsumerWorker<V extends Value<V>> implements Runnable {

    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final Server<V> server;
    private final String nodeName;
    private final int index;
    private Consumer<Long, byte[]> consumer;

    public ConsumerWorker (Server<V> server, String nodeName, int index) {

      this.server = server;
      this.nodeName = nodeName;
      this.index = index;

      consumer = createConsumer(index);
    }

    private void stop ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
        consumer.wakeup();

        exitLatch.await();
      }
    }

    @Override
    public void run () {

      try {
        while (!finished.get()) {
          try {

            ConsumerRecords<Long, byte[]> records;

            if (((records = consumer.poll(Duration.ofSeconds(3))) != null) && (!records.isEmpty())) {
              for (TopicPartition partition : records.partitions()) {

                List<ConsumerRecord<Long, byte[]>> recordList;
                long lastOffset = 0;

                for (ConsumerRecord<Long, byte[]> record : recordList = records.records(partition)) {
                  try {

                    DebonedPacket<V> debonedPacket = RecordUtility.deserialize(server.getCodec(), record.value());

                    if (!nodeName.equals(debonedPacket.getNodeName())) {
                      server.deliver(null, debonedPacket.getPacket(), false);
                    }
                  } catch (Exception exception) {
                    LoggerManager.getLogger(KafkaBackbone.class).error(exception);
                  }

                  lastOffset = record.offset();
                }

                if (!recordList.isEmpty()) {
                  consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                }
              }
            }
          } catch (Exception exception) {
            LoggerManager.getLogger(KafkaBackbone.class).error(exception);

            try {
              consumer.close();
            } finally {
              consumer = createConsumer(index);
            }
          }
        }
      } catch (WakeupException wakeupException) {
        if (!finished.get()) {
          LoggerManager.getLogger(KafkaBackbone.class).error(wakeupException);
        }
      } finally {
        try {
          consumer.close();
        } finally {
          exitLatch.countDown();
        }
      }
    }
  }

 */
}
