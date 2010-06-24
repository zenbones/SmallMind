package org.smallmind.cloud.cluster.protocol.socket;

import org.smallmind.cloud.cluster.ClusterManagementException;

public interface PortMapper {

   public abstract int mapPort (String instanceId)
      throws ClusterManagementException;
}
