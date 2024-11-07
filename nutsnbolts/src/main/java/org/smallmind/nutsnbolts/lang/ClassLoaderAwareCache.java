/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class ClassLoaderAwareCache<K, V> {

  private final ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<>();
  private final ConcurrentHashMap<LoaderKey, ConcurrentHashMap<K, V>> loaderMap = new ConcurrentHashMap<>();
  private final Function<K, ClassLoader> loaderExtractor;

  public ClassLoaderAwareCache (Function<K, ClassLoader> loaderExtractor) {

    this.loaderExtractor = loaderExtractor;
  }

  public V get (K key) {

    return getMap(key).get(key);
  }

  public V put (K key, V value) {

    clearExpiredReferences();

    return getMap(key).put(key, value);
  }

  public V putIfAbsent (K key, V value) {

    clearExpiredReferences();

    return getMap(key).putIfAbsent(key, value);
  }

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

  private void clearExpiredReferences () {

    Reference<? extends ClassLoader> reference;

    while ((reference = referenceQueue.poll()) != null) {
      if (reference instanceof ClassLoaderAwareCache.LoaderKey) {
        loaderMap.remove(reference);
      }
    }
  }

  private class LoaderKey extends PhantomReference<ClassLoader> {

    private final int identityHashCode;

    public LoaderKey (ClassLoader classLoader) {

      super(classLoader, referenceQueue);

      identityHashCode = System.identityHashCode(classLoader);
    }

    @Override
    public int hashCode () {

      return identityHashCode;
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ClassLoaderAwareCache.LoaderKey) && (identityHashCode == obj.hashCode());
    }
  }
}
