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
package org.smallmind.plumber.http;

import java.net.InetAddress;
import java.net.Socket;
import org.smallmind.plumber.io.ServerSocketHerald;
import org.smallmind.plumber.io.SocketWorker;
import org.smallmind.scribe.pen.Logger;

public class RecordingSocketWorker extends SocketWorker {

  private String httpHost;
  private int httpPort;
  private int bufferSize;

  public RecordingSocketWorker (Logger logger, ServerSocketHerald herald, String httpHost, int httpPort, int bufferSize) {

    super(logger, herald);

    this.httpHost = httpHost;
    this.httpPort = httpPort;
    this.bufferSize = bufferSize;
  }

  public void socketWork (Socket socket)
    throws Exception {

    Socket serviceSocket;
    RecordingSocketPipe socketPipe;

    serviceSocket = new Socket(InetAddress.getByName(httpHost), httpPort);

    socketPipe = new RecordingSocketPipe(socket, serviceSocket, bufferSize);
    socketPipe.startPipe();

    serviceSocket.close();
  }
}
