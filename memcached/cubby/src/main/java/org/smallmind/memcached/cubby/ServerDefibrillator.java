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
package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class ServerDefibrillator implements Runnable {

  private final CountDownLatch finishedLatch = new CountDownLatch(1);
  private final CountDownLatch terminatedLatch = new CountDownLatch(1);
  private final CubbyMemcachedClient client;
  private final ServerPool serverPool;
  private final long resuscitationSeconds;
  private final int connectionTimeoutMilliseconds;

  public ServerDefibrillator (CubbyMemcachedClient client, ServerPool serverPool, int connectionTimeoutMilliseconds, long resuscitationSeconds) {

    this.client = client;
    this.serverPool = serverPool;
    this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
    this.resuscitationSeconds = resuscitationSeconds;
  }

  public void stop ()
    throws InterruptedException {

    finishedLatch.countDown();
    terminatedLatch.await();
  }

  @Override
  public void run () {

    try {
      while (!finishedLatch.await(resuscitationSeconds, TimeUnit.SECONDS)) {

        LinkedList<MemcachedHost> reconnectionList = new LinkedList<>();

        for (MemcachedHost memcachedHost : serverPool.values()) {
          if (!memcachedHost.isActive()) {
            try (Socket socket = new Socket()) {
              socket.connect(memcachedHost.getAddress(), connectionTimeoutMilliseconds);
              reconnectionList.add(memcachedHost);
            } catch (IOException ioException) {
              // do nothing
            }
          }
        }

        if (!reconnectionList.isEmpty()) {
          for (MemcachedHost memcachedHost : reconnectionList) {
            try {
              client.reconnect(memcachedHost);
            } catch (IOException ioException) {
              LoggerManager.getLogger(ServerDefibrillator.class).error(ioException);
            }
          }
        }
      }
    } catch (InterruptedException interruptedException) {
      finishedLatch.countDown();
      LoggerManager.getLogger(ServerDefibrillator.class).error(interruptedException);
    } finally {
      terminatedLatch.countDown();
    }
  }
}
