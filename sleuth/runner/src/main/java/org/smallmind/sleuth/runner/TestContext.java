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
 * Thread-local context map shared across test executions within a thread and its children.
 */
public class TestContext {

  private static final InheritableThreadLocal<HashMap<String, Object>> CONTEXT_MAP = new InheritableThreadLocal<>() {

    @Override
    protected HashMap<String, Object> initialValue () {

      return new HashMap<>();
    }
  };

  /**
   * Retrieves a value by key without casting.
   *
   * @param key context key
   * @return stored value or {@code null} if absent
   */
  public static Object get (String key) {

    return CONTEXT_MAP.get().get(key);
  }

  /**
   * Retrieves and casts a value by key.
   *
   * @param key   context key
   * @param clazz expected type
   * @param <T>   inferred type parameter
   * @return value cast to the requested type or {@code null} if absent
   * @throws ClassCastException if the value is not of the expected type
   */
  public static <T> T get (String key, Class<T> clazz) {

    return clazz.cast(CONTEXT_MAP.get().get(key));
  }

  /**
   * Stores or replaces a context value.
   *
   * @param key   context key
   * @param value value to associate
   */
  public static void put (String key, Object value) {

    CONTEXT_MAP.get().put(key, value);
  }

  /**
   * Stores a context value only if the key is not already present.
   *
   * @param key   context key
   * @param value value to associate when absent
   */
  public static void putIfAbsent (String key, Object value) {

    CONTEXT_MAP.get().putIfAbsent(key, value);
  }
}
