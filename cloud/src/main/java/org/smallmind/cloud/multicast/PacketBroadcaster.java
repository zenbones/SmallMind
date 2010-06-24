package org.smallmind.cloud.multicast;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedList;
import org.smallmind.scribe.pen.Logger;

public class PacketBroadcaster {

   private static final int SO_TIMEOUT = 1000;
   private static final int TTL = 128;

   private Logger logger;
   private DatagramSocket datagramSocket;
   private MulticastSocket multicastSocket;
   private DatagramBroadcastAgent datagramAgent;
   private MulticastBroadcastAgent multicastAgent;
   private InetAddress multicastInetAddress;
   private boolean finished = false;

   public PacketBroadcaster (Logger logger, String multicastIP, int multicastPort, String[] broadcastHosts, int broadcastPort, int messageSegmentSize)
      throws IOException {

      Thread multicastAgentThread;
      Thread datagramAgentThread;
      LinkedList<InetAddress> broadcastAddressList;
      InetAddress[] broadcastInetAddresses;

      this.logger = logger;

      broadcastAddressList = new LinkedList<InetAddress>();
      for (String broadcastHost : broadcastHosts) {
         broadcastAddressList.add(InetAddress.getByName(broadcastHost));
      }

      broadcastAddressList.remove(InetAddress.getLocalHost());
      broadcastInetAddresses = new InetAddress[broadcastAddressList.size()];
      broadcastAddressList.toArray(broadcastInetAddresses);

      multicastInetAddress = InetAddress.getByName(multicastIP);

      datagramSocket = new DatagramSocket(broadcastPort);
      datagramSocket.setReuseAddress(true);
      datagramSocket.setSoTimeout(SO_TIMEOUT);

      multicastSocket = new MulticastSocket(multicastPort);
      multicastSocket.setReuseAddress(true);
      multicastSocket.setSoTimeout(SO_TIMEOUT);
      multicastSocket.setTimeToLive(TTL);
      multicastSocket.joinGroup(multicastInetAddress);

      datagramAgent = new DatagramBroadcastAgent(this, datagramSocket, multicastSocket, multicastInetAddress, multicastPort, messageSegmentSize);
      multicastAgent = new MulticastBroadcastAgent(this, datagramSocket, multicastSocket, broadcastInetAddresses, broadcastPort, messageSegmentSize);

      datagramAgentThread = new Thread(datagramAgent);
      multicastAgentThread = new Thread(multicastAgent);

      datagramAgentThread.start();
      multicastAgentThread.start();
   }

   public String getComponentType () {

      return "multicast";
   }

   public void logError (Throwable throwable) {

      logger.error(throwable);
   }

   public synchronized void finish () {

      if (!finished) {
         multicastAgent.finish();
         datagramAgent.finish();

         try {
            multicastSocket.leaveGroup(multicastInetAddress);
            multicastSocket.close();
            datagramSocket.close();
         }
         catch (IOException ioException) {
            logError(ioException);
         }
      }
   }

   public void finalize () {

      finish();
   }
}
