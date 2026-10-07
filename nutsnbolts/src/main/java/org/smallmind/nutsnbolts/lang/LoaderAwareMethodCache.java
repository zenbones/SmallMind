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

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache keyed by {@link Method} and partitioned by the method's declaring class. Each partition is
 * attached to its class through a {@link ClassValue}, so the cache never keeps a class, or its class loader,
 * reachable: a partition and its entries become collectible along with the class. A value that references classes
 * from a different class loader keeps that loader reachable for as long as the declaring class is reachable.
 *
 * @param <T> the type of value stored for each method
 */
public class LoaderAwareMethodCache<T> {

  private final ClassValue<ConcurrentHashMap<Method, T>> partitionValue = new ClassValue<>() {

    @Override
    protected ConcurrentHashMap<Method, T> computeValue (Class<?> type) {

      return new ConcurrentHashMap<>();
    }
  };

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

    return getMethodMap(method).putIfAbsent(method, value);
  }

  /**
   * Returns the method-to-value map for the class that declares the given method, creating it lazily if needed.
   *
   * @param method the method whose declaring class identifies the partition
   * @return the concurrent map for that partition
   */
  private ConcurrentHashMap<Method, T> getMethodMap (Method method) {

    return partitionValue.get(method.getDeclaringClass());
  }
}
