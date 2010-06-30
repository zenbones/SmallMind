package org.smallmind.persistence.cache;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.SingleItemIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorPredicate;
import org.terracotta.modules.annotations.AutolockRead;
import org.terracotta.modules.annotations.AutolockWrite;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class SingularByReferenceDurableVector<I extends Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

   private D durable;

   public SingularByReferenceDurableVector (D durable, long timeToLive) {

      super(null, 1, timeToLive, false);

      this.durable = durable;
   }

   @AutolockRead
   public DurableVector<I, D> copy () {

      return new SingularByReferenceDurableVector<I, D>(durable, getTimeToLive());
   }

   public boolean isSingular () {

      return true;
   }

   @AutolockWrite
   public synchronized void add (D durable) {

      if ((durable != null) && (!durable.equals(this.durable))) {
         this.durable = durable;
      }
   }

   public void remove (D durable) {

      throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
   }

   public void removeId (I id) {

      throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
   }

   public void filter (VectorPredicate<D> predicate) {

      throw new UnsupportedOperationException("Attempted filter of a 'singular' vector");
   }

   @AutolockRead
   public synchronized D head () {

      if (durable == null) {
         throw new UnsupportedOperationException("Empty singular reference");
      }

      return durable;
   }

   @AutolockRead
   public synchronized List<D> asList () {

      if (durable == null) {

         return Collections.emptyList();
      }

      return Collections.singletonList(durable);
   }

   @AutolockRead
   public synchronized Iterator<D> iterator () {

      return new SingleItemIterator<D>(durable);
   }
}
