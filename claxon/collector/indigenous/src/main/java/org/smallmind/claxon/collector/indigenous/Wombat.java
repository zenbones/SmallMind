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
package org.smallmind.claxon.collector.indigenous;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.HistogramBuilder;
import org.smallmind.claxon.registry.Identifier;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Registry;
import org.smallmind.claxon.registry.SystemClock;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
//import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Wombat {

  public static void main (String... args)
    throws Exception {

    new PerApplicationContext().prepareThread();
    Registry registry = new Registry(SystemClock.instance()).bind("jmx", new IndigenousCollector());
    Instrument.register(registry);

    for (int i = 0; i < 10; i++) {
      new Thread(new Worker()).start();
    }

    Thread.sleep(3000000);
  }

  private static class Worker implements Runnable {

    @Override
    public void run () {

      while (true) {

        long value = ThreadLocalRandom.current().nextLong(100, 300);

        try {
          Instrument.with(Identifier.instance("hid"), new HistogramBuilder(), new Tag("one", "hello"), new Tag("two", "goodbye")).as(TimeUnit.SECONDS).on(() -> {
            Thread.sleep(value);
          });
        } catch (Exception e) {
          e.printStackTrace();
        }

        try {
          Thread.sleep(1);
        } catch (InterruptedException i) {

        }
      }
    }
  }
}
