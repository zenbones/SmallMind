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

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

/**
 * {@link PathMatcher} implementation that tests paths against a compiled regular expression.
 * Instances are created by {@link EphemeralFileSystem#getPathMatcher(String)} for both the
 * {@code "glob"} and {@code "regex"} syntaxes.
 *
 * <p>Because an {@link EphemeralFileSystem} may hand out {@link NativePath} instances for paths
 * that fall outside its configured roots, a matcher may also carry the equivalent matcher obtained
 * from the native file system. A native path is rendered with the native separator, so testing it
 * against a pattern compiled for the {@code '/'} separator would silently fail to match; such
 * paths are delegated instead.
 */
public class RegexPathMatcher implements PathMatcher {

  /**
   * The compiled regular expression used to match ephemeral path strings.
   */
  private final Pattern pattern;

  /**
   * The equivalent matcher from the native file system, used for {@link NativePath} instances, or
   * {@code null} when no native file system is attached.
   */
  private final PathMatcher nativePathMatcher;

  /**
   * Creates a matcher backed by the given compiled regular expression, with no native delegate.
   *
   * @param pattern the pre-compiled pattern to use for matching; must not be {@code null}
   */
  public RegexPathMatcher (Pattern pattern) {

    this(pattern, null);
  }

  /**
   * Creates a matcher backed by the given compiled regular expression.
   *
   * @param pattern           the pre-compiled pattern to use for matching; must not be
   *                          {@code null}
   * @param nativePathMatcher the matcher to consult for {@link NativePath} instances, which may be
   *                          {@code null}
   */
  public RegexPathMatcher (Pattern pattern, PathMatcher nativePathMatcher) {

    this.pattern = pattern;
    this.nativePathMatcher = nativePathMatcher;
  }

  /**
   * Tests whether the string representation of {@code path} matches the regular expression, or, for
   * a {@link NativePath}, whether the native file system's equivalent matcher accepts it.
   *
   * @param path the path to test; must not be {@code null}
   * @return {@code true} if the path matches
   */
  @Override
  public boolean matches (Path path) {

    if (path instanceof NativePath) {

      return (nativePathMatcher != null) && nativePathMatcher.matches(((NativePath)path).getNativePath());
    } else {

      return pattern.matcher(path.toString()).matches();
    }
  }
}
