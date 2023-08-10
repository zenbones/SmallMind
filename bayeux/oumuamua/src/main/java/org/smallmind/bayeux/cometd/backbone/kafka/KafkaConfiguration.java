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
package org.smallmind.bayeux.cometd.backbone.kafka;

public class KafkaConfiguration {

  // Should go in a config file (config map for k8s)
  /*
  min.insync.replicas - 1/2 given 3 nodes (min.insync.replicas=2, acks=all, replication.factor=3 for n/2-1) (min.insync.replicas=1, acks=0, replication.factor=1 for fast and unreplicated)
  group.min.session.timeout.ms - min session time a consumer can ask for
  group.max.session.timeout.ms - max session time a consumer can ask for
  compression.type - none, gzip, lz4, snappy, and zstd (prefer lz4 as fastest if not smallest)
  rack.id - must be set to the data centre ID (ex: AZ ID in AWS)
  replica.selector.class - must be set to org.apache.kafka.common.replica.RackAwareReplicaSelector
  default.replication.factor - 1
  num.partitions - default number of partitions (defaults to 1, which may be fine given that each reader will be its own group, but might also be number of nodes)
  log.retention.ms - ttl for messages, should be set in minutes really, there's a hard drive underneath all this
  log.retention.check.interval.ms - should be lower than log.retention.ms, but again not too low (300000?)
  log.cleaner.backoff.ms - 15000
  log.cleaner.delete.retention.ms - 900000
  log.segment.delete.delay.ms - 60000
  log.cleaner.enable - true
  log.cleanup.policy - compact,delete (maybe just delete)
  num.network.threads - 3
  num.io.threads - 8
  num.recovery.threads.per.data.dir - 1
  queued.max.requests - limit the number of requests allowed in the request queue before the network thread is blocked.
  group.initial.rebalance.delay.ms - 0
  auto.create.topics.enable - true
  delete.topic.enable - true
  */
}
