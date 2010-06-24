package org.smallmind.scribe.pen;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceGenerator {

   private static AtomicLong count = new AtomicLong(0);

   public static long next () {

      return count.incrementAndGet();
   }
}
