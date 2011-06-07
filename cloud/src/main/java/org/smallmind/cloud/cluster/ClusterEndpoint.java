/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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

import java.net.InetAddress;

public class ClusterEndpoint implements java.io.Serializable {

   private InetAddress inetAddress;
   private ClusterInstance clusterInstance;

   public ClusterEndpoint (InetAddress inetAddress, ClusterInstance clusterInstance) {

      this.inetAddress = inetAddress;
      this.clusterInstance = clusterInstance;
   }

   public InetAddress getHostAddress () {

      return inetAddress;
   }

   public String getHostName () {

      return inetAddress.getHostName();
   }

   public ClusterInstance getClusterInstance () {

      return clusterInstance;
   }

   public String toString () {

      StringBuilder endpointBuilder;

      endpointBuilder = new StringBuilder("ClusterEndpoint [");
      endpointBuilder.append(inetAddress.toString());
      endpointBuilder.append(":");
      endpointBuilder.append(clusterInstance.toString());
      endpointBuilder.append("]");

      return endpointBuilder.toString();
   }

   public int hashCode () {

      return (inetAddress.hashCode() ^ clusterInstance.hashCode());
   }

   public boolean equals (Object obj) {

      if (obj instanceof ClusterEndpoint) {
         if (inetAddress.equals(((ClusterEndpoint)obj).getHostAddress())) {
            if (clusterInstance.equals(((ClusterEndpoint)obj).getClusterInstance())) {
               return true;
            }
         }
      }

      return false;
   }
}
