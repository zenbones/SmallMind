package org.smallmind.cloud.cluster.protocol.queue;

import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterServiceBuilder;

public class QueueClusterServiceBuilder implements ClusterServiceBuilder<QueueClusterProtocolDetails> {

   public QueueClusterService getClusterService (ClusterHub clusterHub, ClusterInstance<QueueClusterProtocolDetails> clusterInstance) {

      return new QueueClusterService(clusterHub, clusterInstance);
   }
}
