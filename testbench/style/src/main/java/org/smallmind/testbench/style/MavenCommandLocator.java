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
package org.smallmind.testbench.style;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Resolves the path to the Maven executable by running the platform's {@code where} command and
 * reading its first line of output. {@link DependencyReducer} uses this to find the {@code mvn}
 * launcher before invoking it per module. Both methods return {@code null} when Maven is not on the
 * PATH.
 */
public class MavenCommandLocator {

  /**
   * Locates the Maven launcher on Windows by running {@code where.exe mvn.cmd}.
   *
   * @param commandDir the working directory for the probe command
   * @return the path to {@code mvn.cmd}, or {@code null} if it is not found on the PATH
   * @throws IOException if {@code where.exe} cannot be executed
   */
  public static String inWindows (Path commandDir)
    throws IOException {

    ByteArrayOutputStream buffer = ProcessOutputUtility.buffer(commandDir, "where.exe", "mvn.cmd");
    String result;

    if (((result = buffer.toString()) == null) || result.isBlank() || result.startsWith("INFO: ")) {

      return null;
    } else {

      return result.strip();
    }
  }

  /**
   * Locates the Maven launcher on Linux by running {@code where mvn}.
   *
   * @param commandDir the working directory for the probe command
   * @return the path to {@code mvn}, or {@code null} if it is not found on the PATH
   * @throws IOException if {@code where} cannot be executed
   */
  public static String inLinux (Path commandDir)
    throws IOException {

    ByteArrayOutputStream buffer = ProcessOutputUtility.buffer(commandDir, "where", "mvn");
    String result;

    if (((result = buffer.toString()) == null) || result.isBlank() || result.startsWith("INFO: ")) {

      return null;
    } else {

      return result.strip();
    }
  }
}
