/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.cloud.cluster;

import java.io.Serializable;
import org.smallmind.cloud.cluster.pivot.ClusterPivot;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public class ClusterInterface<D extends ClusterProtocolDetails> implements Serializable {

   private String clusterName;
   private ClusterPivot clusterPivot;
   private D clusterProtocolDetails;

   public ClusterInterface (String clusterName, ClusterPivot clusterPivot, D clusterProtocolDetails) {

      this.clusterName = clusterName;
      this.clusterPivot = clusterPivot;
      this.clusterProtocolDetails = clusterProtocolDetails;
   }

   public String getClusterName () {

      return clusterName;
   }

   public ClusterPivot getClusterPivot () {

      return clusterPivot;
   }

   public D getClusterProtocolDetails () {

      return clusterProtocolDetails;
   }

   public String toString () {

      StringBuilder interfaceBuilder;

      interfaceBuilder = new StringBuilder("ClusterInterface [");
      interfaceBuilder.append(clusterName);
      interfaceBuilder.append(':');
      interfaceBuilder.append(clusterProtocolDetails.getClusterProtocol());
      interfaceBuilder.append("]");

      return interfaceBuilder.toString();
   }

   public int hashCode () {

      return (clusterName.hashCode() ^ clusterProtocolDetails.getClass().hashCode());
   }

   public boolean equals (Object obj) {

      if (obj instanceof ClusterInterface) {
         if (clusterName.equals(((ClusterInterface)obj).getClusterName())) {
            if (clusterProtocolDetails.getClusterProtocol().equals(((ClusterInterface)obj).getClusterProtocolDetails().getClusterProtocol())) {
               return true;
            }
         }
      }

      return false;
   }
}
