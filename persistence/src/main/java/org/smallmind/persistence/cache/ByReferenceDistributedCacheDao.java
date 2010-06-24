package org.smallmind.persistence.cache;

import java.util.LinkedList;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;

public class ByReferenceDistributedCacheDao<Long, D extends Durable<Long>> extends WaterfallCacheDao<Long, D> {

   public ByReferenceDistributedCacheDao (CacheDomain<Long, D> cacheDomain) {

      super(null, cacheDomain);
   }

   public ByReferenceDistributedCacheDao (VectoredDao<Long, D> nextDao, CacheDomain<Long, D> cacheDomain) {

      super(nextDao, cacheDomain);
   }

   public D get (Class<D> durableClass, Long id) {

      D cachedDurable;
      VectoredDao<Long, D> nextDao = getNextDao();

      if (nextDao != null) {
         if ((cachedDurable = nextDao.get(durableClass, id)) != null) {

            return cachedDurable;
         }
      }

      if ((cachedDurable = getInstanceCache(durableClass).get(id)) != null) {
         if (nextDao != null) {
            nextDao.persist(durableClass, cachedDurable);
         }

         return cachedDurable;
      }

      return null;
   }

   public D persist (Class<D> durableClass, D durable) {

      D cachedDurable;
      VectoredDao<Long, D> nextDao = getNextDao();

      if ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durable.getId(), durable)) != null) {
         return cachedDurable;
      }

      if (nextDao != null) {
         nextDao.persist(durableClass, durable);
      }

      return durable;
   }

   public void delete (Class<D> durableClass, D durable) {

      VectoredDao<Long, D> nextDao = getNextDao();

      getInstanceCache(durableClass).remove(durable.getId());

      if (nextDao != null) {
         nextDao.delete(durableClass, durable);
      }
   }

   public void updateInVector (VectorKey<Long, D> vectorKey, D durable) {

      DurableVector<Long, D> vector;
      VectoredDao<Long, D> nextDao = getNextDao();

      if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
         vector.add(durable);
      }

      if (nextDao != null) {
         nextDao.updateInVector(vectorKey, durable);
      }
   }

   public void removeFromVector (VectorKey<Long, D> vectorKey, D durable) {

      DurableVector<Long, D> vector;
      VectoredDao<Long, D> nextDao = getNextDao();

      if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
         vector.remove(durable);
      }

      if (nextDao != null) {
         nextDao.removeFromVector(vectorKey, durable);
      }
   }

   public DurableVector<Long, D> getVector (VectorKey<Long, D> vectorKey) {

      DurableVector<Long, D> cachedVector;
      VectoredDao<Long, D> nextDao = getNextDao();

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

   public DurableVector<Long, D> persistVector (VectorKey<Long, D> vectorKey, Iterable<D> elementIter) {

      DurableVector<Long, D> vector;
      DurableVector<Long, D> cachedVector;

      LinkedList<D> cacheConsistentElements;
      D inCacheDurable;
      VectoredDao<Long, D> nextDao = getNextDao();

      cacheConsistentElements = new LinkedList<D>();
      for (D element : elementIter) {
         if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(element.getId(), element)) != null) {
            cacheConsistentElements.add(inCacheDurable);
         }
         else {
            cacheConsistentElements.add(element);
         }
      }

      if ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), vector = new ByReferenceDurableVector<Long, D>(cacheConsistentElements))) != null) {
         return cachedVector;
      }
      else {
         if (nextDao != null) {
            nextDao.persistVector(vectorKey, cacheConsistentElements);
         }
      }

      return vector;
   }

   public void deleteVector (VectorKey<Long, D> vectorKey) {

      VectoredDao<Long, D> nextDao = getNextDao();

      getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());

      if (nextDao != null) {
         nextDao.deleteVector(vectorKey);
      }
   }
}
