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
 * Naming strategy that returns the fully qualified class name when it matches a whitelist.
 */
public class ObviousNamingStrategy implements NamingStrategy {

  private Set<DotNotation> whiteListSet = new HashSet<>();

  /**
   * Creates a strategy with an empty whitelist.
   */
  public ObviousNamingStrategy () {

  }

  /**
   * Creates a strategy using the provided whitelist.
   *
   * @param whiteListSet allowed caller patterns
   */
  public ObviousNamingStrategy (Set<DotNotation> whiteListSet) {

    this.whiteListSet = whiteListSet;
  }

  /**
   * Replaces the whitelist of allowed caller patterns.
   *
   * @param whiteListSet new whitelist
   * @return this strategy for chaining
   */
  public ObviousNamingStrategy setWhiteListSet (Set<DotNotation> whiteListSet) {

    this.whiteListSet = whiteListSet;

    return this;
  }

  /**
   * Returns the caller class name when the whitelist includes a matching pattern.
   *
   * @param caller class requesting a meter name
   * @return fully qualified class name or {@code null} if not permitted
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
