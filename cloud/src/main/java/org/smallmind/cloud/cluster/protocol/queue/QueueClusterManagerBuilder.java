/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.cloud.cluster.protocol.queue;

import java.util.HashMap;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManagerBuilder;
import org.smallmind.quorum.transport.message.MessageTransmitter;

public class QueueClusterManagerBuilder implements ClusterManagerBuilder<QueueClusterProtocolDetails> {

  private HashMap<String, QueueClusterManager> managerMap;

  public QueueClusterManagerBuilder () {

    managerMap = new HashMap<String, QueueClusterManager>();
  }

  public QueueClusterManager getClusterManager (ClusterHub clusterHub, ClusterInterface<QueueClusterProtocolDetails> clusterInterface)
    throws ClusterManagementException {

    QueueClusterManager clusterManager;
    MessageTransmitter messagingTransmitter;

    try {
      messagingTransmitter = new MessageTransmitter(clusterInterface.getClusterProtocolDetails().getConnectionDetails(), clusterInterface.getClusterProtocolDetails().getTransmissionPoolSize());
    }
    catch (Exception exception) {
      throw new ClusterManagementException(exception);
    }

    if ((clusterManager = managerMap.get(clusterInterface.getClusterName())) == null) {
      clusterManager = new QueueClusterManager(clusterHub, messagingTransmitter, clusterInterface);
      managerMap.put(clusterInterface.getClusterName(), clusterManager);
    }

    return clusterManager;
  }
}
