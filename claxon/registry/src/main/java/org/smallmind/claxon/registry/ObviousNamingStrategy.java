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

import java.util.HashSet;
import java.util.Set;
import org.smallmind.nutsnbolts.util.DotNotation;

/**
 * A {@link NamingStrategy} that uses the fully qualified class name as the meter name,
 * but only for callers whose class name matches at least one pattern in a configurable
 * whitelist of {@link DotNotation} expressions.
 *
 * <p>Callers that do not match any whitelist pattern receive {@code null} from
 * {@link #from(Class)}, causing the registry to substitute a {@link NoOpMeter}.
 * Inner-class separators ({@code $}) are normalised to dots in the returned name.
 *
 * <p>When the whitelist is empty or {@code null}, {@link #from(Class)} always returns
 * {@code null}, effectively disabling all meter registration through this strategy.
 *
 * @see ImpliedNamingStrategy
 */
public class ObviousNamingStrategy implements NamingStrategy {

  /**
   * The set of {@link DotNotation} patterns that identify permitted caller classes.
   */
  private Set<DotNotation> whiteListSet = new HashSet<>();

  /**
   * Creates a strategy with an empty whitelist. No callers are permitted until
   * {@link #setWhiteListSet(Set)} is called.
   */
  public ObviousNamingStrategy () {

  }

  /**
   * Creates a strategy pre-loaded with the given whitelist of permitted caller patterns.
   *
   * @param whiteListSet the initial set of {@link DotNotation} patterns that identify
   *                     permitted caller classes
   */
  public ObviousNamingStrategy (Set<DotNotation> whiteListSet) {

    this.whiteListSet = whiteListSet;
  }

  /**
   * Replaces the whitelist of permitted caller patterns.
   *
   * @param whiteListSet the new set of {@link DotNotation} patterns; must not be
   *                     {@code null} if any caller is to be permitted
   * @return this strategy instance to support fluent chaining
   */
  public ObviousNamingStrategy setWhiteListSet (Set<DotNotation> whiteListSet) {

    this.whiteListSet = whiteListSet;

    return this;
  }

  /**
   * Returns the fully qualified name of {@code caller} (with {@code $} replaced by
   * {@code .}) when the caller's class name matches at least one pattern in the whitelist,
   * or {@code null} when no pattern matches.
   *
   * <p>Pattern matching is performed using {@link DotNotation#calculateValue}; a positive
   * score indicates a match.
   *
   * @param caller the class for which a meter name is requested
   * @return the normalised fully qualified class name if permitted, or {@code null} if
   * the caller does not match any whitelist pattern
   */
  @Override
  public String from (Class<?> caller) {

    if ((whiteListSet != null) && (!whiteListSet.isEmpty())) {
      for (DotNotation dotNotation : whiteListSet) {
        if (dotNotation.calculateValue(caller.getName(), -1) > 0) {

          return caller.getName().replace('$', '.');
        }
      }
    }

    return null;
  }
}
