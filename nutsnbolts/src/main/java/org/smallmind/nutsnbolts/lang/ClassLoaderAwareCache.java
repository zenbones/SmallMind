/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.nutsnbolts.lang;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A concurrent cache partitioned by {@link ClassLoader} identity, so that entries are automatically evicted when their associated loader is garbage-collected.
 *
 * @param <K> the type of keys stored in the cache
 * @param <V> the type of values stored in the cache
 */
public class ClassLoaderAwareCache<K, V> {

  private final ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<>();
  private final ConcurrentHashMap<LoaderKey, ConcurrentHashMap<K, V>> loaderMap = new ConcurrentHashMap<>();
  private final Function<K, ClassLoader> loaderExtractor;

  /**
   * Creates a cache that uses the supplied function to determine which {@link ClassLoader} segment each key belongs to.
   * Keys for which the extractor returns {@code null} are placed in the system class loader segment.
   *
   * @param loaderExtractor a function that maps a cache key to its associated {@link ClassLoader}
   */
  public ClassLoaderAwareCache (Function<K, ClassLoader> loaderExtractor) {

    this.loaderExtractor = loaderExtractor;
  }

  /**
   * Returns the cached value for the given key within the key's class loader segment, or {@code null} if absent.
   *
   * @param key the cache key
   * @return the cached value, or {@code null} if no mapping exists
   */
  public V get (K key) {

    return getMap(key).get(key);
  }

  /**
   * Associates the given value with the key in the key's class loader segment, replacing any existing mapping.
   *
   * @param key   the cache key
   * @param value the value to associate with the key
   * @return the previous value associated with the key, or {@code null} if there was no prior mapping
   */
  public V put (K key, V value) {

    clearExpiredReferences();

    return getMap(key).put(key, value);
  }

  /**
   * Associates the given value with the key in the key's class loader segment only if no mapping currently exists.
   *
   * @param key   the cache key
   * @param value the value to store if no mapping is present
   * @return the existing value if one was already present, or {@code null} if the new value was stored
   */
  public V putIfAbsent (K key, V value) {

    clearExpiredReferences();

    return getMap(key).putIfAbsent(key, value);
  }

  /**
   * Returns the inner map for the class loader segment associated with the given key, creating it lazily if needed.
   *
   * @param key the key whose class loader segment is required
   * @return the concurrent map for the segment corresponding to the key's class loader
   */
  private synchronized ConcurrentHashMap<K, V> getMap (K key) {

    ConcurrentHashMap<K, V> map;
    ClassLoader extractedClassLoader;
    LoaderKey loaderKey = new LoaderKey(((extractedClassLoader = loaderExtractor.apply(key)) == null) ? ClassLoader.getSystemClassLoader() : extractedClassLoader);

    if ((map = loaderMap.get(loaderKey)) == null) {

      ConcurrentHashMap<K, V> priorMap;

      if ((priorMap = loaderMap.putIfAbsent(loaderKey, map = new ConcurrentHashMap<>())) != null) {
        map = priorMap;
      }
    }

    return map;
  }

  /**
   * Polls the reference queue and removes any cache segments whose associated class loaders have been garbage-collected.
   */
  private void clearExpiredReferences () {

    Reference<? extends ClassLoader> reference;

    while ((reference = referenceQueue.poll()) != null) {
      if (reference instanceof ClassLoaderAwareCache.LoaderKey) {
        loaderMap.remove(reference);
      }
    }
  }

  /**
   * A phantom reference to a {@link ClassLoader} that also serves as the map key, allowing the cache segment to be removed once the loader is garbage-collected.
   */
  private class LoaderKey extends PhantomReference<ClassLoader> {

    private final int identityHashCode;

    /**
     * Creates a {@code LoaderKey} that tracks {@code classLoader} via a phantom reference registered on the enclosing cache's reference queue.
     *
     * @param classLoader the class loader to track
     */
    public LoaderKey (ClassLoader classLoader) {

      super(classLoader, referenceQueue);

      identityHashCode = System.identityHashCode(classLoader);
    }

    /**
     * Returns the identity hash code captured at construction time, remaining stable even after the referent is collected.
     *
     * @return the identity hash code of the original class loader
     */
    @Override
    public int hashCode () {

      return identityHashCode;
    }

    /**
     * Returns {@code true} when {@code obj} is a {@code LoaderKey} with the same identity hash code.
     *
     * @param obj the object to compare
     * @return {@code true} if the other key has an equal identity hash code
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ClassLoaderAwareCache.LoaderKey) && (identityHashCode == obj.hashCode());
    }
  }
}
