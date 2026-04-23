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
 * Thread-safe cache keyed by {@link Method} and segmented by the defining class's {@link ClassLoader},
 * automatically expunging stale segments when their associated loader is garbage collected.
 *
 * @param <T> the type of value stored for each method
 */
public class LoaderAwareMethodCache<T> {

  private final ReferenceQueue<ClassLoader> referenceQueue = new ReferenceQueue<>();
  private final ConcurrentHashMap<LoaderKey, ConcurrentHashMap<Method, T>> loaderMap = new ConcurrentHashMap<>();

  /**
   * Returns the cached value for the given method, or {@code null} if no entry exists.
   *
   * @param method the method whose cached value is requested
   * @return the cached value, or {@code null} if absent
   */
  public T get (Method method) {

    return getMethodMap(method).get(method);
  }

  /**
   * Stores a value for the given method, replacing any previously cached value.
   *
   * @param method the method to use as the cache key
   * @param value  the value to associate with the method
   * @return the previous value associated with the method, or {@code null} if none existed
   */
  public T put (Method method, T value) {

    clearExpiredReferences();

    return getMethodMap(method).put(method, value);
  }

  /**
   * Stores a value for the given method only if no value is already present.
   *
   * @param method the method to use as the cache key
   * @param value  the value to store if the method is not already cached
   * @return the existing value if one was already present, or {@code null} if the new value was stored
   */
  public T putIfAbsent (Method method, T value) {

    clearExpiredReferences();

    return getMethodMap(method).putIfAbsent(method, value);
  }

  /**
   * Returns the method-to-value map for the class loader that defined the given method, creating an
   * empty map and registering it if one does not yet exist.
   *
   * @param method the method whose declaring class's loader identifies the segment
   * @return the concurrent map for that loader segment
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
   * Polls the reference queue and removes any loader segments whose class loaders have been
   * garbage collected.
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
   * Phantom reference that serves as the map key for a class loader segment, enabling automatic
   * removal of the segment once the loader is garbage collected.
   */
  private class LoaderKey extends PhantomReference<ClassLoader> {

    private final int identityHashCode;

    /**
     * Constructs a key that tracks the given class loader via the enclosing cache's reference queue.
     *
     * @param classLoader the class loader to monitor for collection
     */
    public LoaderKey (ClassLoader classLoader) {

      super(classLoader, referenceQueue);

      identityHashCode = System.identityHashCode(classLoader);
    }

    /**
     * Returns the identity hash code of the tracked class loader captured at construction time.
     *
     * @return the identity hash code of the tracked loader
     */
    @Override
    public int hashCode () {

      return identityHashCode;
    }

    /**
     * Returns {@code true} if the other object is a {@code LoaderKey} with the same identity hash code.
     *
     * @param obj the object to compare with this key
     * @return {@code true} if {@code obj} is a {@code LoaderKey} whose identity hash code matches
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof LoaderAwareMethodCache.LoaderKey) && (identityHashCode == obj.hashCode());
    }
  }
}
