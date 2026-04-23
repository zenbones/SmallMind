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
package org.smallmind.nutsnbolts.io;

import java.nio.file.Path;

/**
 * Utility methods for converting {@link Path} objects to normalized, cross-platform string representations.
 */
public class PathUtility {

  /**
   * Returns the file-name element of the given path as a string.
   *
   * @param path the path whose final name component is desired
   * @return the file name as a string
   */
  public static String fileNameAsString (Path path) {

    return path.getFileName().toString();
  }

  /**
   * Returns the path as a string with platform file separators replaced by forward slashes.
   *
   * @param path the path to convert
   * @return path string suitable for use as a resource path
   */
  public static String asResourceString (Path path) {

    return path.toString().replace(System.getProperty("file.separator"), "/");
  }

  /**
   * Resolves the path to its absolute, normalized form and returns it as a forward-slash string.
   *
   * @param path the path to normalize
   * @return absolute normalized path string with {@code /} separators
   */
  public static String asNormalizedString (Path path) {

    return path.toAbsolutePath().normalize().toString().replace(System.getProperty("file.separator"), "/");
  }
}
