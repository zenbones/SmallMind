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
 * Immutable configuration for an {@link EphemeralFileSystem}.
 *
 * <p>A configuration declares the total {@linkplain #getCapacity() capacity} of the backing heap
 * store, the {@linkplain #getBlockSize() block size} used to segment file content, the set of
 * absolute {@linkplain #getRoots() roots} the ephemeral file system claims, and the
 * {@linkplain #getWorkingDirectory() working directory} against which relative paths are resolved.
 *
 * <p>The roots are what make this file system an <em>overlay</em>. A path that falls beneath one of
 * the roots is handled in memory; any other absolute path is delegated to the native file system
 * (see {@link NativePath}). Matching is performed on whole path segments, so a root of
 * {@code /overlay} claims {@code /overlay/a} but not {@code /overlayed}.
 *
 * <p>Relative paths belong to whichever file system owns the working directory, which keeps a
 * relative path from silently resolving against the real file system while an ephemeral working
 * directory is in effect.
 *
 * <p>Each value may be supplied directly or derived from a system property:
 * <ul>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.capacity}</li>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.blockSize}</li>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.roots}</li>
 *   <li>{@code org.smallmind.file.ephemeral.configuration.workingDirectory}</li>
 * </ul>
 */
public class EphemeralFileSystemConfiguration {

  private static final String CAPACITY_PROPERTY = "org.smallmind.file.ephemeral.configuration.capacity";
  private static final String BLOCK_SIZE_PROPERTY = "org.smallmind.file.ephemeral.configuration.blockSize";
  private static final String ROOTS_PROPERTY = "org.smallmind.file.ephemeral.configuration.roots";
  private static final String WORKING_DIRECTORY_PROPERTY = "org.smallmind.file.ephemeral.configuration.workingDirectory";

  private final String[] roots;
  private final String workingDirectory;
  private final boolean workingDirectoryIsOurs;
  private final long capacity;
  private final int blockSize;

  /**
   * Creates a configuration entirely from system properties, falling back to
   * {@link Long#MAX_VALUE} capacity, a 1024 byte block size, a single root of {@code "/"}, and a
   * working directory equal to the first root.
   */
  public EphemeralFileSystemConfiguration () {

    this(deriveCapacity(), deriveBlockSize(), deriveRoots(), deriveWorkingDirectory());
  }

  /**
   * Creates a configuration whose working directory is the first of the supplied roots. Use
   * {@link #withWorkingDirectory(String)} to specify a different one.
   *
   * @param capacity  the total number of bytes the heap store may hold; must be &gt; 0
   * @param blockSize the segment size used to allocate file content; must be &gt; 0
   * @param roots     at least one absolute root claimed by this file system
   * @throws IllegalArgumentException if capacity or block size is not positive, if no roots are
   *                                  supplied, or if any root is not absolute
   */
  public EphemeralFileSystemConfiguration (long capacity, int blockSize, String... roots) {

    this(capacity, blockSize, roots, ((roots == null) || (roots.length == 0)) ? null : roots[0]);
  }

  /**
   * Creates a fully specified configuration. The parameter order places the roots before the
   * working directory so that this constructor can never compete with the varargs constructor
   * during overload resolution.
   *
   * @param capacity         the total number of bytes the heap store may hold; must be &gt; 0
   * @param blockSize        the segment size used to allocate file content; must be &gt; 0
   * @param roots            at least one absolute root claimed by this file system
   * @param workingDirectory the absolute directory against which relative paths are resolved
   * @throws IllegalArgumentException if capacity or block size is not positive, if no roots are
   *                                  supplied, or if any root or the working directory is not
   *                                  absolute
   */
  private EphemeralFileSystemConfiguration (long capacity, int blockSize, String[] roots, String workingDirectory) {

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
      if ((workingDirectory == null) || (!workingDirectory.startsWith(EphemeralPath.getSeparator()))) {
        throw new IllegalArgumentException("The working directory must start with " + EphemeralPath.getSeparator());
      }

      this.capacity = capacity;
      this.blockSize = blockSize;
      this.roots = roots;
      this.workingDirectory = collapse(workingDirectory);

      workingDirectoryIsOurs = isOursAbsolute(this.workingDirectory);
    }
  }

  private static long deriveCapacity () {

    String capacityProperty;

    if ((capacityProperty = System.getProperty(CAPACITY_PROPERTY)) != null) {

      return Long.parseLong(capacityProperty);
    } else {

      return Long.MAX_VALUE;
    }
  }

  private static int deriveBlockSize () {

    String blockSizeProperty;

    if ((blockSizeProperty = System.getProperty(BLOCK_SIZE_PROPERTY)) != null) {

      return Integer.parseInt(blockSizeProperty);
    } else {

      return 1024;
    }
  }

  private static String[] deriveRoots () {

    String rootsProperty;

    if ((rootsProperty = System.getProperty(ROOTS_PROPERTY)) != null) {

      String[] rawNames;
      String[] translatedNames;
      String trimmedRootsProperty = rootsProperty.strip();

      if (trimmedRootsProperty.startsWith("[") && trimmedRootsProperty.endsWith("]")) {
        rawNames = trimmedRootsProperty.substring(1, trimmedRootsProperty.length() - 1).split(",");
      } else {
        rawNames = trimmedRootsProperty.split(",");
      }

      translatedNames = new String[rawNames.length];
      for (int index = 0; index < rawNames.length; index++) {

        String trimmedName = rawNames[index].strip();

        translatedNames[index] = trimmedName.startsWith(EphemeralPath.getSeparator()) ? trimmedName : (EphemeralPath.getSeparator() + trimmedName);
      }

      return translatedNames;
    } else {

      return new String[] {EphemeralPath.getSeparator()};
    }
  }

  private static String deriveWorkingDirectory () {

    String workingDirectoryProperty;

    if ((workingDirectoryProperty = System.getProperty(WORKING_DIRECTORY_PROPERTY)) != null) {

      String trimmedWorkingDirectory = workingDirectoryProperty.strip();

      return trimmedWorkingDirectory.startsWith(EphemeralPath.getSeparator()) ? trimmedWorkingDirectory : (EphemeralPath.getSeparator() + trimmedWorkingDirectory);
    } else {

      return deriveRoots()[0];
    }
  }

  /**
   * Collapses redundant and trailing separators so that two spellings of the same absolute
   * directory compare equal.
   *
   * @param path an absolute path
   * @return the canonical spelling of {@code path}, never with a trailing separator
   */
  private static String collapse (String path) {

    StringBuilder collapsedBuilder = new StringBuilder();

    for (String segment : path.split(EphemeralPath.getSeparator())) {
      if (!segment.isEmpty()) {
        collapsedBuilder.append(EphemeralPath.getSeparator()).append(segment);
      }
    }

    return collapsedBuilder.isEmpty() ? EphemeralPath.getSeparator() : collapsedBuilder.toString();
  }

  /**
   * Joins a first path component with any additional components, inserting separators as needed
   * and ignoring empty components.
   *
   * @param first the first component; must not be {@code null}
   * @param more  additional components, which may be {@code null}
   * @return the joined path
   */
  private static String join (String first, String... more) {

    StringBuilder joinedBuilder = new StringBuilder(first);

    if (more != null) {
      for (String another : more) {
        if (!another.isEmpty()) {
          if ((!joinedBuilder.isEmpty()) && (joinedBuilder.charAt(joinedBuilder.length() - 1) != EphemeralPath.getSeparatorChar())) {
            joinedBuilder.append(EphemeralPath.getSeparator());
          }
          joinedBuilder.append(another);
        }
      }
    }

    return joinedBuilder.toString();
  }

  /**
   * Tests whether a path is absolute in the <em>native</em> namespace while not being rooted at
   * this file system's separator.
   *
   * <p>This matters only on a platform whose absolute paths do not begin with a separator. A
   * Windows path such as {@code C:\Program Files\...} or a UNC share such as
   * {@code \\host\share} would otherwise fall through to the relative case and be claimed
   * whenever the working directory is claimed — which would hand the entire real file system to the
   * heap, and leave the platform unable to read even its own configuration.
   *
   * <p>The test is syntactic and performs no I/O, so it is safe to call while
   * {@link java.nio.file.FileSystems} is still resolving its default provider.
   *
   * @param path the joined path to examine
   * @return {@code true} if the path is absolute in the native namespace
   */
  private static boolean isNativeAbsolute (String path) {

    return path.startsWith("\\\\") || ((path.length() >= 2) && (path.charAt(1) == ':') && Character.isLetter(path.charAt(0)));
  }

  /**
   * Returns a copy of this configuration that resolves relative paths against a different
   * directory.
   *
   * @param workingDirectory the absolute directory against which relative paths are resolved
   * @return a new configuration; this instance is left unchanged
   * @throws IllegalArgumentException if the working directory is not absolute
   */
  public EphemeralFileSystemConfiguration withWorkingDirectory (String workingDirectory) {

    return new EphemeralFileSystemConfiguration(capacity, blockSize, roots, workingDirectory);
  }

  /**
   * Returns the absolute roots claimed by this file system.
   *
   * @return the configured roots; never empty
   */
  public String[] getRoots () {

    return roots;
  }

  /**
   * Returns the absolute directory against which relative paths are resolved.
   *
   * @return the canonical working directory; never {@code null}
   */
  public String getWorkingDirectory () {

    return workingDirectory;
  }

  /**
   * Returns the total number of bytes the heap store may hold.
   *
   * @return the capacity in bytes; always &gt; 0
   */
  public long getCapacity () {

    return capacity;
  }

  /**
   * Returns the segment size used to allocate file content.
   *
   * @return the block size in bytes; always &gt; 0
   */
  public int getBlockSize () {

    return blockSize;
  }

  /**
   * Tests whether the path formed by joining the supplied components belongs to this file system.
   *
   * <p>An absolute path is ours when it equals one of the configured roots or lies beneath one on a
   * segment boundary. A relative path is ours when the working directory is ours.
   *
   * @param first the first path component; must not be {@code null}
   * @param more  additional components to be joined with the separator
   * @return {@code true} if the path should be handled in memory rather than delegated to the
   * native file system
   */
  public boolean isOurs (String first, String... more) {

    String joined = join(first, more);

    if (joined.startsWith(EphemeralPath.getSeparator())) {

      return isOursAbsolute(joined);
    } else if (isNativeAbsolute(joined)) {
      // an absolute native path is not a relative ephemeral one, so the working directory has no
      // bearing on it
      return false;
    } else {

      return workingDirectoryIsOurs;
    }
  }

  /**
   * Verifies that this configuration is fit to serve as the JVM default file system, and explains
   * the problem if it is not.
   *
   * <p>A configuration that claims every path cannot be the default, on any platform: while the JVM
   * is starting it reads its own configuration — {@code java.security} above all — through
   * {@link java.nio.file.Files}, and an empty heap has nothing to give it. The resulting failure is
   * an {@code InternalError: Error loading java.security file} raised from deep inside the security
   * provider machinery, which says nothing about the real cause. Refusing the configuration up
   * front, with the same rule on every platform, is far kinder than letting that happen.
   *
   * <p>Installing as the default therefore calls for an explicit overlay root, such as
   * {@code -Dorg.smallmind.file.ephemeral.configuration.roots=/overlay}.
   *
   * @throws IllegalArgumentException if this configuration cannot serve as the JVM default
   */
  public void checkFitToBeDefaultFileSystem () {

    for (String root : roots) {
      if (EphemeralPath.getSeparator().equals(collapse(root))) {
        throw new IllegalArgumentException("A root of " + EphemeralPath.getSeparator() + " claims every path, so it cannot serve as the default file system, which must leave the platform able to read its own configuration. Set " + ROOTS_PROPERTY + " to an overlay root, such as " + EphemeralPath.getSeparator() + "overlay.");
      }
    }

    String javaHome;

    if (((javaHome = System.getProperty("java.home")) != null) && isOurs(javaHome)) {
      throw new IllegalArgumentException("The configured roots claim java.home(" + javaHome + "), so the platform would be unable to read its own configuration. Set " + ROOTS_PROPERTY + " to a root that excludes it.");
    }
  }

  /**
   * Tests whether an absolute path lies at or beneath one of the configured roots.
   *
   * @param path an absolute path
   * @return {@code true} if the path is claimed by one of the roots
   */
  private boolean isOursAbsolute (String path) {

    String candidate = collapse(path);

    for (String root : roots) {

      String collapsedRoot = collapse(root);

      if (EphemeralPath.getSeparator().equals(collapsedRoot) || candidate.equals(collapsedRoot) || candidate.startsWith(collapsedRoot + EphemeralPath.getSeparator())) {

        return true;
      }
    }

    return false;
  }
}
