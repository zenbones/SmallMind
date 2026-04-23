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
package org.smallmind.file.ephemeral;

/**
 * Immutable configuration for an {@link EphemeralFileSystem}. Holds the logical storage
 * capacity, the allocation block size, and the set of path prefixes ("roots") that will be
 * served from the ephemeral heap rather than delegated to the native file system.
 *
 * <p>The no-argument constructor reads values from the following system properties:
 * <ul>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.capacity} – defaults to
 *       {@link Long#MAX_VALUE}</li>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.blockSize} – defaults to
 *       {@code 1024}</li>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.roots} – comma-separated list,
 *       optionally bracketed; defaults to {@code "/"}</li>
 * </ul>
 */
public class EphemeralFileSystemConfiguration {

  private static final String CAPACITY_PROPERTY = "org.smallmind.file.ephemeral.configuration.capacity";
  private static final String BLOCK_SIZ_PROPERTY = "org.smallmind.file.ephemeral.configuration.blockSize";
  private static final String ROOTS_PROPERTY = "org.smallmind.file.ephemeral.configuration.roots";
  private final String[] roots;
  private final long capacity;
  private final int blockSize;

  /**
   * Builds a configuration by reading values from system properties, falling back to sensible
   * defaults when a property is absent.
   */
  public EphemeralFileSystemConfiguration () {

    this(deriveCapacity(), deriveBlockSize(), deriveRoots());
  }

  /**
   * Builds a configuration with explicitly supplied values.
   *
   * @param capacity  the total storage capacity to report, in bytes; must be positive
   * @param blockSize the allocation unit size in bytes used when creating new file nodes;
   *                  must be positive
   * @param roots     one or more root path prefixes that will be served from the ephemeral
   *                  heap; each entry must start with {@code "/"}
   * @throws IllegalArgumentException if {@code capacity} or {@code blockSize} are not positive,
   *                                  if {@code roots} is {@code null} or empty, or if any root
   *                                  does not start with {@code "/"}
   */
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

  /**
   * Reads the capacity from the {@code org.smallmind.file.ephemeral.configuration.capacity}
   * system property, or returns {@link Long#MAX_VALUE} when the property is absent.
   *
   * @return the configured capacity, or {@link Long#MAX_VALUE} by default
   */
  private static long deriveCapacity () {

    String capacityEnvVar;

    if ((capacityEnvVar = System.getProperty(CAPACITY_PROPERTY)) != null) {

      return Long.parseLong(capacityEnvVar);
    } else {

      return Long.MAX_VALUE;
    }
  }

  /**
   * Reads the block size from the {@code org.smallmind.file.ephemeral.configuration.blockSize}
   * system property, or returns {@code 1024} when the property is absent.
   *
   * @return the configured block size, or {@code 1024} by default
   */
  private static int deriveBlockSize () {

    String blockSizeEnvVar;

    if ((blockSizeEnvVar = System.getProperty(BLOCK_SIZ_PROPERTY)) != null) {

      return Integer.parseInt(blockSizeEnvVar);
    } else {

      return 1024;
    }
  }

  /**
   * Reads the root list from the {@code org.smallmind.file.ephemeral.configuration.roots}
   * system property. The value may be a bare comma-separated list or a bracketed list
   * ({@code [a,b,c]}). Each entry is stripped of whitespace and prefixed with {@code "/"} if
   * it does not already start with one. When the property is absent a single root of
   * {@code "/"} is returned.
   *
   * @return the resolved array of root path strings; never empty
   */
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

  /**
   * Returns the configured root path prefixes.
   *
   * @return the array of root strings; never {@code null} or empty
   */
  public String[] getRoots () {

    return roots;
  }

  /**
   * Returns the configured storage capacity.
   *
   * @return capacity in bytes
   */
  public long getCapacity () {

    return capacity;
  }

  /**
   * Returns the configured allocation block size.
   *
   * @return block size in bytes
   */
  public int getBlockSize () {

    return blockSize;
  }

  /**
   * Tests whether the path assembled from the given segments belongs to one of the configured
   * roots. The segments are compared character-by-character against each root prefix; the
   * first matching root causes the method to return {@code true}.
   *
   * @param first the first segment of the path to test
   * @param more  optional additional segments of the path
   * @return {@code true} if the assembled path starts with at least one configured root
   */
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
