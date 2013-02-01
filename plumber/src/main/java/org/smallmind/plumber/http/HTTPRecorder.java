/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.plumber.http;

import java.io.IOException;
import java.net.ServerSocket;
import org.smallmind.nutsnbolts.command.CommandException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.CommandSet;
import org.smallmind.plumber.io.ServerSocketHerald;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

public class HTTPRecorder {

  private static final String[] REQUIRED_ARGUMENTS = {"localport", "remotehost", "remoteport", "buffer", "log"};

  private Logger logger;
  private String remoteHost;
  private int localPort;
  private int remotePort;
  private int buffer;

  public HTTPRecorder (int localPort, String remoteHost, int remotePort, int buffer, Logger logger) {

    this.localPort = localPort;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    this.buffer = buffer;
    this.logger = logger;
  }

  public void record ()
    throws IOException {

    ServerSocket serverSocket;
    ServerSocketHerald herald;
    Thread heraldThread;

    serverSocket = new ServerSocket(localPort);
    serverSocket.setReuseAddress(true);

    herald = new ServerSocketHerald(logger, new RecordingSocketWorkerFactory(remoteHost, remotePort, buffer), serverSocket, ServerSocketHerald.NO_THROTTLE, 8);
    heraldThread = new Thread(herald);
    heraldThread.start();

    try {
      Thread.sleep(300000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void main (String[] args)
    throws CommandException, IOException {

    HTTPRecorder HTTPRecorder;
    CommandSet commandSet;
    int localPort;
    int remotePort;
    int buffer;

    commandSet = CommandLineParser.parseCommands(args);
    if (!commandSet.containsAllCommands(REQUIRED_ARGUMENTS)) {
      System.out.println("HTTPRecorder [localport] [remotehost] [remoteport] [buffer] [log]");
    }
    else {
      localPort = Integer.parseInt(commandSet.getArgument("localport"));
      remotePort = Integer.parseInt(commandSet.getArgument("remoteport"));
      buffer = Integer.parseInt(commandSet.getArgument("buffer"));
      HTTPRecorder = new HTTPRecorder(localPort, commandSet.getArgument("remotehost"), remotePort, buffer, LoggerManager.getLogger(commandSet.getArgument("log")));
      HTTPRecorder.record();
    }
  }

}
