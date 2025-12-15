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
 * A cache segmented by {@link ClassLoader} identity so entries are scoped to the loader
 * that produced the key. Loader segments are discarded automatically when the associated
 * {@link ClassLoader} is collected.
 *
 * @param <K> key type stored in the cache
 * @param <V> value type stored in the cache
 */
public class ClassLoaderAwareCache<K, V> {

  private final ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<>();
  private final ConcurrentHashMap<LoaderKey, ConcurrentHashMap<K, V>> loaderMap = new ConcurrentHashMap<>();
  private final Function<K, ClassLoader> loaderExtractor;

  /**
   * Creates a classloader-aware cache using the given extractor to derive the class loader
   * associated with each key. Keys resolving to {@code null} fall back to the system class loader.
   *
   * @param loaderExtractor function that extracts a {@link ClassLoader} from a key
   */
  public ClassLoaderAwareCache (Function<K, ClassLoader> loaderExtractor) {

    this.loaderExtractor = loaderExtractor;
  }

  /**
   * Retrieves a value associated with the supplied key within that key's class loader segment.
   *
   * @param key the cache key
   * @return the cached value, or {@code null} if absent
   */
  public V get (K key) {

    return getMap(key).get(key);
  }

  /**
   * Inserts or replaces a value within the class loader segment for the given key.
   *
   * @param key   the cache key
   * @param value the value to store
   * @return the previous value associated with the key, or {@code null} if none
   */
  public V put (K key, V value) {

    clearExpiredReferences();

    return getMap(key).put(key, value);
  }

  /**
   * Inserts a value only if the key is not already present in the class loader segment.
   *
   * @param key   the cache key
   * @param value the value to store
   * @return the existing value if present; otherwise {@code null}
   */
  public V putIfAbsent (K key, V value) {

    clearExpiredReferences();

    return getMap(key).putIfAbsent(key, value);
  }

  /**
   * Returns the map corresponding to the class loader derived from the provided key,
   * creating it lazily if absent.
   *
   * @param key the key used to locate the class loader segment
   * @return a concurrent map scoped to the key's class loader
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
   * Removes cache segments whose associated class loaders have been garbage collected.
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
   * Phantom reference keyed by class loader identity hash to support eviction when loaders are collected.
   */
  private class LoaderKey extends PhantomReference<ClassLoader> {

    private final int identityHashCode;

    /**
     * Creates a phantom reference entry for the given class loader.
     *
     * @param classLoader the loader to track for eviction
     */
    public LoaderKey (ClassLoader classLoader) {

      super(classLoader, referenceQueue);

      identityHashCode = System.identityHashCode(classLoader);
    }

    /**
     * Uses the identity hash code of the tracked loader to remain stable after collection.
     *
     * @return the identity hash code of the original loader
     */
    @Override
    public int hashCode () {

      return identityHashCode;
    }

    /**
     * Compares loader keys by stored identity hash.
     *
     * @param obj the object to compare
     * @return {@code true} when the other key has the same identity hash
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ClassLoaderAwareCache.LoaderKey) && (identityHashCode == obj.hashCode());
    }
  }
}
