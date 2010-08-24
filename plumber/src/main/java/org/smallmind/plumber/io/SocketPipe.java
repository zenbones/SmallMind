package org.smallmind.plumber.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.smallmind.nutsnbolts.util.RotaryLock;

public class SocketPipe {

   private static final int UPSTREAM = 0;
   private static final int DOWNSTREAM = 1;

   private IOException threadException = null;
   private Socket downstreamSocket;
   private Socket upstreamSocket;
   private int bufferSize;

   public SocketPipe (Socket downstreamSocket, Socket upstreamSocket, int bufferSize) {

      this.downstreamSocket = downstreamSocket;
      this.upstreamSocket = upstreamSocket;
      this.bufferSize = bufferSize;
   }

   public void startPipe ()
      throws IOException {

      Thread lockThread;
      RotaryLock rotaryLock;

      lockThread = Thread.currentThread();
      rotaryLock = new RotaryLock(2);

      synchronized (rotaryLock) {
         startTransfer(lockThread, rotaryLock, UPSTREAM, downstreamSocket, upstreamSocket);
         startTransfer(lockThread, rotaryLock, DOWNSTREAM, upstreamSocket, downstreamSocket);

         try {
            rotaryLock.wait();
         }
         catch (InterruptedException i) {
            if (threadException != null) {
               throw threadException;
            }
         }
      }
   }

   private void startTransfer (Thread lockThread, RotaryLock rotaryLock, int condition, Socket inputSocket, Socket outputSocket)
      throws IOException {

      Thread transferThread;

      transferThread = new Thread(new TransferHandler(lockThread, rotaryLock, condition, inputSocket, outputSocket));
      transferThread.setDaemon(true);

      transferThread.start();
   }

   public class TransferHandler implements Runnable {

      private final Thread lockThread;

      private RotaryLock rotaryLock;
      private Socket inputSocket;
      private Socket outputSocket;
      private InputStream inputStream;
      private OutputStream outputStream;
      private int condition;
      private byte[] buffer;

      public TransferHandler (Thread lockThread, RotaryLock rotaryLock, int condition, Socket inputSocket, Socket outputSocket)
         throws IOException {

         this.lockThread = lockThread;
         this.rotaryLock = rotaryLock;
         this.condition = condition;
         this.inputSocket = inputSocket;
         this.outputSocket = outputSocket;

         inputStream = inputSocket.getInputStream();
         outputStream = outputSocket.getOutputStream();
         buffer = new byte[bufferSize];
      }

      public void run () {

         int bytesRead;

         try {
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
               outputStream.write(buffer, 0, bytesRead);
            }

            inputSocket.shutdownInput();
            outputSocket.shutdownOutput();
            rotaryLock.unlock(condition);
         }
         catch (IOException ioException) {
            synchronized (lockThread) {
               if (threadException == null) {
                  threadException = ioException;
                  lockThread.interrupt();
               }
            }
         }
      }

   }

}
