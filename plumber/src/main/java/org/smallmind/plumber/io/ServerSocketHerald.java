package org.smallmind.plumber.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.smallmind.nutsnbolts.util.Counter;
import org.smallmind.quorum.pool.ComponentFactory;
import org.smallmind.quorum.pool.ComponentPool;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.scribe.pen.Logger;

public class ServerSocketHerald implements ComponentFactory<SocketWorker>, Runnable {

   public static final int NO_THROTTLE = -1;

   private final Counter acceptCounter;

   private Logger logger;
   private Thread runnableThread;
   private ComponentPool<SocketWorker> workerPool;
   private SocketWorkerFactory workerFactory;
   private ServerSocket serverSocket;
   private boolean finished = false;
   private boolean exited = false;
   private int maxAccepted;

   public ServerSocketHerald (Logger logger, SocketWorkerFactory workerFactory, ServerSocket serverSocket, int maxAccepted, int poolSize)
      throws IOException {

      this.logger = logger;
      this.workerFactory = workerFactory;
      this.serverSocket = serverSocket;
      this.maxAccepted = maxAccepted;

      serverSocket.setSoTimeout(1000);

      acceptCounter = new Counter();

      workerPool = new ComponentPool<SocketWorker>(this, poolSize, PoolMode.EXPANDING_POOL);
   }

   public SocketWorker createComponent ()
      throws Exception {

      return workerFactory.createWorker(logger, this);
   }

   public void finish () {

      finished = true;

      while (!exited) {
         runnableThread.interrupt();

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException i) {
         }
      }
   }

   public void run () {

      Socket clientSocket;
      SocketWorker worker;
      Thread workThread;
      boolean annointed;

      runnableThread = Thread.currentThread();

      while (!finished) {
         try {
            annointed = false;
            synchronized (acceptCounter) {
               if ((maxAccepted < 0) || (acceptCounter.getCount() < maxAccepted)) {
                  annointed = true;
               }
            }

            if (annointed) {
               try {
                  clientSocket = serverSocket.accept();
                  acceptCounter.inc();

                  worker = workerPool.getComponent();
                  worker.setSocket(clientSocket);
                  workThread = new Thread(worker);
                  workThread.setDaemon(true);
                  workThread.start();
               }
               catch (SocketTimeoutException t) {
               }
            }
            else {
               try {
                  Thread.sleep(100);
               }
               catch (InterruptedException i) {
               }
            }
         }
         catch (Exception e) {
            logger.error(e);
         }
      }

      exited = true;
   }

   public void returnConnection (SocketWorker worker) {

      workerPool.returnComponent(worker);

      synchronized (acceptCounter) {
         acceptCounter.dec();
      }
   }

}
