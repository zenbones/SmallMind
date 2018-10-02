/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.praxis.intrinsic;

import java.io.Serializable;
import java.util.Comparator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.AbstractCacheDao;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;

// The cache supports thread-safe operations
// The vector cache references the instance by a JVM object handle
public class ByReferenceIntrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  public ByReferenceIntrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  public D persist (Class<D> durableClass, D durable, UpdateMode mode) {

    if (durable != null) {

      D cachedDurable;
      DurableKey<I, D> durableKey = new DurableKey<>(durableClass, durable.getId());

      return ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durableKey.getKey(), durable, 0)) != null) ? cachedDurable : durable;
    }

    return null;
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
        if (vector.isSingular()) {
          deleteVector(vectorKey);
        }
        else {
          vector.remove(durable);
        }
      }
    }
  }

  public DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector) {

    if (vector.isSingular()) {
      if (!(vector instanceof ByReferenceSingularVector)) {

        return new ByReferenceSingularVector<>(vector.head(), vector.getTimeToLiveSeconds());
      }

      return vector;
    }
    else {
      if (!(vector instanceof ByReferenceIntrinsicVector)) {

        return new ByReferenceIntrinsicVector<>(new IntrinsicRoster<>(vector.asBestEffortPreFetchedList()), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLiveSeconds(), vector.isOrdered());
      }

      return vector;
    }
  }

  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

    DurableKey<I, D> durableKey;
    D inCacheDurable;

    durableKey = new DurableKey<>(vectorKey.getElementClass(), durable.getId());
    if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(durableKey.getKey(), durable, 0)) != null) {

      return new ByReferenceSingularVector<>(inCacheDurable, timeToLiveSeconds);
    }

    return new ByReferenceSingularVector<>(durable, timeToLiveSeconds);
  }

  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    IntrinsicRoster<D> cacheConsistentElements;
    DurableKey<I, D> durableKey;
    D inCacheDurable;

    cacheConsistentElements = new IntrinsicRoster<>();
    for (D element : elementIter) {
      if (element != null) {

        durableKey = new DurableKey<>(vectorKey.getElementClass(), element.getId());
        if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(durableKey.getKey(), element, timeToLiveSeconds)) != null) {
          cacheConsistentElements.add(inCacheDurable);
        }
        else {
          cacheConsistentElements.add(element);
        }
      }
    }

    return new ByReferenceIntrinsicVector<>(cacheConsistentElements, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
