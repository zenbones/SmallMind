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
package org.smallmind.cloud.multicast.wrapper;

import org.smallmind.cloud.multicast.PacketBroadcaster;
import org.smallmind.nutsnbolts.command.CommandException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.CommandSet;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public class PacketBroadcasterWrapperListener implements WrapperListener {

  private static final String[] REQUIRED_ARGUMENTS = {"multicastip", "multicastport", "broadcasthosts", "broadcastport", "messagesize"};

  private static final int NO_ERROR_CODE = 0;
  private static final int COMMAND_ERROR_CODE = 1;
  private static final int STACK_TRACE_ERROR_CODE = 2;

  private PacketBroadcaster packetBroadcaster;

  public void controlEvent (int event) {

    if (!WrapperManager.isControlledByNativeWrapper()) {
      if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
        WrapperManager.stop(0);
      }
    }
  }

  public Integer start (String[] args) {

    CommandSet commandSet;
    Logger logger;
    String[] broadcastHosts;
    String multicastIP;
    int multicastPort;
    int broadcastPort;
    int messagesize;

    try {
      commandSet = CommandLineParser.parseCommands(args);
    }
    catch (CommandException commandException) {
      commandException.printStackTrace();
      return COMMAND_ERROR_CODE;
    }

    if (!commandSet.containsAllCommands(REQUIRED_ARGUMENTS)) {
      System.out.println("PacketBroadcasterWrapperListener [multicastip] [multicastport] {broadcasthosts} [broadcastport] [messagesize]");
      return COMMAND_ERROR_CODE;
    }
    else {
      multicastIP = commandSet.getArgument("multicastip");
      multicastPort = Integer.parseInt(commandSet.getArgument("multicastport"));
      broadcastHosts = commandSet.getArguments("broadcasthost");
      broadcastPort = Integer.parseInt(commandSet.getArgument("multicastport"));
      messagesize = Integer.parseInt(commandSet.getArgument("messagesize"));

      try {
        logger = LoggerManager.getLogger(PacketBroadcaster.class);
        packetBroadcaster = new PacketBroadcaster(logger, multicastIP, multicastPort, broadcastHosts, broadcastPort, messagesize);
      }
      catch (Exception exception) {
        exception.printStackTrace();
        return STACK_TRACE_ERROR_CODE;
      }

      return null;
    }
  }

  public int stop (int event) {

    try {
      packetBroadcaster.finish();
    }
    catch (Exception exception) {
      exception.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return NO_ERROR_CODE;
  }

  public static void main (String[] args) {

    WrapperManager.start(new PacketBroadcasterWrapperListener(), args);
  }

}
