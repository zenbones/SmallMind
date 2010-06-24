package org.smallmind.plumber.io;

import java.io.IOException;
import java.net.Socket;
import org.smallmind.scribe.pen.Logger;

public abstract class SocketWorker implements Runnable {

   private Logger logger;
   private ServerSocketHerald herald;
   private Socket socket;

   public SocketWorker (Logger logger, ServerSocketHerald herald) {

      this.logger = logger;
      this.herald = herald;
   }

   public void setSocket (Socket socket) {

      this.socket = socket;
   }

   public abstract void socketWork (Socket socket)
      throws Exception;

   public void run () {

      try {
         if (socket == null) {
            throw new IllegalArgumentException("No socket has been set on this SocketWorker");
         }
         if (socket.isClosed()) {
            throw new IllegalArgumentException("The socket has already been closed");
         }

         try {
            socketWork(socket);
         }
         finally {
            socket.close();
         }
      }
      catch (Exception e) {
         logger.error(e);
      }
      finally {
         herald.returnConnection(this);
      }
   }

   public void close ()
      throws IOException {

      socket.close();
   }
}
