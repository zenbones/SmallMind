/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.cometd.oumuamua;

import java.util.Collection;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.smallmind.cometd.oumuamua.backbone.kafka.KafkaConnector;
import org.smallmind.cometd.oumuamua.backbone.kafka.KafkaServer;

public class KafkaProducerTest {

  /*
      image: 'bitnami/kafka:latest'
    ports:
      - '9092:9092'
    environment:
      - ALLOW_PLAINTEXT_LISTENER=yes
   */

  /*
  min.insync.replicas - 1/2 given 3 nodes (min.insync.replicas=2, acks=all, replication.factor=3 for n/2-1)
  group.min.session.timeout.ms - min session time a consumer can ask for
  group.max.session.timeout.ms - max session time a consumer can ask for
  compression.type - none, gzip, lz4, snappy, and zstd (prefer lz4 as fastest if not smallest)
  rack.id - must be set to the data centre ID (ex: AZ ID in AWS)
  replica.selector.class - must be set to org.apache.kafka.common.replica.RackAwareReplicaSelector
  log.retention.ms - ttl for messages
  */

  public static void main (String... args)
    throws Exception {

    AdminClient client;

    Properties props = new Properties();
    props.put("bootstrap.servers", "127.0.0.1:9094");
    props.put("connections.max.idle.ms", 300000);
    props.put("request.timeout.ms", 1500);
    props.put("default.api.timeout.ms", 1500);
    props.put("retries", 0);
    props.put("client.id", "");

    client = AdminClient.create(props);

    Collection<Node> nodes = client.describeCluster().nodes().get();
    System.out.println(nodes != null && nodes.size() > 0);

    KafkaConnector connector = new KafkaConnector(new KafkaServer("localhost", 9094));
    Producer<Long, byte[]> producer = connector.createProducer("node-p2");

    for (int i = 0; i < 10; i++) {
      producer.send(new ProducerRecord<>("push", "one..two,,three".getBytes()));
    }

    Thread.sleep(3000);

    System.out.println("Done...");
  }
}
