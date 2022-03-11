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
package org.smallmind.memcached.utility.spring;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.impl.AddressMemcachedSessionComparator;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;

public class CubbyTest3 {

  public static void main (String... args)
    throws Exception {

    HashMap<InetSocketAddress, InetSocketAddress> addressMap = new HashMap<>();
    addressMap.put(new InetSocketAddress("localhost", 11211), new InetSocketAddress("localhost", 11211));

    XMemcachedClientBuilder builder = new XMemcachedClientBuilder(addressMap);

    builder.setFailureMode(true);
    builder.setConnectionPoolSize(1);
    builder.setCommandFactory(new TextCommandFactory());
    builder.setSessionLocator(new KetamaMemcachedSessionLocator());
    builder.setSessionComparator(new AddressMemcachedSessionComparator());

    MemcachedClient memcachedClient = builder.build();

    System.out.println("send...");
    memcachedClient.set("hello", 30000, "goodbye");

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(100);
    AtomicInteger counter = new AtomicInteger(0);

    for (int i = 0; i < 100; i++) {
      new Thread(new Worker(counter, startLatch, finishLatch, memcachedClient)).start();
    }

    long start = System.currentTimeMillis();
    startLatch.countDown();
    finishLatch.await();
    System.out.println(System.currentTimeMillis() - start);

    System.out.println("done[" + counter.get() + "]...");
  }

  private static class Worker implements Runnable {

    private final AtomicInteger counter;
    private final CountDownLatch startLatch;
    private final CountDownLatch finishLatch;
    private final MemcachedClient client;

    public Worker (AtomicInteger counter, CountDownLatch startLatch, CountDownLatch finishLatch, MemcachedClient client) {

      this.counter = counter;
      this.startLatch = startLatch;
      this.finishLatch = finishLatch;
      this.client = client;
    }

    @Override
    public void run () {

      try {
        startLatch.await();

        for (int i = 0; i < 1000; i++) {
          client.get("hello");
          counter.incrementAndGet();
        }
        finishLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
