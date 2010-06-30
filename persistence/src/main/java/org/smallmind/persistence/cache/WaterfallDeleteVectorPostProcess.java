package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcess;

public class WaterfallDeleteVectorPostProcess<I extends Comparable<I>, D extends Durable<I>> extends TransactionPostProcess {

   private VectoredDao<I, D> nextDao;
   private VectorKey<D> vectorKey;

   public WaterfallDeleteVectorPostProcess (VectoredDao<I, D> nextDao, VectorKey<D> vectorKey) {

      super(TransactionEndState.COMMIT, ProcessPriority.MIDDLE);

      this.nextDao = nextDao;
      this.vectorKey = vectorKey;
   }

   public void process ()
      throws Exception {

      nextDao.deleteVector(vectorKey);
   }
}