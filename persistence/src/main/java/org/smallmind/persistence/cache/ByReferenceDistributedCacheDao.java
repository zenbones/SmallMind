package org.smallmind.persistence.cache;

import java.util.Comparator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableKey;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.cache.util.CachedList;

public class ByReferenceDistributedCacheDao<I extends Comparable<I>, D extends Durable<I>> extends WaterfallCacheDao<I, D> {

   public ByReferenceDistributedCacheDao (CacheDomain<I, D> cacheDomain) {

      super(null, cacheDomain);
   }

   public ByReferenceDistributedCacheDao (VectoredDao<I, D> nextDao, CacheDomain<I, D> cacheDomain) {

      super(nextDao, cacheDomain);
   }

   public D acquire (Class<D> durableClass, I id) {

      DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, id);

      return getInstanceCache(durableClass).get(durableKey.getKey());
   }

   public D get (Class<D> durableClass, I id) {

      D cachedDurable;
      VectoredDao<I, D> nextDao = getNextDao();
      DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, id);

      if (nextDao != null) {
         if ((cachedDurable = nextDao.get(durableClass, id)) != null) {

            return cachedDurable;
         }
      }

      if ((cachedDurable = getInstanceCache(durableClass).get(durableKey.getKey())) != null) {
         if (nextDao != null) {
            nextDao.persist(durableClass, cachedDurable);
         }

         return cachedDurable;
      }

      return null;
   }

   public D persist (Class<D> durableClass, D durable) {

      if (durable != null) {

         D cachedDurable;
         VectoredDao<I, D> nextDao = getNextDao();
         DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, durable.getId());

         if ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durableKey.getKey(), durable)) != null) {

            return cachedDurable;
         }

         if (nextDao != null) {
            nextDao.persist(durableClass, durable);
         }
      }

      return durable;
   }

   public void delete (Class<D> durableClass, D durable) {

      if (durable != null) {

         VectoredDao<I, D> nextDao = getNextDao();
         DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, durable.getId());

         getInstanceCache(durableClass).remove(durableKey.getKey());

         if (nextDao != null) {
            nextDao.delete(durableClass, durable);
         }
      }
   }

   public void updateInVector (VectorKey<D> vectorKey, D durable) {

      if (durable != null) {

         DurableVector<I, D> vector;
         VectoredDao<I, D> nextDao = getNextDao();

         if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
            vector.add(durable);
         }

         if (nextDao != null) {
            nextDao.updateInVector(vectorKey, durable);
         }
      }
   }

   public void removeFromVector (VectorKey<D> vectorKey, D durable) {

      if (durable != null) {

         DurableVector<I, D> vector;
         VectoredDao<I, D> nextDao = getNextDao();

         if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
            vector.remove(durable);
         }

         if (nextDao != null) {
            nextDao.removeFromVector(vectorKey, durable);
         }
      }
   }

   public DurableVector<I, D> getVector (VectorKey<D> vectorKey) {

      DurableVector<I, D> cachedVector;
      VectoredDao<I, D> nextDao = getNextDao();

      if (nextDao != null) {
         if ((cachedVector = nextDao.getVector(vectorKey)) != null) {

            return cachedVector;
         }
      }

      if ((cachedVector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
         if (nextDao != null) {
            nextDao.persistVector(vectorKey, cachedVector);
         }

         return cachedVector;
      }

      return null;
   }

   public DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector) {

      DurableVector<I, D> migratedVector;
      DurableVector<I, D> cachedVector;
      VectoredDao<I, D> nextDao = getNextDao();

      migratedVector = migrateVector(vector);
      if ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), migratedVector)) != null) {

         return cachedVector;
      }
      else if (nextDao != null) {
         nextDao.persistVector(vectorKey, migratedVector);
      }

      return vector;
   }

   public DurableVector<I, D> migrateVector (DurableVector<I, D> vector) {

      if (vector.isSingular()) {
         if (!(vector instanceof SingularByReferenceDurableVector)) {

            return new SingularByReferenceDurableVector<I, D>(vector.head(), vector.getTimeToLive());
         }

         return vector;
      }
      else {

         if (!(vector instanceof ByReferenceDurableVector)) {

            return new ByReferenceDurableVector<I, D>(new CachedList<D>(vector.asList()), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLive(), vector.isOrdered());
         }

         return vector;
      }
   }

   public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, long timeToLive) {

      DurableKey<I, D> durableKey;
      D inCacheDurable;

      durableKey = new DurableKey<I, D>(vectorKey.getElementClass(), durable.getId());
      if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(durableKey.getKey(), durable)) != null) {

         return new SingularByReferenceDurableVector<I, D>(inCacheDurable, timeToLive);
      }

      return new SingularByReferenceDurableVector<I, D>(durable, timeToLive);
   }

   public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

      CachedList<D> cacheConsistentElements;
      DurableKey<I, D> durableKey;
      D inCacheDurable;

      cacheConsistentElements = new CachedList<D>();
      for (D element : elementIter) {
         if (element != null) {

            durableKey = new DurableKey<I, D>(vectorKey.getElementClass(), element.getId());
            if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(durableKey.getKey(), element)) != null) {
               cacheConsistentElements.add(inCacheDurable);
            }
            else {
               cacheConsistentElements.add(element);
            }
         }
      }

      return new ByReferenceDurableVector<I, D>(cacheConsistentElements, comparator, maxSize, timeToLive, ordered);
   }

   public void deleteVector (VectorKey<D> vectorKey) {

      VectoredDao<I, D> nextDao = getNextDao();

      getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());

      if (nextDao != null) {
         nextDao.deleteVector(vectorKey);
      }
   }
}
