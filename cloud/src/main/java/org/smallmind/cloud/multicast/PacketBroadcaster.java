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
      try {
        multicastAgent.finish();
      }
      catch (InterruptedException interruptedException) {
        logger.error(interruptedException);
      }

      try {
        datagramAgent.finish();
      }
      catch (InterruptedException interruptedException) {
        logger.error(interruptedException);
      }

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
