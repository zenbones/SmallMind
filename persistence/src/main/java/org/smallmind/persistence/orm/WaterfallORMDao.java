package org.smallmind.persistence.orm;

import java.io.Serializable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.WaterfallDao;

public abstract class WaterfallORMDao<I extends Serializable, D extends Durable<I>> implements ORMDao<I, D>, WaterfallDao<I, D> {

   private VectoredDao<I, D> nextDao;

   public WaterfallORMDao () {
   }

   public WaterfallORMDao (VectoredDao<I, D> nextDao) {

      this.nextDao = nextDao;
   }

   public VectoredDao<I, D> getNextDao () {

      return nextDao;
   }
}