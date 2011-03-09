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

import java.util.Comparator;
import org.smallmind.persistence.Dao;
import org.smallmind.persistence.Durable;

public interface VectoredDao<I extends Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

   public abstract void lookupLock ();

   public abstract void lookupUnlock ();

   public abstract void updateLock ();

   public abstract void updateUnlock ();

   public abstract void readLock (Class<D> managedClass, I id);

   public abstract void readUnlock (Class<D> managedClass, I id);

   public abstract void writeLock (Class<D> managedClass, I id);

   public abstract void writeUnlock (Class<D> managedClass, I id);

   public abstract void updateInVector (VectorKey<D> vectorKey, D durable);

   public abstract void removeFromVector (VectorKey<D> vectorKey, D durable);

   public abstract DurableVector<I, D> getVector (VectorKey<D> vectorKey);

   public abstract DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector);

   public abstract DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector);

   public abstract DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, long timeToLive);

   public abstract DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered);

   public abstract void deleteVector (VectorKey<D> vectorKey);
}