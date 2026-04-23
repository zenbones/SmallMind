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
package org.smallmind.sleuth.runner;

import java.util.HashMap;

/**
 * Thread-local key/value store accessible to test code running on Sleuth-managed threads.
 * <p>
 * The backing map is stored in an {@link InheritableThreadLocal} so that values written by a suite
 * runner thread are visible to child test runner threads without requiring explicit coordination.
 * All methods are static; instantiation is not intended.
 * <p>
 * Typical usage is to share fixtures or configuration between {@code @BeforeSuite} setup code and
 * the test methods that depend on it.
 */
public class TestContext {

  private static final InheritableThreadLocal<HashMap<String, Object>> CONTEXT_MAP = new InheritableThreadLocal<>() {

    @Override
    protected HashMap<String, Object> initialValue () {

      return new HashMap<>();
    }
  };

  /**
   * Retrieves the value associated with {@code key} without type casting.
   *
   * @param key lookup key; must not be {@code null}
   * @return the associated value, or {@code null} if no mapping exists
   */
  public static Object get (String key) {

    return CONTEXT_MAP.get().get(key);
  }

  /**
   * Retrieves and casts the value associated with {@code key} to the requested type.
   *
   * @param key   lookup key; must not be {@code null}
   * @param clazz expected type of the stored value; must not be {@code null}
   * @param <T>   inferred return type
   * @return the value cast to {@code T}, or {@code null} if no mapping exists
   * @throws ClassCastException if the stored value is not assignable to {@code clazz}
   */
  public static <T> T get (String key, Class<T> clazz) {

    return clazz.cast(CONTEXT_MAP.get().get(key));
  }

  /**
   * Stores or replaces the value for {@code key}.
   *
   * @param key   storage key; must not be {@code null}
   * @param value value to associate; may be {@code null}
   */
  public static void put (String key, Object value) {

    CONTEXT_MAP.get().put(key, value);
  }

  /**
   * Stores a value for {@code key} only if the key is not already present.
   * <p>
   * Equivalent to a conditional put that does not overwrite an existing mapping.
   *
   * @param key   storage key; must not be {@code null}
   * @param value value to associate when the key is absent; may be {@code null}
   */
  public static void putIfAbsent (String key, Object value) {

    CONTEXT_MAP.get().putIfAbsent(key, value);
  }
}
