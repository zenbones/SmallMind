package org.smallmind.scribe.pen.probe;

import java.io.Serializable;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;

public class MetricMilieu implements Serializable {

   private Discriminator discriminator;
   private Level level;
   private Metric metric;

   public MetricMilieu (Discriminator discriminator, Level level, Metric metric) {

      this.discriminator = discriminator;
      this.level = level;
      this.metric = metric;
   }

   public MetricMilieu (MetricMilieu metricMilieu) {

      discriminator = metricMilieu.getDiscriminator();
      level = metricMilieu.getLevel();
      metric = new Metric(metricMilieu.getMetric());
   }

   public Discriminator getDiscriminator () {

      return discriminator;
   }

   public Level getLevel () {

      return level;
   }

   public Metric getMetric () {

      return metric;
   }
}
