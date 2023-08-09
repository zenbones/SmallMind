/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.bayeux.cometd.transport;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class AbstractExpiringCarrier implements OumuamuaCarrier {

  private final IdleCheck idleCheck;
  private final long clientTimeoutMilliseconds;
  private final long idleCheckMilliseconds;
  private final long defaultMaxSessionIdleTimeout;
  private long lastContactMilliseconds;
  private long maxSessionIdleTimeout;

  public AbstractExpiringCarrier (long clientTimeoutMilliseconds, long idleCheckMilliseconds, long defaultMaxSessionIdleTimeout) {

    Thread idleCheckThread;

    this.clientTimeoutMilliseconds = clientTimeoutMilliseconds;
    this.idleCheckMilliseconds = idleCheckMilliseconds;
    this.defaultMaxSessionIdleTimeout = defaultMaxSessionIdleTimeout;

    maxSessionIdleTimeout = (clientTimeoutMilliseconds > 0) ? clientTimeoutMilliseconds : defaultMaxSessionIdleTimeout;

    idleCheckThread = new Thread(idleCheck = new IdleCheck());
    idleCheckThread.setDaemon(true);
    idleCheckThread.start();
  }

  public abstract void finishClosing ();

  public synchronized void updateLastContact () {

    lastContactMilliseconds = System.currentTimeMillis();
  }

  @Override
  public void setMaxSessionIdleTimeout (long maxSessionIdleTimeout) {

    long adjustedIdleTimeout = (maxSessionIdleTimeout >= 0) ? maxSessionIdleTimeout : clientTimeoutMilliseconds;

    this.maxSessionIdleTimeout = (adjustedIdleTimeout >= 0) ? adjustedIdleTimeout : defaultMaxSessionIdleTimeout;
  }

  @Override
  public synchronized void close () {

    try {
      idleCheck.stop();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(LocalCarrier.class).error(interruptedException);
    } finally {
      finishClosing();
    }
  }

  private class IdleCheck implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!finishLatch.await(idleCheckMilliseconds, TimeUnit.MILLISECONDS)) {
          if (lastContactMilliseconds > 0) {
            if (System.currentTimeMillis() > lastContactMilliseconds + maxSessionIdleTimeout) {
              finishLatch.countDown();
              finishClosing();
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
      } finally {
        exitLatch.countDown();
      }
    }
  }
}
