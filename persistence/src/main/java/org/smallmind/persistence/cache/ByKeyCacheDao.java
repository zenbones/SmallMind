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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.Comparator;
import org.smallmind.persistence.Durable;

public class ByKeyCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  public ByKeyCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  public D acquire (Class<D> durableClass, I id) {

    DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, id);

    return getInstanceCache(durableClass).get(durableKey.getKey());
  }

  public D get (Class<D> durableClass, I id) {

    DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, id);

    return getInstanceCache(durableClass).get(durableKey.getKey());
  }

  public D persist (Class<D> durableClass, D durable) {

    if (durable != null) {

      D cachedDurable;
      DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, durable.getId());

      return ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durableKey.getKey(), durable)) != null) ? cachedDurable : durable;
    }

    return null;
  }

  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, durable.getId());

      getInstanceCache(durableClass).remove(durableKey.getKey());
    }
  }

  public void updateInVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      DurableVector<I, D> vector;

      if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
        vector.add(durable);
      }
    }
  }

  public void removeFromVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      DurableVector<I, D> vector;

      if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
        vector.remove(durable);
      }
    }
  }

  public DurableVector<I, D> getVector (VectorKey<D> vectorKey) {

    return getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey());
  }

  public DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector) {

    DurableVector<I, D> migratedVector;
    DurableVector<I, D> cachedVector;

    migratedVector = migrateVector(vectorKey.getElementClass(), vector);

    return ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), migratedVector)) != null) ? cachedVector : vector;
  }

  public DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector) {

    if (vector.isSingular()) {
      if (!(vector instanceof ByKeySingularDurableVector)) {

        return new ByKeySingularDurableVector<I, D>(new DurableKey<I, D>(managedClass, vector.head().getId()), vector.getTimeToLive());
      }

      return vector;
    }
    else {
      if (!(vector instanceof ByKeyDurableVector)) {

        return new ByKeyDurableVector<I, D>(managedClass, vector.asList(), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLive(), vector.isOrdered());
      }

      return vector;
    }
  }

  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, long timeToLive) {

    return new ByKeySingularDurableVector<I, D>(new DurableKey<I, D>(vectorKey.getElementClass(), durable.getId()), timeToLive);
  }

  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

    return new ByKeyDurableVector<I, D>(vectorKey.getElementClass(), elementIter, comparator, maxSize, timeToLive, ordered);
  }

  public void deleteVector (VectorKey<D> vectorKey) {

    getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());
  }
}
