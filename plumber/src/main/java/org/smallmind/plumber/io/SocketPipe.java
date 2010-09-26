/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
