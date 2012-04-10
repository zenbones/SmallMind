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
package org.smallmind.cloud.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import org.smallmind.cloud.multicast.event.EventMessage;
import org.smallmind.cloud.multicast.event.MessageStatus;

public class DatagramBroadcastAgent implements Runnable {

  private CountDownLatch exitLatch;
  private PacketBroadcaster packetBroadcaster;
  private MulticastSocket multicastSocket;
  private DatagramSocket datagramSocket;
  private InetAddress multicastInetAddress;
  private boolean finished = false;
  private int multicastPort;
  private int messageBufferSize;

  public DatagramBroadcastAgent (PacketBroadcaster packetBroadcaster, DatagramSocket datagramSocket, MulticastSocket multicastSocket, InetAddress multicastInetAddress, int multicastPort, int messageSegmentSize) {

    this.packetBroadcaster = packetBroadcaster;
    this.datagramSocket = datagramSocket;
    this.multicastSocket = multicastSocket;
    this.multicastInetAddress = multicastInetAddress;
    this.multicastPort = multicastPort;

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
    byte[] messageBuffer = new byte[messageBufferSize];
    boolean packetReceived;

    translationBuffer = ByteBuffer.wrap(messageBuffer);
    messagePacket = new DatagramPacket(messageBuffer, messageBuffer.length);

    try {
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
    }
    finally {
      exitLatch.countDown();
    }
  }
}
