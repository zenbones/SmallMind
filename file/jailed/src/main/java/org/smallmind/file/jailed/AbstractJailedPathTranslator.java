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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for {@link JailedPathTranslator} implementations that constrain access to
 * a subtree of the native file system defined by a root path.
 *
 * <p>This class provides the reusable {@link #wrapPath(Path, JailedFileSystem, Path)} and
 * {@link #unwrapPath(Path, Path, LinkOption...)} helpers that perform the translation between
 * the native path space and the jailed path space. Concrete subclasses supply the specific root
 * path, either statically or derived at call time from a context, and that root path must always
 * be absolute and normalized.
 *
 * <h2>How confinement is enforced</h2>
 *
 * <p>Jail space is a closed, slash-separated path space whose name elements are opaque strings,
 * and the native path is assembled from those elements rather than parsed from text:
 *
 * <ol>
 *   <li>The jailed path is made absolute against the jail root and normalized, which clamps
 *       {@code ..} at the root and so removes any possibility of traversal above it.</li>
 *   <li>Each remaining name element is validated as a single, legal name on the native file
 *       system by {@link #asNativeName(String)} and attached with {@link Path#resolve(Path)}.
 *       A name element is never handed to the native file system as text, because a string that
 *       is one name in jail space may be an entire path, or an absolute one, natively - on
 *       Windows {@code a\b}, {@code C:}, {@code C:foo} and {@code \\host\share} are all single
 *       jailed names but none of them is a single native name.</li>
 *   <li>The assembled native path is verified to start with the jail root.</li>
 *   <li>Under {@link JailPolicy#STRICT} the real location of the native path is verified to
 *       start with the real location of the jail root, which refuses a symbolic link, junction
 *       or other reparse point that points out of the jail.</li>
 * </ol>
 *
 * @see RootedPathTranslator
 * @see ContextSensitiveRootedPathTranslator
 * @see JailPolicy
 */
public abstract class AbstractJailedPathTranslator implements JailedPathTranslator {

  /**
   * The number of cached real root paths beyond which the cache is discarded, which bounds the
   * memory held by translators whose root varies from call to call.
   */
  private static final int MAXIMUM_CACHED_ROOT_PATHS = 256;

  /**
   * Device names that the Windows kernel resolves to a character device in <em>any</em>
   * directory, with or without a file extension, so that a jailed name element of {@code NUL}
   * would address the null device rather than a file inside the jail.
   */
  private static final Set<String> RESERVED_DEVICE_NAMES = reservedDeviceNames();

  /**
   * Memoized {@link Path#toRealPath(LinkOption...)} results for jail roots, keyed by the root
   * path as supplied by the subclass.
   */
  private final ConcurrentHashMap<Path, Path> realRootPathMap = new ConcurrentHashMap<>();

  /**
   * The policy governing how strictly the jail boundary is enforced.
   */
  private final JailPolicy jailPolicy;

  /**
   * Constructs a translator that enforces the jail boundary according to the given policy.
   *
   * @param jailPolicy the {@link JailPolicy} to enforce, or {@code null} to enforce
   *                   {@link JailPolicy#STRICT}
   */
  public AbstractJailedPathTranslator (JailPolicy jailPolicy) {

    this.jailPolicy = (jailPolicy == null) ? JailPolicy.STRICT : jailPolicy;
  }

  /**
   * Assembles the set of Windows reserved device names.
   *
   * @return the reserved device names, in upper case and without extensions
   */
  private static Set<String> reservedDeviceNames () {

    HashSet<String> deviceNameSet = new HashSet<>(Set.of("CON", "PRN", "AUX", "NUL", "CONIN$", "CONOUT$"));

    for (int ordinal = 0; ordinal <= 9; ordinal++) {
      deviceNameSet.add("COM" + ordinal);
      deviceNameSet.add("LPT" + ordinal);
    }

    return Set.copyOf(deviceNameSet);
  }

  /**
   * Returns the policy that governs how strictly this translator enforces the jail boundary.
   *
   * @return the {@link JailPolicy} in effect; never {@code null}
   */
  @Override
  public JailPolicy getJailPolicy () {

    return jailPolicy;
  }

  /**
   * Normalizes a candidate jail root into the absolute, normalized form required by the
   * translation helpers.
   *
   * @param rootPath the native root path to normalize
   * @return the absolute, normalized equivalent of {@code rootPath}
   * @throws IllegalArgumentException if {@code rootPath} is {@code null}, does not belong to the
   *                                  native file system, or can not be made absolute
   */
  protected Path normalizeRootPath (Path rootPath) {

    if (rootPath == null) {
      throw new IllegalArgumentException("The root path must not be null");
    } else if (!rootPath.getFileSystem().equals(getNativeFileSystem())) {
      throw new IllegalArgumentException("The root path must belong to the native file system");
    } else {

      Path normalizedRootPath = rootPath.toAbsolutePath().normalize();

      if (!normalizedRootPath.isAbsolute()) {
        throw new IllegalArgumentException("The root path must be absolute");
      }

      return normalizedRootPath;
    }
  }

  /**
   * Wraps a native path in a {@link JailedPath}.
   *
   * <p>An absolute native path is normalized and must lie at or beneath {@code rootPath}; the
   * root itself wraps to the jail root. Under {@link JailPolicy#STRICT} a native path that is
   * not textually contained is given a second chance through its real path, so that a path
   * arriving by way of a resolved root (a linked {@code /tmp}, or a Windows short name alias)
   * still wraps. A relative native path is taken to be relative to the jail root and wraps to a
   * relative jailed path with the same name elements.
   *
   * @param rootPath         the native root path that defines the jail boundary; must be
   *                         absolute and normalized
   * @param jailedFileSystem the {@link JailedFileSystem} for which the resulting path is created
   * @param nativePath       the native path to translate into the jailed path space
   * @return a {@link JailedPath} representing the same location relative to the jail root
   * @throws IOException               if an I/O error occurs while resolving real paths
   * @throws SecurityException         if {@code nativePath} is absolute but does not lie within
   *                                   the jail, indicating an attempted escape
   * @throws ProviderMismatchException if {@code nativePath} does not belong to the native file
   *                                   system
   */
  protected Path wrapPath (Path rootPath, JailedFileSystem jailedFileSystem, Path nativePath)
    throws IOException {

    if (nativePath == null) {
      throw new IllegalArgumentException("The native path must not be null");
    } else if (!nativePath.getFileSystem().equals(getNativeFileSystem())) {
      throw new ProviderMismatchException();
    } else if (!nativePath.isAbsolute()) {

      return constructJailedPath(jailedFileSystem, nativePath, false);
    } else {

      Path normalizedNativePath = nativePath.normalize();

      if (normalizedNativePath.startsWith(rootPath)) {

        return constructJailedPath(jailedFileSystem, rootPath.relativize(normalizedNativePath), true);
      } else {
        if (JailPolicy.STRICT.equals(jailPolicy)) {
          try {

            Path realRootPath = getRealRootPath(rootPath);
            Path realNativePath = normalizedNativePath.toRealPath();

            if (realNativePath.startsWith(realRootPath)) {

              return constructJailedPath(jailedFileSystem, realRootPath.relativize(realNativePath), true);
            }
          } catch (IOException ioException) {
            // the path can not be shown to lie inside the jail, so it is refused below
          }
        }

        throw new SecurityException("No authorization for path");
      }
    }
  }

  /**
   * Resolves a jailed path to its absolute native path, enforcing the jail boundary.
   *
   * <p>The jailed path is made absolute against {@code rootPath} and normalized, so that a
   * relative jailed path is taken to be relative to the jail root and {@code ..} can never
   * traverse above it. Each resulting name element is validated by {@link #asNativeName(String)}
   * and attached to the root with {@link Path#resolve(Path)}.
   *
   * @param rootPath   the native root path that defines the jail boundary; must be absolute and
   *                   normalized
   * @param jailedPath the jailed path to translate back to the native file system
   * @param options    options indicating how symbolic links are handled by the operation that
   *                   the translated path will be used for
   * @return the absolute native {@link Path} that corresponds to {@code jailedPath}
   * @throws IOException               if an I/O error occurs while resolving real paths
   * @throws SecurityException         if {@code jailedPath} contains a name element that can not
   *                                   be confined on the native file system, or if its real
   *                                   location lies outside the jail
   * @throws ProviderMismatchException if {@code jailedPath} is not a {@link JailedPath}
   */
  protected Path unwrapPath (Path rootPath, Path jailedPath, LinkOption... options)
    throws IOException {

    if (!(jailedPath instanceof JailedPath)) {
      throw new ProviderMismatchException();
    } else {

      Path confinedPath = jailedPath.toAbsolutePath().normalize();
      Path nativePath = rootPath;

      for (int index = 0; index < confinedPath.getNameCount(); index++) {
        nativePath = nativePath.resolve(asNativeName(confinedPath.getName(index).toString()));
      }

      if (!nativePath.startsWith(rootPath)) {
        throw new SecurityException("No authorization for path(=" + jailedPath + ")");
      }

      if (JailPolicy.STRICT.equals(jailPolicy)) {
        confineRealPath(rootPath, jailedPath, nativePath, isNoFollowLinks(options));
      }

      return nativePath;
    }
  }

  /**
   * Validates a single jailed name element and converts it into a single-name native path.
   *
   * <p>The element must parse on the native file system as exactly one relative name whose text
   * is unchanged by that parse, which is what rejects native path syntax smuggled through a
   * jailed name element. When the native file system separates names with a backslash, two
   * further Windows canonicalizations are refused as well: a trailing period or space, which
   * Win32 strips - making a name element of {@code ".. "} equivalent to {@code ".."} - and the
   * reserved device names, which resolve to a device in any directory.
   *
   * @param name the text of the jailed name element
   * @return a relative native {@link Path} consisting of exactly that one name
   * @throws SecurityException if the element is empty, is {@code "."} or {@code ".."}, contains a
   *                           NUL character, or does not denote exactly one legal native name
   */
  protected Path asNativeName (String name) {

    if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
      throw new SecurityException("Illegal path element(=" + name + ")");
    } else if (name.indexOf('\u0000') >= 0) {
      throw new SecurityException("Illegal character in path element");
    } else {

      Path nativeName;

      if (isBackslashSeparated()) {

        char lastChar = name.charAt(name.length() - 1);

        if ((lastChar == '.') || (lastChar == ' ')) {
          throw new SecurityException("Illegal path element(=" + name + ")");
        } else if (isReservedDeviceName(name)) {
          throw new SecurityException("Reserved path element(=" + name + ")");
        }
      }

      try {
        nativeName = getNativeFileSystem().getPath(name);
      } catch (InvalidPathException invalidPathException) {
        throw new SecurityException("Illegal path element(=" + name + ")");
      }

      if (nativeName.isAbsolute() || (nativeName.getRoot() != null) || (nativeName.getNameCount() != 1) || (!nativeName.toString().equals(name))) {
        throw new SecurityException("Illegal path element(=" + name + ")");
      }

      return nativeName;
    }
  }

  /**
   * Verifies that the real location of a translated native path remains inside the jail.
   *
   * <p>Because the path need not exist yet, the deepest existing ancestor is resolved instead,
   * walking up no further than the jail root. A name element that does not exist can not itself
   * be a link, so resolving the deepest existing ancestor is sufficient.
   *
   * @param rootPath      the native root path that defines the jail boundary
   * @param jailedPath    the jailed path being translated, used for diagnostics
   * @param nativePath    the assembled native path to verify
   * @param noFollowLinks {@code true} to exclude the final name element from verification,
   *                      because the operation using the path will not follow it
   * @throws IOException       if the jail root or an existing ancestor can not be resolved
   * @throws SecurityException if the real location lies outside the jail
   */
  private void confineRealPath (Path rootPath, Path jailedPath, Path nativePath, boolean noFollowLinks)
    throws IOException {

    Path realRootPath = getRealRootPath(rootPath);
    Path probePath = (noFollowLinks && (nativePath.getNameCount() > rootPath.getNameCount())) ? nativePath.getParent() : nativePath;

    while (true) {
      try {
        if (!probePath.toRealPath().startsWith(realRootPath)) {
          throw new SecurityException("No authorization for path(=" + jailedPath + ")");
        }

        return;
      } catch (IOException ioException) {
        if ((probePath.getNameCount() <= rootPath.getNameCount()) || ((probePath = probePath.getParent()) == null)) {
          throw ioException;
        }
      }
    }
  }

  /**
   * Returns the real location of the jail root, memoized because it is consulted on every
   * strictly enforced translation.
   *
   * @param rootPath the native root path that defines the jail boundary
   * @return the {@link Path#toRealPath(LinkOption...)} of {@code rootPath}
   * @throws IOException if the jail root does not exist or can not be resolved
   */
  private Path getRealRootPath (Path rootPath)
    throws IOException {

    Path realRootPath;

    if ((realRootPath = realRootPathMap.get(rootPath)) == null) {
      if (realRootPathMap.size() > MAXIMUM_CACHED_ROOT_PATHS) {
        realRootPathMap.clear();
      }

      realRootPathMap.put(rootPath, realRootPath = rootPath.toRealPath());
    }

    return realRootPath;
  }

  /**
   * Builds a {@link JailedPath} from the name elements of a native path.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} for which the path is created
   * @param namePath         the native path whose name elements become the jailed path's name
   *                         elements
   * @param absolute         {@code true} if the resulting jailed path should be absolute
   * @return the assembled {@link JailedPath}
   * @throws SecurityException if a native name element contains the jailed separator, which
   *                           would silently introduce name elements into jail space
   */
  private Path constructJailedPath (JailedFileSystem jailedFileSystem, Path namePath, boolean absolute) {

    StringBuilder pathBuilder = new StringBuilder();

    for (int index = 0; index < namePath.getNameCount(); index++) {

      String name = namePath.getName(index).toString();

      if (name.indexOf(JailedPath.SEPARATOR) >= 0) {
        throw new SecurityException("Illegal character in native path element");
      } else if (!name.isEmpty()) {
        if (absolute || (!pathBuilder.isEmpty())) {
          pathBuilder.append(JailedPath.SEPARATOR);
        }

        pathBuilder.append(name);
      }
    }

    if (absolute && pathBuilder.isEmpty()) {
      pathBuilder.append(JailedPath.SEPARATOR);
    }

    return new JailedPath(jailedFileSystem, pathBuilder.toString());
  }

  /**
   * Indicates whether the native file system separates name elements with a backslash, which is
   * taken as the signal that Windows canonicalization rules apply to its name elements.
   *
   * @return {@code true} if the native separator is a backslash
   */
  private boolean isBackslashSeparated () {

    return "\\".equals(getNativeFileSystem().getSeparator());
  }

  /**
   * Indicates whether a name element denotes a Windows reserved device, with or without an
   * extension.
   *
   * @param name the text of the name element
   * @return {@code true} if the element addresses a character device
   */
  private boolean isReservedDeviceName (String name) {

    int dotPos = name.indexOf('.');

    return RESERVED_DEVICE_NAMES.contains(((dotPos < 0) ? name : name.substring(0, dotPos)).toUpperCase());
  }

  /**
   * Indicates whether a set of link options asks that symbolic links not be followed.
   *
   * @param options the options to inspect
   * @return {@code true} if {@link LinkOption#NOFOLLOW_LINKS} is present
   */
  private boolean isNoFollowLinks (LinkOption... options) {

    if (options != null) {
      for (LinkOption option : options) {
        if (LinkOption.NOFOLLOW_LINKS.equals(option)) {

          return true;
        }
      }
    }

    return false;
  }
}
