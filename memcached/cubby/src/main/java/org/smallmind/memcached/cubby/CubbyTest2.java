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

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.codec.LargeValueCompressingCodec;
import org.smallmind.memcached.cubby.codec.ObjectStreamCubbyCodec;
import org.smallmind.memcached.cubby.command.GetCommand;
import org.smallmind.memcached.cubby.command.SetCommand;
import org.smallmind.memcached.cubby.locator.MaglevKeyLocator;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.translator.DefaultKeyTranslator;
import org.smallmind.memcached.cubby.translator.LargeKeyHashingTranslator;

public class CubbyTest2 {

  public static void main (String... args)
    throws Exception {

    CubbyCodec codec = new LargeValueCompressingCodec(new ObjectStreamCubbyCodec());
    CubbyConfiguration configuration = new CubbyConfiguration()
      .setCodec(codec)
      .setKeyLocator(new MaglevKeyLocator())
      .setKeyTranslator(new LargeKeyHashingTranslator(new DefaultKeyTranslator()))
      .setConnectionsPerHost(1);
    CubbyMemcachedClient client = new CubbyMemcachedClient(configuration, new MemcachedHost("0", new InetSocketAddress("localhost", 11211)));

    client.start();

    System.out.println("send...");
    client.send(new SetCommand().setKey("hello").setValue("goodbye"), null);

    TokenGenerator tokenGenerator = new TokenGenerator();
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(100);
    AtomicInteger counter = new AtomicInteger(0);

    for (int i = 0; i < 100; i++) {
      new Thread(new Worker(counter, startLatch, finishLatch, client, tokenGenerator)).start();
    }

    long start = System.currentTimeMillis();
    startLatch.countDown();
    finishLatch.await();
    System.out.println(System.currentTimeMillis() - start);

    System.out.println("done[" + counter.get() + "]...");
    client.stop();
  }

  private static class Worker implements Runnable {

    private final AtomicInteger counter;
    private final CountDownLatch startLatch;
    private final CountDownLatch finishLatch;
    private final CubbyMemcachedClient client;
    private final TokenGenerator tokenGenerator;

    public Worker (AtomicInteger counter, CountDownLatch startLatch, CountDownLatch finishLatch, CubbyMemcachedClient client, TokenGenerator tokenGenerator) {

      this.counter = counter;
      this.startLatch = startLatch;
      this.finishLatch = finishLatch;
      this.client = client;
      this.tokenGenerator = tokenGenerator;
    }

    @Override
    public void run () {

      try {
        startLatch.await();

        for (int i = 0; i < 1000; i++) {

//          String opaqueToken = tokenGenerator.next();

          Response response = client.send(new GetCommand().setKey("hello").setCas(true), null);
  //        if (!opaqueToken.equals(response.getToken())) {
      //      System.exit(0);
    //      }
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
