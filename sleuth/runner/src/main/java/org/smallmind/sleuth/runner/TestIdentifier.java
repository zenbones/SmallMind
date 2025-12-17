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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates stable numeric identifiers for test class/method combinations.
 * <p>
 * Identifiers are stored in an inheritable thread-local to ensure child threads report under the correct test id.
 */
public class TestIdentifier {

  private static final InheritableThreadLocal<Long> IDENTIFIER_LOCAL = new InheritableThreadLocal<>() {

    @Override
    protected Long initialValue () {

      return 1L;
    }
  };

  private static final HashMap<TestKey, Long> IDENTIFIER_MAP = new HashMap<>();
  private static final AtomicLong IDENTIFIER_COUNTER = new AtomicLong(0);

  /**
   * Updates the current thread's identifier to the value associated with the given class/method, creating one if needed.
   *
   * @param className  fully qualified class name
   * @param methodName method name; may be {@code null} for suite-level identifiers
   */
  public static void updateIdentifier (String className, String methodName) {

    TestKey testKey = new TestKey(className, methodName);
    Long fixedIdentifier;

    if ((fixedIdentifier = IDENTIFIER_MAP.get(testKey)) == null) {
      synchronized (IDENTIFIER_MAP) {
        if ((fixedIdentifier = IDENTIFIER_MAP.get(testKey)) == null) {
          IDENTIFIER_MAP.put(testKey, fixedIdentifier = IDENTIFIER_COUNTER.incrementAndGet());
        }
      }
    }

    IDENTIFIER_LOCAL.set(fixedIdentifier);
  }

  /**
   * @return identifier assigned to the current thread's active test
   */
  public static long getTestIdentifier () {

    return IDENTIFIER_LOCAL.get();
  }

  /**
   * Composite key for identifier lookup.
   */
  private static class TestKey {

    private final String className;
    private final String methodName;

    /**
     * @param className  fully qualified class name
     * @param methodName method name; may be {@code null}
     */
    public TestKey (String className, String methodName) {

      this.className = className;
      this.methodName = methodName;
    }

    public String getClassName () {

      return className;
    }

    /**
     * @return method name component; may be {@code null}
     */
    public String getMethodName () {

      return methodName;
    }

    /**
     * @return combined hash of class and method names
     */
    @Override
    public int hashCode () {

      return (((className == null) ? 0 : className.hashCode()) * 31) + ((methodName == null) ? 0 : methodName.hashCode());
    }

    /**
     * Equality is based on both class and method name.
     *
     * @param obj object to compare
     * @return {@code true} when both parts match
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof TestKey) && ((TestKey)obj).className.equals(className) && ((TestKey)obj).methodName.equals(methodName);
    }
  }
}
