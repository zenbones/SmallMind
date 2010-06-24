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
