/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
