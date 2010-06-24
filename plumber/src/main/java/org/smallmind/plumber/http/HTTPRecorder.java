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
