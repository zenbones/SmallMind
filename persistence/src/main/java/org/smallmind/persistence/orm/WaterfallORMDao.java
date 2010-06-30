package org.smallmind.persistence.orm;

import java.io.Serializable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.WaterfallDao;
import org.smallmind.persistence.statistics.StatSource;

public abstract class WaterfallORMDao<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ORMDao<I, D>, WaterfallDao<I, D> {

   private VectoredDao<I, D> nextDao;
   private boolean allowCascade;

   public WaterfallORMDao (VectoredDao<I, D> nextDao, boolean allowCascade) {

      this.nextDao = nextDao;
      this.allowCascade = allowCascade;
   }

   public abstract void imprint (D durable);

   public String getStatisticsSource () {

      return StatSource.ORM.getDisplay();
   }

   public VectoredDao<I, D> getNextDao () {

      if (!allowCascade) {

         return null;
      }

      return nextDao;
   }
}