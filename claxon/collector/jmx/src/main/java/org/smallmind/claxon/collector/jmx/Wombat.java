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
package org.smallmind.claxon.collector.jmx;

import java.util.concurrent.ThreadLocalRandom;
import org.smallmind.claxon.registry.Gauge;
import org.smallmind.claxon.registry.GaugeBuilder;
import org.smallmind.claxon.registry.Histogram;
import org.smallmind.claxon.registry.HistogramBuilder;
import org.smallmind.claxon.registry.Identifier;
import org.smallmind.claxon.registry.Registry;
import org.smallmind.claxon.registry.SystemClock;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.Trace;
import org.smallmind.claxon.registry.TraceBuilder;
import org.smallmind.claxon.registry.aggregate.Averaged;
import org.smallmind.claxon.registry.aggregate.Bounded;
import org.smallmind.claxon.registry.aggregate.Paced;
import org.smallmind.claxon.registry.aggregate.Pursued;
import org.smallmind.claxon.registry.aggregate.Stratified;
//import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Wombat {

  public static void main (String... args)
    throws Exception {

    Averaged averaged = new Averaged(SystemClock.instance());
    Bounded bounded = new Bounded(SystemClock.instance());
    Paced paced = new Paced(SystemClock.instance());
    Pursued pursued = new Pursued(SystemClock.instance());
    Stratified stratified = new Stratified(SystemClock.instance());

//    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/smallmind/claxon/registry/jmx.xml");
//    Registry registry = new Registry(SystemClock.instance()).bind("jmx", new JMXCollector(context.getBean("mbeanServer", MBeanServer.class)));
    Registry registry = new Registry();
    Gauge gauge = registry.register(Identifier.instance("gid"), new GaugeBuilder(), new Tag("one", "hello"), new Tag("two", "goodbye"));
    Histogram histogram = registry.register(Identifier.instance("hid"), new HistogramBuilder(), new Tag("one", "hello"), new Tag("two", "goodbye"));
    Trace trace = registry.register(Identifier.instance("tid"), new TraceBuilder(), new Tag("one", "hello"), new Tag("two", "goodbye"));

    for (int i = 0; i < 10; i++) {
      new Thread(new Worker(averaged, bounded, paced, pursued, stratified, gauge, histogram, trace)).start();
    }

    Thread.sleep(3000000);
  }

  private static class Worker implements Runnable {

    private Averaged averaged;
    private Bounded bounded;
    private Paced paced;
    private Pursued pursued;
    private Stratified stratified;
    private Gauge gauge;
    private Histogram histogram;
    private Trace trace;

    public Worker (Averaged averaged, Bounded bounded, Paced paced, Pursued pursued, Stratified stratified, Gauge gauge, Histogram histogram, Trace trace) {

      this.averaged = averaged;
      this.bounded = bounded;
      this.paced = paced;
      this.pursued = pursued;
      this.stratified = stratified;
      this.gauge = gauge;
      this.histogram = histogram;
      this.trace = trace;
    }

    @Override
    public void run () {

      while (true) {

        long value = ThreadLocalRandom.current().nextLong(100, 300);

        long start = System.nanoTime();
        averaged.update(value);
        long a = System.nanoTime() - start;
        start = System.nanoTime();
        bounded.update(value);
        long b = System.nanoTime() - start;
        start = System.nanoTime();
        paced.update(value);
        long c = System.nanoTime() - start;
        start = System.nanoTime();
        pursued.update(value);
        long d = System.nanoTime() - start;
        start = System.nanoTime();
        stratified.update(value);
        long e = System.nanoTime() - start;

        // System.out.println("av:" + a + ":bo:" + b + ":pa:" + c + ":pu:" + d + ":st:" + e);

        gauge.update(value);
        histogram.update(value);
        trace.update(value);

        try {
          Thread.sleep(1);
        } catch (InterruptedException i) {

        }
      }
    }
  }
}
