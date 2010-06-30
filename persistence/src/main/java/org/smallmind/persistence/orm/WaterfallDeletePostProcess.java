package org.smallmind.persistence.orm;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;

public class WaterfallDeletePostProcess<I extends Comparable<I>, D extends Durable<I>> extends TransactionPostProcess {

   private VectoredDao<I, D> nextDao;
   Class<D> durableClass;
   D durable;

   public WaterfallDeletePostProcess (VectoredDao<I, D> nextDao, Class<D> durableClass, D durable) {

      super(TransactionEndState.COMMIT, ProcessPriority.LAST);

      this.nextDao = nextDao;
      this.durableClass = durableClass;
      this.durable = durable;
   }

   public void process () {

      nextDao.delete(durableClass, durable);
   }
}