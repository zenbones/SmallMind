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
 * Supported operating systems and their associated behaviors for deployment artifacts.
 */
public enum OperatingSystem {

  LINUX("linux", ".sh") {
    @Override
    public void makeExecutable (Path path)
      throws IOException {

      Files.setPosixFilePermissions(path, PERMISSIONS_755);
    }
  },
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

  OperatingSystem (String code, String batchExtension) {

    this.code = code;
    this.batchExtension = batchExtension;
  }

  /**
   * Resolve an operating system from its short code.
   *
   * @param code the string representation (e.g. {@code linux} or {@code windows})
   * @return the matching operating system, or {@code null} if the code is not recognized
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
   * Ensure the supplied file is executable on the target operating system.
   *
   * @param path the file that should be executable
   * @throws IOException if permissions cannot be set
   */
  public abstract void makeExecutable (Path path)
    throws IOException;

  /**
   * @return the identifier used to select the operating system (e.g. {@code linux})
   */
  public String getCode () {

    return code;
  }

  /**
   * @return the batch/script extension associated with the operating system
   */
  public String getBatchExtension () {

    return batchExtension;
  }
}
