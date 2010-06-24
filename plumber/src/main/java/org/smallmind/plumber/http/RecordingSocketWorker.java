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
