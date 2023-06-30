package org.smallmind.cometd.oumuamua.backbone.kafka;

public class KafkaConfiguration {

  /*
  min.insync.replicas - 1/2 given 3 nodes (min.insync.replicas=2, acks=all, replication.factor=3 for n/2-1)
  group.min.session.timeout.ms - min session time a consumer can ask for
  group.max.session.timeout.ms - max session time a consumer can ask for
  compression.type - none, gzip, lz4, snappy, and zstd (prefer lz4 as fastest if not smallest)
  rack.id - must be set to the data centre ID (ex: AZ ID in AWS)
  replica.selector.class - must be set to org.apache.kafka.common.replica.RackAwareReplicaSelector
  log.retention.ms - ttl for messages, should be set in minutes really, there's a hard drive underneath all this
  log.retention.check.interval.ms - should be lower than log.retention.ms, but again not too low
  */
}
