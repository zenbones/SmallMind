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
package org.smallmind.forge.deploy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Supported target operating systems for application deployment, each encapsulating the
 * platform-specific behavior for setting file execute permissions and the filename extension
 * used by service control wrapper scripts.
 */
public enum OperatingSystem {

  /**
   * Linux deployment target; applies POSIX permission mode 755 and uses {@code .sh} script extensions.
   */
  LINUX("linux", ".sh") {
    @Override
    public void makeExecutable (Path path)
      throws IOException {

      Files.setPosixFilePermissions(path, PERMISSIONS_755);
    }
  },
  /**
   * Windows deployment target; execute-bit setting is a no-op and service scripts use {@code .bat} extensions.
   */
  WINDOWS("windows", ".bat") {
    @Override
    public void makeExecutable (Path path) {

    }
  };

  private static final Set<PosixFilePermission> PERMISSIONS_755;
  private final String code;
  private final String batchExtension;

  static {

    PERMISSIONS_755 = new HashSet<>();

    PERMISSIONS_755.add(PosixFilePermission.OWNER_READ);
    PERMISSIONS_755.add(PosixFilePermission.OWNER_WRITE);
    PERMISSIONS_755.add(PosixFilePermission.OWNER_EXECUTE);

    PERMISSIONS_755.add(PosixFilePermission.GROUP_READ);
    PERMISSIONS_755.add(PosixFilePermission.GROUP_EXECUTE);

    PERMISSIONS_755.add(PosixFilePermission.OTHERS_READ);
    PERMISSIONS_755.add(PosixFilePermission.OTHERS_EXECUTE);
  }

  /**
   * Initialise the enum constant with its short code and script extension.
   *
   * @param code           identifier used in CLI arguments (e.g. {@code linux})
   * @param batchExtension filename extension for service control scripts (e.g. {@code .sh})
   */
  OperatingSystem (String code, String batchExtension) {

    this.code = code;
    this.batchExtension = batchExtension;
  }

  /**
   * Look up an operating system by its short code.
   *
   * @param code platform identifier, e.g. {@code linux} or {@code windows}
   * @return the matching constant, or {@code null} if no constant carries that code
   */
  public static OperatingSystem fromCode (String code) {

    for (OperatingSystem operatingSystem : OperatingSystem.values()) {
      if (operatingSystem.getCode().equals(code)) {

        return operatingSystem;
      }
    }

    return null;
  }

  /**
   * Apply the platform-appropriate execute permissions to {@code path}.
   *
   * <p>On Linux, sets POSIX permission mode 755 (owner rwx, group rx, others rx). On Windows,
   * this method is a no-op because the OS does not use POSIX file permissions.
   *
   * @param path the file to make executable
   * @throws IOException if the permission change cannot be applied (Linux only)
   */
  public abstract void makeExecutable (Path path)
    throws IOException;

  /**
   * Returns the short identifier for this platform as used in CLI arguments.
   *
   * @return platform code, e.g. {@code linux} or {@code windows}
   */
  public String getCode () {

    return code;
  }

  /**
   * Returns the filename extension used by service control scripts on this platform.
   *
   * @return script extension, e.g. {@code .sh} on Linux or {@code .bat} on Windows
   */
  public String getBatchExtension () {

    return batchExtension;
  }
}
