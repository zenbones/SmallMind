/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import org.smallmind.quorum.multicast.event.EventMessage;
import org.smallmind.quorum.multicast.event.MessageStatus;

public class MulticastBroadcastAgent implements Runnable {

  private CountDownLatch exitLatch;
  private PacketBroadcaster packetBroadcaster;
  private MulticastSocket multicastSocket;
  private DatagramSocket datagramSocket;
  private InetAddress[] broadcastInetAddresses;
  private boolean finished = false;
  private int broadcastPort;
  private int messageBufferSize;

  public MulticastBroadcastAgent (PacketBroadcaster packetBroadcaster, DatagramSocket datagramSocket, MulticastSocket multicastSocket, InetAddress[] broadcastInetAddresses, int broadcastPort, int messageSegmentSize) {

    this.packetBroadcaster = packetBroadcaster;
    this.datagramSocket = datagramSocket;
    this.multicastSocket = multicastSocket;
    this.broadcastInetAddresses = broadcastInetAddresses;
    this.broadcastPort = broadcastPort;

    messageBufferSize = messageSegmentSize + EventMessage.MESSAGE_HEADER_SIZE;

    exitLatch = new CountDownLatch(1);
  }

  public synchronized void finish ()
    throws InterruptedException {

    finished = true;
    exitLatch.await();
  }

  public void run () {

    DatagramPacket messagePacket;
    ByteBuffer translationBuffer;
    MessageStatus messageStatus;
    boolean packetReceived;
    byte[] messageBuffer = new byte[messageBufferSize];

    translationBuffer = ByteBuffer.wrap(messageBuffer);
    messagePacket = new DatagramPacket(messageBuffer, messageBuffer.length);

    try {
      while (!finished) {
        try {
          try {
            multicastSocket.receive(messagePacket);
            packetReceived = true;
          } catch (SocketTimeoutException s) {
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
        } catch (Exception e) {
          packetBroadcaster.logError(e);
        }
      }
    } finally {
      exitLatch.countDown();
    }
  }
}
