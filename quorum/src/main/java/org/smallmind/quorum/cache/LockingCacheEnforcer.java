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

import java.util.HashMap;

public class LockingCacheEnforcer<K, V> implements LockingCache<K, V> {

   private static final InheritableThreadLocal<HashMap<Object, KeyLock>> KEY_LOCK_MAP_LOCAL = new InheritableThreadLocal<HashMap<Object, KeyLock>>() {
      @Override
      protected HashMap<Object, KeyLock> initialValue () {

         return new HashMap<Object, KeyLock>();
      }
   };

   private ExternallyLockedCache<K, V> cache;

   public LockingCacheEnforcer (ExternallyLockedCache<K, V> cache) {

      this.cache = cache;
   }

   public long getExternalLockTimeout () {

      return cache.getExternalLockTimeout();
   }

   public void lock (K key) {

      KEY_LOCK_MAP_LOCAL.get().put(key, cache.lock(KEY_LOCK_MAP_LOCAL.get().get(key), key));
   }

   public void unlock (K key) {

      cache.unlock(KEY_LOCK_MAP_LOCAL.get().remove(key), key);
   }

   public <R> R executeLockedCallback (LockedCallback<K, R> callback) {

      return cache.executeLockedCallback(KEY_LOCK_MAP_LOCAL.get().get(callback.getKey()), callback);
   }

   public int size () {

      return cache.size();
   }

   public String getCacheName () {

      return cache.getCacheName();
   }

   public V get (K key, Object... parameters) {

      return cache.get(KEY_LOCK_MAP_LOCAL.get().get(key), key, parameters);
   }

   public V remove (K key) {

      return cache.remove(KEY_LOCK_MAP_LOCAL.get().get(key), key);
   }

   public V put (K key, V value) {

      return cache.put(KEY_LOCK_MAP_LOCAL.get().get(key), key, value);
   }

   public V putIfAbsent (K key, V value) {

      return cache.putIfAbsent(KEY_LOCK_MAP_LOCAL.get().get(key), key, value);
   }

   public boolean exists (K key) {

      return cache.exists(KEY_LOCK_MAP_LOCAL.get().get(key), key);
   }

   public void clear () {

      cache.clear();
   }

   public boolean isClosed () {

      return cache.isClosed();
   }

   public void close () {

      cache.close();
   }
}
