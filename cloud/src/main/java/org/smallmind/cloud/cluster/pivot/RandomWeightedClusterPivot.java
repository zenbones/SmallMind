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
