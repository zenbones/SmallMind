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
package org.smallmind.nutsnbolts.spring;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;

/**
 * Evaluates property keys against a set of dot-notation include and exclude patterns to determine whether debug logging should apply.
 */
public class KeyDebugger {

  private final LinkedList<DebugMatcher> matcherList;
  boolean debug = false;

  /**
   * Compiles the supplied patterns into matchers; patterns prefixed with {@code -} are treated as exclusions
   * and all others as inclusions.
   *
   * @param patterns dot-notation patterns governing which keys to debug
   * @throws DotNotationException if any pattern cannot be compiled
   */
  public KeyDebugger (String[] patterns)
    throws DotNotationException {

    DebugMatcher debugMatcher;

    matcherList = new LinkedList<>();
    for (String pattern : patterns) {
      matcherList.add(debugMatcher = new DebugMatcher(pattern));
      if (!debugMatcher.isExclusion()) {
        debug = true;
      }
    }
  }

  /**
   * Returns whether any inclusion patterns were provided, indicating that debug logging is active.
   *
   * @return {@code true} if at least one inclusion pattern exists
   */
  public boolean willDebug () {

    return debug;
  }

  /**
   * Determines whether the given property key should be logged by testing it against all inclusion and exclusion patterns.
   *
   * @param key the property key to evaluate
   * @return {@code true} if an inclusion pattern matches and no exclusion pattern overrides it
   */
  public boolean matches (String key) {

    boolean match = false;

    for (DebugMatcher debugMatcher : matcherList) {
      if (debugMatcher.matches(key)) {
        if (debugMatcher.isExclusion()) {

          return false;
        } else {
          match = true;
        }
      }
    }

    return match;
  }

  /**
   * Wraps a compiled {@link DotNotation} pattern and tracks whether it represents an exclusion rule.
   */
  private static class DebugMatcher {

    private final DotNotation dotNotation;
    private boolean exclusion = false;

    /**
     * Compiles the pattern, stripping a leading {@code -} prefix and marking the matcher as an exclusion when present.
     *
     * @param pattern a dot-notation pattern, optionally prefixed with {@code -} to indicate exclusion
     * @throws DotNotationException if the pattern cannot be compiled
     */
    public DebugMatcher (String pattern)
      throws DotNotationException {

      if (pattern.charAt(0) == '-') {
        exclusion = true;
        dotNotation = new DotNotation(pattern.substring(1));
      } else {
        dotNotation = new DotNotation(pattern);
      }
    }

    /**
     * Returns whether this matcher represents an exclusion rule.
     *
     * @return {@code true} if this matcher excludes matching keys from debug logging
     */
    public boolean isExclusion () {

      return exclusion;
    }

    /**
     * Tests whether the given key matches this pattern.
     *
     * @param key the property key to test
     * @return {@code true} if the key matches the compiled dot-notation pattern
     */
    public boolean matches (String key) {

      return dotNotation.getPattern().matcher(key).matches();
    }
  }
}
