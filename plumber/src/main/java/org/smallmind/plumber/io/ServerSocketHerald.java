/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.Counter;
import org.smallmind.quorum.pool.simple.ComponentFactory;
import org.smallmind.quorum.pool.simple.ComponentPool;
import org.smallmind.quorum.pool.simple.SimplePoolConfig;
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

    SimplePoolConfig simplePoolConfig;

    this.logger = logger;
    this.workerFactory = workerFactory;
    this.serverSocket = serverSocket;
    this.maxAccepted = maxAccepted;

    serverSocket.setSoTimeout(1000);

    acceptCounter = new Counter();
    pulseLatch = new CountDownLatch(1);
    exitLatch = new CountDownLatch(1);

    simplePoolConfig = new SimplePoolConfig();
    simplePoolConfig.setAcquireWaitTimeMillis(poolSize);

    workerPool = new ComponentPool<SocketWorker>(this, simplePoolConfig);
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

    try {
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
    }
    finally {
      exitLatch.countDown();
    }
  }

  public void returnConnection (SocketWorker worker) {

    workerPool.returnComponent(worker);

    synchronized (acceptCounter) {
      acceptCounter.dec();
    }
  }

}
