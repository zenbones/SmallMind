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

public class KafkaConfiguration {

  // Should go in a config file (config map for k8s)
  /*

  // acks=all
  num.partitions=24 // as few as 1 might be fine, consumer threads * broker nodes, at a guess (24 is still small)
  default.replication.factor=3 // default for number of replicas
  min.insync.replicas=2 // given 3 nodes - min.insync.replicas=2, acks=all, replication.factor=3 for n/2+1

  // acks=0
  num.partitions=24 // as few as 1 might be fine, consumer threads * broker nodes, at a guess (24 is still small)
  default.replication.factor=1 // default for number of replicas
  min.insync.replicas=1 // min.insync.replicas=1, acks=0, replication.factor=1 for fast and un-replicated

  // the best of the rest
  auto.create.topics.enable=true // must have
  auto.leader.rebalance.enable=true // allow re-balances
  compression.type=lz4 // none, gzip, lz4, snappy, and zstd (prefer lz4 as fastest if not smallest)
  delete.topic.enable=true // seems like we should
  group.initial.rebalance.delay.ms=3000 // default 3000 (wait for consumers to join before first re-balance)
  leader.imbalance.check.interval.seconds=300 // how often to run the re-balance check
  leader.imbalance.per.broker.percentage=10 // The allowed percentage of partitions for which the broker is not the preferred leader before a re-balance occurs
  log.cleaner.backoff.ms=15000 // general consensus
  log.cleaner.delete.retention.ms=86400000 // (24 hours) the retention time for deleted tombstone markers (as we do not use keys this should make no difference)
  log.cleaner.enable=true // we want this
  log.cleanup.policy=delete // if we were using keys we might use 'compact,delete'
  log.retention.check.interval.ms=300000 // (5 minutes) should be lower than log.retention.ms
  log.retention.ms=1680000 // ttl for messages (28 minutes)
  log.segment.delete.delay.ms=60000 // maybe not necessary but not harmful
  message.max.bytes=1048588 // default 1048588 (1mb, but slightly more than replica.fetch.max.bytes)
  num.io.threads=8 // default 8
  num.network.threads=3 // default 3
  num.recovery.threads.per.data.dir=1 // general consensus
  queued.max.requests=500 // default 500 (limit the size of the request queue before the network thread is blocked)
  replica.fetch.max.bytes=1048576 // default (1mb, but slightly less than message.max.bytes)
  replica.lag.time.max.ms=10000 // default 30000 (upper limit on how long a producer must wait for acknowledgement, but also how quickly slow queues are removed)
  retention.ms=300000 // a message not delivered for 5 minutes should eb
  unclean.leader.election.enable=true // defaults to false, but if there's no in-sync follower when a leader fails, then no leader can be elected

  // no need to change
  group.min.session.timeout.ms=6000 // default 6000
  group.max.session.timeout.ms=1800000 // defualt 1800000

  // if we want to be rack sensitive
  rack.id - must be set to the data centre ID (ex: AZ ID in AWS)
  replica.selector.class - must be set to org.apache.kafka.common.replica.RackAwareReplicaSelector

  // in general, do not override
  transaction.state.log.replication.factor=3
  transaction.state.log.min.isr=2
  offsets.topic.num.partitions=50
  offsets.topic.replication.factor=3
  offsets.commit.required.acks=-1
  */
}
