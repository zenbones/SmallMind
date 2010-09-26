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
package org.smallmind.cloud.cluster.pivot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.smallmind.cloud.cluster.ClusterEndpoint;

public class RandomWeightedClusterPivot implements ClusterPivot {

   private static final Random random = new Random();

   private final HashMap<ClusterEndpoint, Integer> endpointMap;

   private ClusterEndpoint[] endpointArray;
   private int[] sumArray;
   private int sumTotal;

   public RandomWeightedClusterPivot () {

      endpointMap = new HashMap<ClusterEndpoint, Integer>();
      endpointArray = new ClusterEndpoint[0];
      sumArray = new int[0];
      sumTotal = 0;
   }

   public void updateClusterStatus (ClusterEndpoint clusterEndpoint, int freeCapacity) {

      synchronized (endpointMap) {
         endpointMap.put(clusterEndpoint, freeCapacity);
      }
      createDistribution();
   }

   public void removeClusterMember (ClusterEndpoint clusterEndpoint) {

      synchronized (endpointMap) {
         endpointMap.remove(clusterEndpoint);
      }
      createDistribution();
   }

   public ClusterEndpoint nextRequestAddress (Object[] parameters, ClusterEndpoint failedEndpoint) {

      int endpointIndex;
      int randomIndex;

      synchronized (this) {
         if (endpointArray.length == 0) {
            return null;
         }

         if (sumTotal == 0) {
            endpointIndex = random.nextInt(endpointArray.length);
         }
         else {
            randomIndex = random.nextInt(sumTotal) + 1;
            endpointIndex = Arrays.binarySearch(sumArray, randomIndex);
            if (endpointIndex < 0) {
               endpointIndex = Math.abs(endpointIndex) - 1;
            }
         }

         return endpointArray[endpointIndex];
      }
   }

   private void createDistribution () {

      ClusterEndpoint[] tempEndpointArray;
      ClusterEndpoint endpointKey;
      Iterator<ClusterEndpoint> endpointIter;
      int[] tempSumArray;
      int freeCapacity;
      int tempSumTotal = 0;
      int index = 0;

      synchronized (endpointMap) {
         tempEndpointArray = new ClusterEndpoint[endpointMap.size()];
         tempSumArray = new int[endpointMap.size()];

         endpointIter = endpointMap.keySet().iterator();
         while (endpointIter.hasNext()) {
            endpointKey = endpointIter.next();
            freeCapacity = endpointMap.get(endpointKey);
            tempEndpointArray[index] = endpointKey;
            tempSumTotal += freeCapacity;
            tempSumArray[index] = tempSumTotal;
            index++;
         }
      }

      synchronized (this) {
         endpointArray = tempEndpointArray;
         sumArray = tempSumArray;
         sumTotal = tempSumTotal;
      }
   }

}
