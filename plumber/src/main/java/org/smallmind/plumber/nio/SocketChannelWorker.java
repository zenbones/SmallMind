package org.smallmind.plumber.nio;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.smallmind.scribe.pen.Logger;

public abstract class SocketChannelWorker implements Runnable {

   private Logger logger;
   private ServerSocketChannelHerald herald;
   private ServerSocketChannel readyChannel;

   public SocketChannelWorker (Logger logger, ServerSocketChannelHerald herald) {

      this.logger = logger;
      this.herald = herald;
   }

   public void setChannel (ServerSocketChannel readyChannel) {

      this.readyChannel = readyChannel;
   }

   public abstract void socketChannelWork (SocketChannel socketChannel)
      throws Exception;

   public void run () {

      SocketChannel socketChannel;

      try {
         if (readyChannel == null) {
            throw new IllegalArgumentException("No channel has been set on this SocketChannelWorker");
         }

         socketChannel = readyChannel.accept();
         try {
            socketChannelWork(socketChannel);
         }
         finally {
            socketChannel.close();
         }
      }
      catch (Exception e) {
         logger.error(e);
      }
      finally {
         herald.returnConnection(this);
      }
   }

}
