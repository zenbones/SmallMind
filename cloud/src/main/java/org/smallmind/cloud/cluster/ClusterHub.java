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

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.cloud.cluster.broadcast.ClusterBroadcast;
import org.smallmind.cloud.cluster.broadcast.GossipClusterBroadcast;
import org.smallmind.cloud.cluster.broadcast.NodeOfflineClusterBroadcast;
import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.cluster.broadcast.UpdateRequestClusterBroadcast;
import org.smallmind.cloud.cluster.broadcast.UpdateResponseClusterBroadcast;
import org.smallmind.cloud.cluster.event.GossipClusterListener;
import org.smallmind.cloud.cluster.meter.CapacityMeter;
import org.smallmind.cloud.multicast.EventMessageException;
import org.smallmind.cloud.multicast.event.EventTransmitter;
import org.smallmind.cloud.multicast.event.MulticastEvent;
import org.smallmind.cloud.multicast.event.MulticastEventHandler;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.quorum.cache.CacheException;
import org.smallmind.scribe.pen.Logger;

public class ClusterHub implements MulticastEventHandler {

   private static final int PROPAGATION_LATENCY = 3000;

   private final HashMap<ClusterInterface, WeakEventListenerList<GossipClusterListener>> listenerMap;
   private final HashMap<ClusterInterface, ClusterManager> managerMap;
   private final HashMap<ClusterInstance, ClusterService> clientMap;

   private Logger logger;
   private CapacityMeter capacityMeter;
   private EventTransmitter eventTransmitter;
   private ClusterUpdateTimer clusterUpdateTimer;

   public ClusterHub (Logger logger, CapacityMeter capacityMeter, String multicastIP, int multicastPort, int messageSegmentSize, int updateInterval)
      throws IOException, CacheException {

      Thread updateTimerThread;
      InetAddress multicastInetAddr;

      this.logger = logger;
      this.capacityMeter = capacityMeter;

      managerMap = new HashMap<ClusterInterface, ClusterManager>();
      clientMap = new HashMap<ClusterInstance, ClusterService>();
      listenerMap = new HashMap<ClusterInterface, WeakEventListenerList<GossipClusterListener>>();

      multicastInetAddr = InetAddress.getByName(multicastIP);
      eventTransmitter = new EventTransmitter(this, logger, multicastInetAddr, multicastPort, messageSegmentSize);

      clusterUpdateTimer = new ClusterUpdateTimer(this, updateInterval);
      updateTimerThread = new Thread(clusterUpdateTimer);
      updateTimerThread.setDaemon(true);
      updateTimerThread.start();
   }

   public void addGossipClusterListener (ClusterInterface clusterInterface, GossipClusterListener gossipClusterListener) {

      WeakEventListenerList<GossipClusterListener> listenerList;

      synchronized (listenerMap) {
         if ((listenerList = listenerMap.get(clusterInterface)) == null) {
            listenerList = new WeakEventListenerList<GossipClusterListener>();
            listenerMap.put(clusterInterface, listenerList);
         }
      }

      synchronized (listenerList) {
         listenerList.addListener(gossipClusterListener);
      }
   }

   public void removeGossipClusterListener (ClusterInterface clusterInterface, GossipClusterListener gossipClusterListener) {

      WeakEventListenerList<GossipClusterListener> listenerList;

      synchronized (listenerMap) {
         if ((listenerList = listenerMap.get(clusterInterface)) != null) {
            synchronized (listenerList) {
               listenerList.removeListener(gossipClusterListener);
            }
         }
      }
   }

   public ClusterManager getClusterManager (ClusterInterface clusterInterface) {

      synchronized (managerMap) {
         return managerMap.get(clusterInterface);
      }
   }

   public void addClusterManager (ClusterManager clusterManager)
      throws ClusterManagementException {

      synchronized (managerMap) {
         managerMap.put(clusterManager.getClusterInterface(), clusterManager);
      }

      requestStatusUpdate(clusterManager.getClusterInterface());

      try {
         Thread.sleep(PROPAGATION_LATENCY);
      }
      catch (InterruptedException interruptdException) {
         throw new ClusterManagementException(interruptdException);
      }
   }

   public void addClusterService (ClusterService clusterService) {

      ClusterInstance clusterInstance;

      clusterInstance = clusterService.getClusterInstance();

      synchronized (clientMap) {
         clientMap.put(clusterInstance, clusterService);
      }

      fireStatusUpdate(new ClusterInstance[] {clusterInstance});
   }

   public void removeClusterService (ClusterInstance clusterInstance) {

      synchronized (clientMap) {
         clientMap.remove(clusterInstance);
      }

      fireClusterNodeOffline(clusterInstance);
   }

   public ClusterInstance[] getClientClusterInstances () {

      ClusterInstance[] clusterInstances;

      synchronized (clientMap) {
         clusterInstances = new ClusterInstance[clientMap.keySet().size()];
         clientMap.keySet().toArray(clusterInstances);
      }

      return clusterInstances;
   }

   public ClusterInstance[] getClientClusterInstances (ClusterInterface clusterInterface) {

      ClusterInstance[] clusterInstances;
      Iterator<ClusterInstance> clusterInstanceIter;
      LinkedList<ClusterInstance> clusterInstanceList;
      ClusterInstance clusterInstance;

      clusterInstanceList = new LinkedList<ClusterInstance>();
      synchronized (clientMap) {
         clusterInstanceIter = clientMap.keySet().iterator();
         while (clusterInstanceIter.hasNext()) {
            clusterInstance = clusterInstanceIter.next();
            if (clusterInstance.getClusterInterface().equals(clusterInterface)) {
               clusterInstanceList.add(clusterInstance);
            }
         }
      }

      clusterInstances = new ClusterInstance[clusterInstanceList.size()];
      clusterInstanceList.toArray(clusterInstances);
      return clusterInstances;
   }

