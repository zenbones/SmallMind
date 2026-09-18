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
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;

/**
 * Strategy interface responsible for bidirectional translation between jailed paths and
 * paths on a backing native file system.
 *
 * <p>Implementations determine how the jail boundary is established (for example, a fixed
 * root directory or a root derived from a thread-bound context) and are the single point at
 * which confinement is enforced: every path that crosses between the jail and the native file
 * system passes through {@link #wrapPath(JailedFileSystem, Path)} or
 * {@link #unwrapPath(Path, LinkOption...)}, and any path that would escape the jail must be
 * rejected with a {@link SecurityException}.
 *
 * <p>Jail space is a closed, {@code '/'}-separated path space whose name elements are opaque
 * strings. An implementation must never allow the native file system to re-parse the text of a
 * jailed name element, since a string that is a single name in jail space may be an entire
 * path - or an absolute one - on the native file system.
 *
 * @see AbstractJailedPathTranslator
 * @see RootedPathTranslator
 * @see ContextSensitiveRootedPathTranslator
 * @see JailPolicy
 */
public interface JailedPathTranslator {

  /**
   * Returns the native file system that backs the jail.
   *
   * <p>All file-system operations delegated by {@link JailedFileSystemProvider} ultimately
   * operate on paths within this file system.
   *
   * @return the native {@link FileSystem} being constrained by the jail
   */
  FileSystem getNativeFileSystem ();

  /**
   * Returns the policy that governs how strictly this translator enforces the jail boundary.
   *
   * @return the {@link JailPolicy} in effect; never {@code null}
   */
  JailPolicy getJailPolicy ();

  /**
   * Converts a native path from the backing file system into the jailed path representation
   * visible to clients of the jail.
   *
   * <p>An absolute native path must lie at or beneath the jail root, and is returned as an
   * absolute jailed path relative to that root; the jail root itself wraps to {@code "/"}. A
   * relative native path is interpreted as being relative to the jail root and is returned as
   * a relative jailed path with the same name elements.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} for which the jailed path will be created
   * @param nativePath       the native path to translate into the jail
   * @return the corresponding jailed {@link Path}
   * @throws IOException               if an I/O error occurs during translation
   * @throws SecurityException         if the native path lies outside the jail boundary
   * @throws ProviderMismatchException if {@code nativePath} does not belong to the native file
   *                                   system returned by {@link #getNativeFileSystem()}
   */
  Path wrapPath (JailedFileSystem jailedFileSystem, Path nativePath)
    throws IOException;

  /**
   * Converts a jailed path back into the native path representation on the backing file system,
   * following symbolic links on the final name element.
   *
   * <p>Equivalent to calling {@link #unwrapPath(Path, LinkOption...)} with no options.
   *
   * @param jailedPath the jailed {@link Path} to translate into a native path
   * @return the corresponding native {@link Path} on the backing file system
   * @throws IOException               if an I/O error occurs during translation
   * @throws SecurityException         if the jailed path cannot be confined to the jail
   * @throws ProviderMismatchException if {@code jailedPath} is not a jailed path
   */
  default Path unwrapPath (Path jailedPath)
    throws IOException {

    return unwrapPath(jailedPath, new LinkOption[0]);
  }

  /**
   * Converts a jailed path back into the native path representation on the backing file system.
   *
   * <p>The returned path is always absolute and reflects the full location on the underlying
   * storage. A relative jailed path is resolved against the jail root, which acts as the working
   * directory of the jail, and {@code ..} elements are clamped at the root so that they can
   * never traverse above it.
   *
   * <p>When {@link JailPolicy#STRICT} is in effect the real location of the native path is
   * verified as well. Passing {@link LinkOption#NOFOLLOW_LINKS} excludes the final name element
   * from that verification, which allows an operation that does not follow the final link (such
   * as deleting it) to proceed even when the link points out of the jail.
   *
   * @param jailedPath the jailed {@link Path} to translate into a native path
   * @param options    options indicating how symbolic links are handled by the operation that
   *                   the translated path will be used for
   * @return the corresponding native {@link Path} on the backing file system
   * @throws IOException               if an I/O error occurs during translation
   * @throws SecurityException         if the jailed path cannot be confined to the jail
   * @throws ProviderMismatchException if {@code jailedPath} is not a jailed path
   */
  Path unwrapPath (Path jailedPath, LinkOption... options)
    throws IOException;
}
