package org.smallmind.persistence.cache;

import java.util.Comparator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorPredicate;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class DurableVector<I extends Comparable<I>, D extends Durable<I>> implements Iterable<D> {

   private Comparator<D> comparator;
   private boolean ordered;
   private long creationTime;
   private long timeToLive;
   private int maxSize;

   public DurableVector (Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

      this.comparator = comparator;
      this.maxSize = maxSize;
      this.timeToLive = timeToLive;
      this.ordered = ordered;

      creationTime = System.currentTimeMillis();
   }

   public abstract DurableVector<I, D> copy ();

   public abstract boolean isSingular ();

   public Comparator<D> getComparator () {

      return comparator;
   }

   public int getMaxSize () {

      return maxSize;
   }

   public long getTimeToLive () {

      return timeToLive;
   }

   public boolean isOrdered () {

      return ordered;
   }

   public boolean isAlive () {

      return (timeToLive <= 0) || (System.currentTimeMillis() - creationTime <= timeToLive);
   }

   public abstract void add (D durable);

   public abstract void remove (D durable);

   public abstract void removeId (I id);

   public abstract void filter (VectorPredicate<D> predicate);

   public abstract D head ();

   public abstract List<D> asList ();
}