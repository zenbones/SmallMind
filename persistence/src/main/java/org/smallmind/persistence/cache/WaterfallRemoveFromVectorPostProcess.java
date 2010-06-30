package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcess;

public class WaterfallRemoveFromVectorPostProcess<I extends Comparable<I>, D extends Durable<I>> extends TransactionPostProcess {

   private VectoredDao<I, D> nextDao;
   private VectorKey<D> vectorKey;
   private D durable;

   public WaterfallRemoveFromVectorPostProcess (VectoredDao<I, D> nextDao, VectorKey<D> vectorKey, D durable) {

      super(TransactionEndState.COMMIT, ProcessPriority.MIDDLE);

      this.nextDao = nextDao;
      this.vectorKey = vectorKey;
      this.durable = durable;
   }

   public void process () {

      nextDao.removeFromVector(vectorKey, durable);
   }
}