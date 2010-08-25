package org.smallmind.plumber.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.Counter;
import org.smallmind.quorum.pool.ComponentFactory;
import org.smallmind.quorum.pool.ComponentPool;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.scribe.pen.Logger;

public class ServerSocketHerald implements ComponentFactory<SocketWorker>, Runnable {

   public static final int NO_THROTTLE = -1;

   private final Counter acceptCounter;

   private Logger logger;
   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private ComponentPool<SocketWorker> workerPool;
   private SocketWorkerFactory workerFactory;
   private ServerSocket serverSocket;
   private int maxAccepted;

   public ServerSocketHerald (Logger logger, SocketWorkerFactory workerFactory, ServerSocket serverSocket, int maxAccepted, int poolSize)
      throws IOException {

      this.logger = logger;
      this.workerFactory = workerFactory;
      this.serverSocket = serverSocket;
      this.maxAccepted = maxAccepted;

      serverSocket.setSoTimeout(1000);

      acceptCounter = new Counter();
      pulseLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);

      workerPool = new ComponentPool<SocketWorker>(this, poolSize, PoolMode.EXPANDING_POOL);
   }

   public SocketWorker createComponent ()
      throws Exception {

      return workerFactory.createWorker(logger, this);
   }

   public void finish ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
         pulseLatch.countDown();
      }

      exitLatch.await();
   }

   public void run () {

      Socket clientSocket;
      SocketWorker worker;
      Thread workThread;
      boolean annointed;

      while (!finished.get()) {
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
                  synchronized (acceptCounter) {
                     acceptCounter.inc();
                  }

                  worker = workerPool.getComponent();
                  worker.setSocket(clientSocket);
                  workThread = new Thread(worker);
                  workThread.setDaemon(true);
                  workThread.start();
               }
               catch (SocketTimeoutException t) {
                  logger.error(t);
               }
            }
            else {
               try {
                  pulseLatch.await(100, TimeUnit.MILLISECONDS);
               }
               catch (InterruptedException interruptedException) {
                  logger.error(interruptedException);
               }
            }
         }
         catch (Exception e) {
            logger.error(e);
         }
      }

      exitLatch.countDown();
   }

   public void returnConnection (SocketWorker worker) {

      workerPool.returnComponent(worker);

      synchronized (acceptCounter) {
         acceptCounter.dec();
      }
   }

}
