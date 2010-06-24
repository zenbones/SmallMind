package org.smallmind.cloud.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import org.smallmind.cloud.multicast.event.EventMessage;
import org.smallmind.cloud.multicast.event.MessageStatus;

public class MulticastBroadcastAgent implements Runnable {

   private PacketBroadcaster packetBroadcaster;
   private MulticastSocket multicastSocket;
   private DatagramSocket datagramSocket;
   private InetAddress[] broadcastInetAddresses;
   private boolean finished = false;
   private boolean exited = false;
   private int broadcastPort;
   private int messageBufferSize;

   public MulticastBroadcastAgent (PacketBroadcaster packetBroadcaster, DatagramSocket datagramSocket, MulticastSocket multicastSocket, InetAddress[] broadcastInetAddresses, int broadcastPort, int messageSegmentSize) {

      this.packetBroadcaster = packetBroadcaster;
      this.datagramSocket = datagramSocket;
      this.multicastSocket = multicastSocket;
      this.broadcastInetAddresses = broadcastInetAddresses;
      this.broadcastPort = broadcastPort;

      messageBufferSize = messageSegmentSize + EventMessage.MESSAGE_HEADER_SIZE;
   }

   public synchronized void finish () {

      if (!finished) {
         finished = true;

         while (!exited) {
            try {
               Thread.sleep(100);
            }
            catch (InterruptedException interruptedException) {
               break;
            }
         }
      }
   }

   public void run () {

      DatagramPacket messagePacket;
      ByteBuffer translationBuffer;
      MessageStatus messageStatus;
      boolean packetReceived;
      byte[] messageBuffer = new byte[messageBufferSize];

      translationBuffer = ByteBuffer.wrap(messageBuffer);
      messagePacket = new DatagramPacket(messageBuffer, messageBuffer.length);

      while (!finished) {
         try {
            try {
               multicastSocket.receive(messagePacket);
               packetReceived = true;
            }
            catch (SocketTimeoutException s) {
               packetReceived = false;
            }

            if (packetReceived) {
               messageStatus = MessageStatus.getMessageStatus(translationBuffer.getInt(0));
               if (!messageStatus.equals(MessageStatus.BROADCAST)) {
                  messagePacket.setPort(broadcastPort);
                  for (InetAddress broadcastAddress : broadcastInetAddresses) {
                     messagePacket.setAddress(broadcastAddress);
                     datagramSocket.send(messagePacket);
                  }
               }
            }
         }
         catch (Exception e) {
            packetBroadcaster.logError(e);
         }
      }

      exited = true;
   }

}
