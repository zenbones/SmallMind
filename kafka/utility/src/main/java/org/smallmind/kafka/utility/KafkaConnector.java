/*
 * Copyright (c) 2007 through 2024 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.kafka.utility;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.smallmind.scribe.pen.LoggerManager;

public class KafkaConnector {

  private final String boostrapServers;

  public KafkaConnector (int startupGracePeriodSeconds, KafkaServer... servers)
    throws KafkaConnectionException {

    StringBuilder boostrapBuilder = new StringBuilder();
    long startTimestamp;
    boolean first = true;

    for (KafkaServer server : servers) {
      if (!first) {
        boostrapBuilder.append(',');
      }

      boostrapBuilder.append(server.getHost()).append(':').append(server.getPort());
      first = false;
    }

    boostrapServers = boostrapBuilder.toString();

    startTimestamp = System.currentTimeMillis();
    if (!invokeAdminClient(adminClient -> {
        while (true) {
          try {
            Collection<Node> nodes = adminClient.describeCluster().nodes().get();

            return (nodes != null) && (!nodes.isEmpty());
          } catch (ExecutionException | InterruptedException exception) {
            if ((System.currentTimeMillis() - startTimestamp) < (startupGracePeriodSeconds * 1000L)) {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException interruptedException) {
                LoggerManager.getLogger(KafkaConnector.class).error(interruptedException);

                return false;
              }
            } else {
              LoggerManager.getLogger(KafkaConnector.class).error(exception);

              return false;
            }
          }
        }
      }
    )) {
      throw new KafkaConnectionException("Unable to prove kafka nodes are available with boostrap servers(%s)...", boostrapServers);
    }
  }

  public String getBoostrapServers () {

    return boostrapServers;
  }

  public <R> R invokeAdminClient (Function<AdminClient, R> clientFunction) {

    Properties props = new Properties();

    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);
    props.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 300000);
    props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
    // Must be less than or equal to request.timeout.ms
    props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);
    props.put(AdminClientConfig.RETRIES_CONFIG, 0);
    props.put(AdminClientConfig.CLIENT_ID_CONFIG, "");

    try (AdminClient client = AdminClient.create(props)) {

      return clientFunction.apply(client);
    }
  }

  public Producer<Long, byte[]> createProducer (String clientId) {

    Properties props = new Properties();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

    props.put(ProducerConfig.ACKS_CONFIG, "0");
    // props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    // props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
    // Must be less than or equal to  max.in.flight.requests.per.connection
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 500);  // > REQUEST_TIMEOUT_MS_CONFIG + LINGER_MS_CONFIG
    props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 500); // > replica.lag.time.max.ms
    props.put(ProducerConfig.LINGER_MS_CONFIG, 0); // maybe 20ms or so, up to 500ms at the outside
    props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024); // bytes, default is 1mb = 1024 * 1024
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // bytes, default is 16kb = 16384
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000); // how long to block when the send buffer is full (and acks > 0?)
    // props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // how long to block when the send buffer is full (and acks > 0?)
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
    // props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    return new KafkaProducer<>(props);
  }

  public Consumer<Long, byte[]> createConsumer (String clientId, String groupId, String... topics) {

    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    // props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, instanceId);
    props.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "consumer");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

    // Can't be set when the group protocol is set to 'consumer'
    // props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
    // props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
    // Can't be set when the group protocol is set to 'consumer'
    // props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
    // props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000);
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
    props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 50 * 1024 * 1024);
    props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 500);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    // props.put(ConsumerConfig.CLIENT_RACK_CONFIG, "<must be set to the data centre ID (ex: AZ ID in AWS)>");

    // Create the consumer using props.
    final Consumer<Long, byte[]> consumer = new KafkaConsumer<>(props);

    // Subscribe to the topic.
    consumer.subscribe(Arrays.asList(topics));

    return consumer;
  }
}