   public boolean hasServiceCluster (ClusterInterface clusterInterface) {

      Iterator<ClusterInstance> clusterInstanceIter;

      synchronized (clientMap) {
         clusterInstanceIter = clientMap.keySet().iterator();
         while (clusterInstanceIter.hasNext()) {
            if ((clusterInstanceIter.next()).getClusterInterface().equals(clusterInterface)) {
               return true;
            }
         }
      }

      return false;
   }

   private void requestStatusUpdate (ClusterInterface clusterInterface) {

      try {
         fireEvent(new UpdateRequestClusterBroadcast(clusterInterface));
      }
      catch (Exception e) {
         logError(e);
      }
   }

   protected void fireStatusUpdate (ClusterInstance[] clusterInstances) {

      int calibratedFreeCapacity;

      if (clusterInstances.length > 0) {
         calibratedFreeCapacity = capacityMeter.getCalibratedFreeCapacity();
         try {
            fireEvent(new UpdateResponseClusterBroadcast(clusterInstances, calibratedFreeCapacity));
         }
         catch (Exception e) {
            logError(e);
         }
      }
   }

   protected void fireClusterNodeOffline (ClusterInstance clusterInstance) {

      try {
         fireEvent(new NodeOfflineClusterBroadcast(clusterInstance));
      }
      catch (Exception e) {
         logError(e);
      }
   }

   public void fireEvent (MulticastEvent multicastEvent)
      throws EventMessageException {

      eventTransmitter.fireEvent(multicastEvent);
   }

   public void deliverEvent (MulticastEvent multicastEvent) {

      if (multicastEvent instanceof ClusterBroadcast) {
         switch (((ClusterBroadcast)multicastEvent).getClusterBroadcastType()) {
            case SYSTEM:
               if (multicastEvent instanceof UpdateRequestClusterBroadcast) {
                  fireStatusUpdate(getClientClusterInstances(((UpdateRequestClusterBroadcast)multicastEvent).getClusterInterface()));
               }
               else if (multicastEvent instanceof UpdateResponseClusterBroadcast) {

                  ClusterManager clusterManager;
                  ClusterInstance[] clusterInstances;
                  int calibratedFreeCapacity;

                  synchronized (managerMap) {
                     calibratedFreeCapacity = ((UpdateResponseClusterBroadcast)multicastEvent).getCalibratedFreeCapacity();

                     clusterInstances = ((UpdateResponseClusterBroadcast)multicastEvent).getClusterInstances();
                     for (ClusterInstance clusterInstance : clusterInstances) {
                        if ((clusterManager = managerMap.get(clusterInstance.getClusterInterface())) != null) {
                           try {
                              clusterManager.updateClusterStatus(new ClusterEndpoint(multicastEvent.getHostAddress(), clusterInstance), calibratedFreeCapacity);
                           }
                           catch (Exception e) {
                              logError(e);
                           }
                        }
                     }
                  }
               }
               else if (multicastEvent instanceof NodeOfflineClusterBroadcast) {

                  ClusterManager clusterManager;
                  ClusterInstance clusterInstance;

                  synchronized (managerMap) {
                     clusterInstance = ((NodeOfflineClusterBroadcast)multicastEvent).getClusterInstance();
                     if ((clusterManager = managerMap.get(clusterInstance.getClusterInterface())) != null) {
                        try {
                           clusterManager.removeClusterMember(new ClusterEndpoint(multicastEvent.getHostAddress(), clusterInstance));
                        }
                        catch (Exception e) {
                           logError(e);
                        }
                     }
                  }
               }

               break;
            case SERVICE:

               ClusterInstance[] clusterInstances;

               synchronized (clientMap) {
                  clusterInstances = getClientClusterInstances(((ServiceClusterBroadcast)multicastEvent).getClusterInterface());
                  for (ClusterInstance clusterInstance : clusterInstances) {
                     spinThread(new ClusterHubBroadcastDelivery(clientMap.get(clusterInstance), (ServiceClusterBroadcast)multicastEvent));
                  }
               }

               break;
            case GOSSIP:

               WeakEventListenerList<GossipClusterListener> listenerList;
               Iterator<GossipClusterListener> listenerIter;

               synchronized (listenerMap) {
                  listenerList = listenerMap.get(((GossipClusterBroadcast)multicastEvent).getClusterInsterface());
               }

               if (listenerList != null) {
                  synchronized (listenerList) {
                     if (listenerList != null) {
                        listenerIter = listenerList.getListeners();
                        while (listenerIter.hasNext()) {
                           spinThread(new ClusterHubGossipDelivery(listenerIter.next(), ((GossipClusterBroadcast)multicastEvent).getGossipClusterEvent()));
                        }
                     }
                  }
               }

               break;
            default:
               logError("Unkown cluster broadacst type (" + ((ClusterBroadcast)multicastEvent).getClusterBroadcastType().name() + ")");
         }
      }
   }

   private void spinThread (Runnable runnable) {

      Thread thread;

      thread = new Thread(runnable);
      thread.start();
   }

   public void finalize () {

      try {
         clusterUpdateTimer.finish();
      }
      catch (InterruptedException interruptedException) {
         logError(interruptedException);
      }

      eventTransmitter.finish();
   }

   public void logError (String message) {

      logger.error(message);
   }

   public void logError (Throwable throwable) {

      logger.error(throwable);
   }

}
