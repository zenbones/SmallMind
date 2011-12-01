/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.cache.praxis.extrinsic;

import java.io.Serializable;
import java.util.Comparator;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceMode;
import org.smallmind.persistence.cache.AbstractCacheDao;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByKeySingularVector;

public class ByKeyExtrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  public ByKeyExtrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

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

  @Override
  public D persist (Class<D> durableClass, D durable, PersistenceMode mode) {

    if (durable != null) {

      D cachedDurable;
      DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, durable.getId());

      switch (mode) {
        case SOFT:

          return ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durableKey.getKey(), durable, 0)) != null) ? cachedDurable : durable;
        case HARD:
          getInstanceCache(durableClass).set(durableKey.getKey(), durable, 0);

          return durable;
        default:
          throw new UnknownSwitchCaseException(mode.name());
      }
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

      CASValue<DurableVector> casValue;
      DurableVector<I, D> vectorCopy;

      do {
        if ((casValue = getVectorCache(vectorKey.getElementClass()).getViaCas(vectorKey.getKey())).getValue() == null) {
          break;
        }

        vectorCopy = (!getVectorCache(vectorKey.getElementClass()).requiresCopyOnDistributedCASOperation()) ? null : (casValue.getValue() == null) ? null : casValue.getValue().copy();
        casValue.getValue().add(durable);
      } while (!getVectorCache(vectorKey.getElementClass()).putViaCas(vectorKey.getKey(), vectorCopy, casValue.getValue(), casValue.getVersion(), casValue.getValue().getTimeToLiveSeconds()));
    }
  }

  public void removeFromVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      CASValue<DurableVector> casValue;
      DurableVector<I, D> vectorCopy;

      do {
        if ((casValue = getVectorCache(vectorKey.getElementClass()).getViaCas(vectorKey.getKey())).getValue() == null) {
          break;
        }

        vectorCopy = (!getVectorCache(vectorKey.getElementClass()).requiresCopyOnDistributedCASOperation()) ? null : (casValue.getValue() == null) ? null : casValue.getValue().copy();
        casValue.getValue().remove(durable);
      } while (!getVectorCache(vectorKey.getElementClass()).putViaCas(vectorKey.getKey(), vectorCopy, casValue.getValue(), casValue.getVersion(), casValue.getValue().getTimeToLiveSeconds()));
    }
  }

  public DurableVector<I, D> getVector (VectorKey<D> vectorKey) {

    return getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey());
  }

  public DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector) {

    DurableVector<I, D> migratedVector;
    DurableVector<I, D> cachedVector;

    migratedVector = migrateVector(vectorKey.getElementClass(), vector);

    return ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), migratedVector, migratedVector.getTimeToLiveSeconds())) != null) ? cachedVector : vector;
  }

  public void deleteVector (VectorKey<D> vectorKey) {

    getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());
  }

  public DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector) {

    if (vector.isSingular()) {
      if (!(vector instanceof ByKeySingularVector)) {

        return new ByKeySingularVector<I, D>(new DurableKey<I, D>(managedClass, vector.head().getId()), vector.getTimeToLiveSeconds());
      }

      return vector;
    }
    else {
      if (!(vector instanceof ByKeyExtrinsicVector)) {

        return new ByKeyExtrinsicVector<I, D>(managedClass, vector.asList(), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLiveSeconds(), vector.isOrdered());
      }

      return vector;
    }
  }

  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

    return new ByKeySingularVector<I, D>(new DurableKey<I, D>(vectorKey.getElementClass(), durable.getId()), timeToLiveSeconds);
  }

  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    return new ByKeyExtrinsicVector<I, D>(vectorKey.getElementClass(), elementIter, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
