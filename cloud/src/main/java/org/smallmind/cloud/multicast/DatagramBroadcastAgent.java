package org.smallmind.cloud.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import org.smallmind.cloud.multicast.event.EventMessage;
import org.smallmind.cloud.multicast.event.MessageStatus;

public class DatagramBroadcastAgent implements Runnable {

   private PacketBroadcaster packetBroadcaster;
   private MulticastSocket multicastSocket;
   private DatagramSocket datagramSocket;
   private InetAddress multicastInetAddress;
   private boolean finished = false;
   private boolean exited = false;
   private int multicastPort;
   private int messageBufferSize;

   public DatagramBroadcastAgent (PacketBroadcaster packetBroadcaster, DatagramSocket datagramSocket, MulticastSocket multicastSocket, InetAddress multicastInetAddress, int multicastPort, int messageSegmentSize) {

      this.packetBroadcaster = packetBroadcaster;
      this.datagramSocket = datagramSocket;
      this.multicastSocket = multicastSocket;
      this.multicastInetAddress = multicastInetAddress;
      this.multicastPort = multicastPort;

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
      byte[] messageBuffer = new byte[messageBufferSize];
      boolean packetReceived;

      translationBuffer = ByteBuffer.wrap(messageBuffer);
      messagePacket = new DatagramPacket(messageBuffer, messageBuffer.length);

      while (!finished) {
         try {
            try {
               datagramSocket.receive(messagePacket);
               packetReceived = true;
            }
            catch (SocketTimeoutException s) {
               packetReceived = false;
            }

            if (packetReceived) {
               translationBuffer.putInt(0, MessageStatus.BROADCAST.ordinal());
               messagePacket.setPort(multicastPort);
               messagePacket.setAddress(multicastInetAddress);
               multicastSocket.send(messagePacket);
            }
         }
         catch (Exception e) {
            packetBroadcaster.logError(e);
         }
      }

      exited = true;
   }

}
