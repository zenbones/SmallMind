package org.smallmind.persistence.statistics;

import java.io.Serializable;

public class StatLine implements Serializable {

   private String source;
   private long avgTime = 0;
   private int hits = 0;

   public StatLine (String source) {

      this.source = source;
   }

   public String getSource () {

      return source;
   }

   public synchronized int getHits () {

      return hits;
   }

   public synchronized long getAvgTime () {

      return avgTime;
   }

   public synchronized void hit (long time) {

      avgTime = ((avgTime * hits) + time) / (hits + 1);
      hits++;
   }
}
