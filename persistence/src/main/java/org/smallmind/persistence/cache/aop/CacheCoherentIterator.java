package org.smallmind.persistence.cache.aop;

import java.util.Iterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;

public class CacheCoherentIterator<I extends Comparable<I>, D extends Durable<I>> implements Iterator<D>, Iterable<D> {

   private Iterator<D> durableIter;
   private VectoredDao<I, D> vectoredDao;
   private Class<D> durableClass;

   public CacheCoherentIterator (Iterator<D> durableIter, Class<D> durableClass, VectoredDao<I, D> vectoredDao) {

      this.durableIter = durableIter;
      this.durableClass = durableClass;
      this.vectoredDao = vectoredDao;
   }

   public boolean hasNext () {

      return durableIter.hasNext();
   }

   public Iterator<D> iterator () {

      return this;
   }

   public D next () {

      return vectoredDao.persist(durableClass, durableIter.next());
   }

   public void remove () {

      durableIter.remove();
   }
}
