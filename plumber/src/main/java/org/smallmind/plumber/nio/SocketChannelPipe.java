package org.smallmind.plumber.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketChannelPipe {

   private SocketChannel downstreamSocketChannel;
   private SocketChannel upstreamSocketChannel;
   private ByteBuffer buffer;

   public SocketChannelPipe (SocketChannel downstreamSocketChannel, SocketChannel upstreamSocketChannel, int bufferSize) {

      this.downstreamSocketChannel = downstreamSocketChannel;
      this.upstreamSocketChannel = upstreamSocketChannel;

      buffer = ByteBuffer.allocate(bufferSize);
   }

   public void startPipe ()
      throws IOException {

      SocketChannel readySocketChannel;
      Selector readSelector;
      Set<SelectionKey> readyKeySet;
      Iterator<SelectionKey> readyKeyIter;
      SelectionKey readyKey;
      boolean downstreamOpen = true;
      boolean upstreamOpen = true;

      upstreamSocketChannel.configureBlocking(false);
      downstreamSocketChannel.configureBlocking(false);

      readSelector = Selector.open();

      upstreamSocketChannel.register(readSelector, SelectionKey.OP_READ);
      downstreamSocketChannel.register(readSelector, SelectionKey.OP_READ);

      while (downstreamOpen || upstreamOpen) {
         if (readSelector.select() > 0) {
            readyKeySet = readSelector.selectedKeys();
            readyKeyIter = readyKeySet.iterator();
            while (readyKeyIter.hasNext()) {
               readyKey = readyKeyIter.next();
               readyKeyIter.remove();
               readySocketChannel = (SocketChannel)readyKey.channel();

               if (downstreamOpen && (readySocketChannel == downstreamSocketChannel)) {
                  downstreamOpen = transferBuffer(downstreamSocketChannel, upstreamSocketChannel);
               }
               else if (upstreamOpen) {
                  upstreamOpen = transferBuffer(upstreamSocketChannel, downstreamSocketChannel);
               }
            }
         }
      }
   }

   private boolean transferBuffer (SocketChannel inChannel, SocketChannel outChannel)
      throws IOException {

      int totalBytes = 0;
      int bytesRead;

      while ((bytesRead = inChannel.read(buffer)) > 0) {
         totalBytes += bytesRead;
         buffer.flip();
         while (buffer.hasRemaining()) {
            outChannel.write(buffer);
         }
         buffer.clear();
      }

      if (totalBytes > 0) {
         return true;
      }
      else {
         inChannel.socket().shutdownInput();
         outChannel.socket().shutdownOutput();
         return false;
      }
   }

}
