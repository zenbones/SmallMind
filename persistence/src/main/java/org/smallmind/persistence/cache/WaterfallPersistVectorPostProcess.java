package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcess;

public class WaterfallPersistVectorPostProcess<I extends Comparable<I>, D extends Durable<I>> extends TransactionPostProcess {

   private VectoredDao<I, D> nextDao;
   private VectorKey<D> vectorKey;
   private DurableVector<I, D> vector;

   public WaterfallPersistVectorPostProcess (VectoredDao<I, D> nextDao, VectorKey<D> vectorKey, DurableVector<I, D> vector) {

      super(TransactionEndState.COMMIT, ProcessPriority.MIDDLE);

      this.nextDao = nextDao;
      this.vectorKey = vectorKey;
      this.vector = vector;
   }

   public DurableVector<I, D> getVector () {

      return vector;
   }

   public void process ()
      throws Exception {

      nextDao.persistVector(vectorKey, vector);
   }
}