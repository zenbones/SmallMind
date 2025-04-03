/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.file.ephemeral;

public class EphemeralFileSystemConfiguration {

  private static final String CAPACITY_PROPERTY = "org.smallmind.file.ephemeral.configuration.capacity";
  private static final String BLOCK_SIZ_PROPERTY = "org.smallmind.file.ephemeral.configuration.blockSize";
  private static final String ROOTS_PROPERTY = "org.smallmind.file.ephemeral.configuration.roots";
  private final String[] roots;
  private final long capacity;
  private final int blockSize;

  public EphemeralFileSystemConfiguration () {

    this(deriveCapacity(), deriveBlockSize(), deriveRoots());
  }

  public EphemeralFileSystemConfiguration (long capacity, int blockSize, String... roots) {

    if ((capacity <= 0) || (blockSize <= 0)) {
      throw new IllegalArgumentException("Both capacity and block size must be > 0");
    } else if ((roots == null) || (roots.length == 0)) {
      throw new IllegalArgumentException("At least 1 root path must be specified");
    } else {

      for (String root : roots) {
        if (!root.startsWith(EphemeralPath.getSeparator())) {
          throw new IllegalArgumentException("All roots must start with " + EphemeralPath.getSeparator());
        }
      }

      this.capacity = capacity;
      this.blockSize = blockSize;
      this.roots = roots;
    }
  }

  private static long deriveCapacity () {

    String capacityEnvVar;

    if ((capacityEnvVar = System.getProperty(CAPACITY_PROPERTY)) != null) {

      return Long.parseLong(capacityEnvVar);
    } else {

      return Long.MAX_VALUE;
    }
  }

  private static int deriveBlockSize () {

    String blockSizeEnvVar;

    if ((blockSizeEnvVar = System.getProperty(BLOCK_SIZ_PROPERTY)) != null) {

      return Integer.parseInt(blockSizeEnvVar);
    } else {

      return 1024;
    }
  }

  private static String[] deriveRoots () {

    String rootsEnvVar;

    if ((rootsEnvVar = System.getProperty(ROOTS_PROPERTY)) != null) {

      String[] rawNames;
      String[] translatedNames;
      String trimmedRootsEnvVar = rootsEnvVar.strip();

      if (trimmedRootsEnvVar.startsWith("[") && trimmedRootsEnvVar.endsWith("]")) {
        rawNames = trimmedRootsEnvVar.substring(1, trimmedRootsEnvVar.length() - 1).split(",");
      } else {
        rawNames = trimmedRootsEnvVar.split(",");
      }

      translatedNames = new String[rawNames.length];
      for (int index = 0; index < rawNames.length; index++) {

        String trimmedName = rawNames[index].strip();

        translatedNames[index] = trimmedName.startsWith("/") ? trimmedName : "/" + trimmedName;
      }

      return translatedNames;
    } else {

      return new String[] {EphemeralPath.getSeparator()};
    }
  }

  public String[] getRoots () {

    return roots;
  }

  public long getCapacity () {

    return capacity;
  }

  public int getBlockSize () {

    return blockSize;
  }

  public boolean isOurs (String first, String... more) {

    for (String root : roots) {

      boolean matched = true;
      int position = 0;
      int match = -1;

      for (int index = 0; index < root.length(); index++) {

        String toBeMatched = (match < 0) ? first : more[match];

        while (position >= toBeMatched.length()) {
          if ((more == null) || (++match >= more.length)) {

            return false;
          } else {
            position = 0;
          }
        }

        if (root.charAt(index) != toBeMatched.charAt(position)) {
          matched = false;
          break;
        } else {
          position++;
        }
      }

      if (matched) {

        return true;
      }
    }

    return false;
  }
}
