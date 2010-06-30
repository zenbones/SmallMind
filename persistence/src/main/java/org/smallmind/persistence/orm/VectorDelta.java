package org.smallmind.persistence.orm;

import java.util.HashSet;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;

public class VectorDelta<I extends Comparable<I>, D extends Durable<I>> {

   private DurableVector<I, D> persistedVector;
   private HashSet<D> updatedInVectorSet;
   private HashSet<I> removedFromVectorSet;
   private boolean deleted;

   public VectorDelta () {

      updatedInVectorSet = new HashSet<D>();
      removedFromVectorSet = new HashSet<I>();
      deleted = false;
   }

   public void delete () {

      deleted = true;
      persistedVector = null;
      updatedInVectorSet.clear();
      removedFromVectorSet.clear();
   }

   public void persist (DurableVector<I, D> persistedVector) {

      this.persistedVector = persistedVector;
      deleted = false;
   }

   public void updateInVector (D durable) {

      if (deleted) {
         throw new IllegalStateException("Vector has been marked as deleted within this transaction");
      }

      removedFromVectorSet.remove(durable.getId());
      updatedInVectorSet.add(durable);
   }

   public void removeFromVector (D durable) {

      if (deleted) {
         throw new IllegalStateException("Vector has been marked as deleted within this transaction");
      }

      updatedInVectorSet.remove(durable);
      removedFromVectorSet.add(durable.getId());
   }

   public DurableVector<I, D> processVector (DurableVector<I, D> durableVector) {

      if (deleted) {
         throw new IllegalStateException("Vector has been marked as deleted within this transaction");
      }

      if (persistedVector != null) {
         if (removedFromVectorSet.isEmpty() && updatedInVectorSet.isEmpty()) {

            return persistedVector;
         }

         return mergeChanges(persistedVector.copy());
      }
      else if (durableVector != null) {

         if (removedFromVectorSet.isEmpty() && updatedInVectorSet.isEmpty()) {

            return durableVector;
         }

         return mergeChanges(durableVector.copy());
      }
      else {

         return null;
      }
   }

   private DurableVector<I, D> mergeChanges (DurableVector<I, D> mergeVector) {

      for (I id : removedFromVectorSet) {
         mergeVector.removeId(id);
      }
      for (D durable : updatedInVectorSet) {
         mergeVector.add(durable);
      }

      return mergeVector;
   }
}
