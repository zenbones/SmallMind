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
 * Assigns and tracks stable numeric identifiers for class/method pairs across a test run.
 * <p>
 * Each unique {@code (className, methodName)} pair is allocated a monotonically increasing
 * {@code long} identifier the first time it is seen. The identifier is stored in an
 * {@link InheritableThreadLocal} so that child threads (e.g., test runner threads spawned by a
 * suite runner) automatically inherit the active identifier. Callers retrieve the current
 * thread's identifier via {@link #getTestIdentifier()} and update it when switching to a new
 * test via {@link #updateIdentifier(String, String)}.
 * <p>
 * The identifier is primarily used by the Surefire integration to correlate output entries with
 * the correct test case in the Surefire report.
 * <p>
 * This class is thread-safe; identifier allocation is double-checked with synchronization.
 * All methods are static; instantiation is not intended.
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
   * Sets the current thread's active identifier to the one assigned to the given class/method pair,
   * creating a new identifier if this pair has not been seen before.
   * <p>
   * Allocation uses double-checked locking: the volatile map is checked unsynchronised first and
   * only synchronised on a miss to keep the common path cheap.
   *
   * @param className  fully qualified class name; must not be {@code null}
   * @param methodName method name within the class; may be {@code null} for suite-level identifiers
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
   * Returns the identifier currently active on this thread.
   * <p>
   * The initial value for a new thread is {@code 1}. The value is updated by calling
   * {@link #updateIdentifier(String, String)} and is inherited by child threads.
   *
   * @return current thread-local test identifier; never {@code null}
   */
  public static long getTestIdentifier () {

    return IDENTIFIER_LOCAL.get();
  }

  /**
   * Composite key combining class name and method name for identifier map lookups.
   */
  private static class TestKey {

    private final String className;
    private final String methodName;

    /**
     * Constructs a key for the given class/method pair.
     *
     * @param className  fully qualified class name; must not be {@code null}
     * @param methodName method name; may be {@code null} for suite-level entries
     */
    public TestKey (String className, String methodName) {

      this.className = className;
      this.methodName = methodName;
    }

    /**
     * @return combined hash of class and method name components
     */
    @Override
    public int hashCode () {

      return (((className == null) ? 0 : className.hashCode()) * 31) + ((methodName == null) ? 0 : methodName.hashCode());
    }

    /**
     * Two keys are equal when both their class and method name components match.
     * <p>
     * The method name component is {@code null} for suite-level identifiers, so the comparison is
     * null-safe on both components, matching the nullability handled by {@link #hashCode()}.
     *
     * @param obj object to compare; may be {@code null}
     * @return {@code true} if both components are identical
     */
    @Override
    public boolean equals (Object obj) {

      if (!(obj instanceof TestKey)) {

        return false;
      }

      TestKey testKey = (TestKey)obj;

      return ((className == null) ? (testKey.className == null) : className.equals(testKey.className)) && ((methodName == null) ? (testKey.methodName == null) : methodName.equals(testKey.methodName));
    }
  }
}
