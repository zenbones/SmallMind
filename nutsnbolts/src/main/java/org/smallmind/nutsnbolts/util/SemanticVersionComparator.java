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
package org.smallmind.nutsnbolts.util;

import java.util.Comparator;

/**
 * Compares dot-delimited semantic version strings component by component as integers.
 */
public class SemanticVersionComparator implements Comparator<String> {

  /**
   * Treats {@code null} or empty strings as less than non-empty versions. Compares each dot-separated segment numerically;
   * if one version has additional segments after a common prefix, it sorts after the shorter version.
   *
   * @param version1 first version string
   * @param version2 second version string
   * @return comparison result per {@link Comparator} contract
   */
  @Override
  public int compare (String version1, String version2) {

    if (version1 == null) {

      return (version2 == null) ? 0 : -1;
    } else if (version2 == null) {

      return 1;
    } else if (version1.isEmpty()) {

      return version2.isEmpty() ? 0 : -1;
    } else if (version2.isEmpty()) {

      return 1;
    } else {

      String[] segments1 = version1.split("\\.");
      String[] segments2 = version2.split("\\.");

      for (int index = 0; index < segments1.length; index++) {
        if (index >= segments2.length) {

          return 1;
        } else {

          int result = Integer.parseInt(segments1[index]) - Integer.parseInt(segments2[index]);

          if (result != 0) {

            return result;
          }
        }
      }

      return (segments1.length == segments2.length) ? 0 : -1;
    }
  }
}
