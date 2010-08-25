package org.smallmind.plumber.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.Counter;
import org.smallmind.quorum.pool.ComponentFactory;
import org.smallmind.quorum.pool.ComponentPool;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.scribe.pen.Logger;

public class ServerSocketChannelHerald implements ComponentFactory<SocketChannelWorker>, Runnable {

   public static final int NO_THROTTLE = -1;

   private final Counter acceptCounter;

   private Logger logger;
   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private ComponentPool<SocketChannelWorker> workerPool;
   private SocketChannelWorkerFactory workerFactory;
   private Selector acceptSelector;
   private int maxAccepted;

   public ServerSocketChannelHerald (Logger logger, SocketChannelWorkerFactory workerFactory, ServerSocketChannel serverSocketChannel, int maxAccepted, int poolSize)
      throws IOException {

      this.logger = logger;
      this.workerFactory = workerFactory;
      this.maxAccepted = maxAccepted;

      serverSocketChannel.configureBlocking(false);

      acceptSelector = Selector.open();
      serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);

      acceptCounter = new Counter();
      pulseLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);

      workerPool = new ComponentPool<SocketChannelWorker>(this, poolSize, PoolMode.EXPANDING_POOL);
   }

   public SocketChannelWorker createComponent ()
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

      Thread workThread;
      SocketChannelWorker worker;
      ServerSocketChannel readyChannel;
      Set<SelectionKey> readyKeySet;
      Iterator<SelectionKey> readyKeyIter;
      SelectionKey readyKey;
      boolean accepted;

      while (!finished.get()) {
         try {
            if (acceptSelector.select(1000) > 0) {
               readyKeySet = acceptSelector.selectedKeys();
               readyKeyIter = readyKeySet.iterator();
               while (readyKeyIter.hasNext()) {
                  if (finished.get()) {
                     break;
                  }

                  accepted = false;
                  synchronized (acceptCounter) {
                     if ((maxAccepted < 0) || (acceptCounter.getCount() < maxAccepted)) {
                        acceptCounter.inc();
                        accepted = true;

                        readyKey = readyKeyIter.next();
                        readyKeyIter.remove();

                        readyChannel = (ServerSocketChannel)readyKey.channel();

                        worker = workerPool.getComponent();
                        worker.setChannel(readyChannel);
                        workThread = new Thread(worker);
                        workThread.setDaemon(true);
                        workThread.start();
                     }
                  }

                  if (!accepted) {
                     try {
                        pulseLatch.await(100, TimeUnit.MILLISECONDS);
                     }
                     catch (InterruptedException interruptedException) {
                        logger.error(interruptedException);
                     }
                  }
               }
            }
         }
         catch (Exception e) {
            logger.error(e);
         }
      }

      exitLatch.countDown();
   }

   public void returnConnection (SocketChannelWorker worker) {

      workerPool.returnComponent(worker);

      synchronized (acceptCounter) {
         acceptCounter.dec();
      }
   }
}
