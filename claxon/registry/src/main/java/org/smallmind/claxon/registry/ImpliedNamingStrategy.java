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
package org.smallmind.claxon.registry;

import java.util.Map;
import org.smallmind.nutsnbolts.util.DotNotation;

/**
 * A {@link NamingStrategy} that maps a caller class to a short metric prefix by finding
 * the most specific {@link DotNotation} pattern in a configured prefix map.
 *
 * <p>Each entry in the prefix map associates a {@link DotNotation} pattern with a string
 * prefix. When {@link #from(Class)} is called, every pattern is evaluated against the
 * fully qualified name of the caller class using
 * {@link DotNotation#calculateValue(String, int)}. The entry whose pattern yields the
 * highest positive score is selected and its associated prefix string is returned as the
 * meter name. If no pattern scores positively, or if the prefix map is empty or
 * {@code null}, {@code null} is returned, which causes the registry to substitute a
 * {@link NoOpMeter}.
 *
 * <p>This is the default {@link NamingStrategy} used by {@link ClaxonConfiguration}.
 *
 * @see ObviousNamingStrategy
 */
public class ImpliedNamingStrategy implements NamingStrategy {

  /**
   * Map of {@link DotNotation} patterns to their associated metric name prefixes.
   * May be {@code null} when no prefixes have been configured.
   */
  private Map<DotNotation, String> prefixMap;

  /**
   * Creates a strategy with no prefix map configured. All calls to {@link #from(Class)}
   * will return {@code null} until {@link #setPrefixMap(Map)} is called.
   */
  public ImpliedNamingStrategy () {

  }

  /**
   * Supplies the map of {@link DotNotation} patterns to metric name prefixes used by
   * this strategy.
   *
   * @param prefixMap a map whose keys are {@link DotNotation} patterns and whose values
   *                  are the metric name prefixes to return for matching callers
   * @return this strategy instance to support fluent chaining
   */
  public ImpliedNamingStrategy setPrefixMap (Map<DotNotation, String> prefixMap) {

    this.prefixMap = prefixMap;

    return this;
  }

  /**
   * Evaluates all configured patterns against the fully qualified name of {@code caller}
   * and returns the prefix associated with the most specifically matching pattern.
   *
   * <p>Pattern specificity is measured by the score returned from
   * {@link DotNotation#calculateValue(String, int)}; a higher score indicates a more
   * specific match. When two patterns score equally, the one encountered first in map
   * iteration order wins. {@code null} is returned when the map is empty, unset, or when
   * no pattern produces a positive score.
   *
   * @param caller the class for which a metric name prefix is requested
   * @return the prefix string for the best-matching pattern, or {@code null} if no
   * pattern matches
   */
  @Override
  public String from (Class<?> caller) {

    if ((prefixMap != null) && (!prefixMap.isEmpty())) {

      Map.Entry<DotNotation, String> strongestEntry = null;
      int currentStrength = 0;

      for (Map.Entry<DotNotation, String> prefixEntry : prefixMap.entrySet()) {

        int possibleStrength;

        if ((possibleStrength = prefixEntry.getKey().calculateValue(caller.getName(), -1)) > currentStrength) {
          currentStrength = possibleStrength;
          strongestEntry = prefixEntry;
        }
      }

      if (strongestEntry != null) {
        return strongestEntry.getValue();
      }
    }

    return null;
  }
}
