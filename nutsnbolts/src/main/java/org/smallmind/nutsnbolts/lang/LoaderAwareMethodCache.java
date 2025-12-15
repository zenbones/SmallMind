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
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache keyed by {@link Method}, segmented by the defining class's {@link ClassLoader}. Segments are
 * cleaned up automatically when the associated loader is collected.
 *
 * @param <T> value type stored for each method
 */
public class LoaderAwareMethodCache<T> {

  private final ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<>();
  private final ConcurrentHashMap<LoaderKey, ConcurrentHashMap<Method, T>> loaderMap = new ConcurrentHashMap<>();

  /**
   * Retrieves a cached value for the given method in its class loader segment.
   *
   * @param method the method key
   * @return the cached value, or {@code null} if absent
   */
  public T get (Method method) {

    return getMethodMap(method).get(method);
  }

  /**
   * Adds or replaces a value for the supplied method within its loader segment.
   *
   * @param method the method key
   * @param value  the value to store
   * @return the prior value associated with the method, or {@code null} if none
   */
  public T put (Method method, T value) {

    clearExpiredReferences();

    return getMethodMap(method).put(method, value);
  }

  /**
   * Adds a value for the supplied method only if one is not already present.
   *
   * @param method the method key
   * @param value  the value to store if absent
   * @return the existing value if present; otherwise {@code null}
   */
  public T putIfAbsent (Method method, T value) {

    clearExpiredReferences();

    return getMethodMap(method).putIfAbsent(method, value);
  }

  /**
   * Retrieves the map corresponding to the method's defining class loader, creating it if absent.
   *
   * @param method the method whose loader determines the segment
   * @return a concurrent map for the loader
   */
  private ConcurrentHashMap<Method, T> getMethodMap (Method method) {

    ConcurrentHashMap<Method, T> methodMap;
    LoaderKey loaderKey = new LoaderKey(method.getDeclaringClass().getClassLoader());

    if ((methodMap = loaderMap.get(loaderKey)) == null) {

      ConcurrentHashMap<Method, T> priorMethodMap;

      if ((priorMethodMap = loaderMap.putIfAbsent(loaderKey, methodMap = new ConcurrentHashMap<>())) != null) {
        methodMap = priorMethodMap;
      }
    }

    return methodMap;
  }

  /**
   * Removes loader segments whose associated class loaders have been collected.
   */
  private void clearExpiredReferences () {

    Reference<?> reference;

    while ((reference = referenceQueue.poll()) != null) {
      if (reference instanceof LoaderAwareMethodCache.LoaderKey) {
        loaderMap.remove((LoaderAwareMethodCache<T>.LoaderKey)reference);
      }
    }
  }

  /**
   * Phantom reference keyed by loader identity to support cleanup when loaders are collected.
   */
  private class LoaderKey extends PhantomReference<ClassLoader> {

    private final int identityHashCode;

    /**
     * Creates a key that tracks the supplied loader for cleanup.
     *
     * @param classLoader the loader to monitor
     */
    public LoaderKey (ClassLoader classLoader) {

      super(classLoader, referenceQueue);

      identityHashCode = System.identityHashCode(classLoader);
    }

    /**
     * Uses the loader's identity hash code for stable hashing.
     *
     * @return the identity hash code of the tracked loader
     */
    @Override
    public int hashCode () {

      return identityHashCode;
    }

    /**
     * Compares loader keys by stored identity hash code.
     *
     * @param obj the object to compare
     * @return {@code true} when the identity hash codes match
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof LoaderAwareMethodCache.LoaderKey) && (identityHashCode == obj.hashCode());
    }
  }
}
