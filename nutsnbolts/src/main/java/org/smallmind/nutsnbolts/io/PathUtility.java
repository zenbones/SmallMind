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
 * Utilities for converting {@link Path} values to normalized string forms.
 */
public class PathUtility {

  /**
   * @param path path whose file name will be returned
   * @return file name component as a string
   */
  public static String fileNameAsString (Path path) {

    return path.getFileName().toString();
  }

  /**
   * Converts a path to a resource-friendly string using forward slashes.
   *
   * @param path path to convert
   * @return path string with separators replaced by '/'
   */
  public static String asResourceString (Path path) {

    return path.toString().replace(System.getProperty("file.separator"), "/");
  }

  /**
   * Normalizes a path to absolute form and returns a forward-slash string.
   *
   * @param path path to normalize
   * @return normalized absolute path with '/' separators
   */
  public static String asNormalizedString (Path path) {

    return path.toAbsolutePath().normalize().toString().replace(System.getProperty("file.separator"), "/");
  }
}
