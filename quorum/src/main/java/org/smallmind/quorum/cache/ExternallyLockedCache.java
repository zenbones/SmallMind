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
package org.smallmind.quorum.cache;

public interface ExternallyLockedCache<K, V> {

   public abstract int size ();

   public abstract String getCacheName ();

   public abstract V get (KeyLock keyLock, K key, Object... parameters);

   public abstract V remove (KeyLock keyLock, K key);

   public abstract V put (KeyLock keyLock, K key, V value);

   public abstract V putIfAbsent (KeyLock keyLock, K key, V value);

   public abstract boolean exists (KeyLock keyLock, K key);

   public abstract void clear ();

   public abstract boolean isClosed ();

   public abstract void close ();

   public abstract long getExternalLockTimeout ();

   public abstract KeyLock lock (KeyLock keyLock, K key);

   public abstract void unlock (KeyLock keyLock, K key);

   public abstract <R> R executeLockedCallback (KeyLock keyLock, LockedCallback<K, R> callback);
}