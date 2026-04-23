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
import java.util.regex.Pattern;

/**
 * A {@link PathFilter} that accepts paths whose file-name element matches a glob-style wildcard pattern
 * translated to a regular expression by {@link RegExTranslator}.
 */
public class WildcardFileNamePathFilter implements PathFilter {

  private final Pattern namePattern;

  /**
   * Constructs a filter that matches path file names against the given glob pattern.
   *
   * @param name a glob pattern using {@code *} (any characters except {@code /}) and
   *             {@code ?} (any single character except {@code /})
   */
  public WildcardFileNamePathFilter (String name) {

    namePattern = Pattern.compile(RegExTranslator.translate(name));
  }

  /**
   * Returns {@code true} if the final name element of {@code path} matches the configured pattern.
   *
   * @param path the path to test
   * @return {@code true} when the file name matches; {@code false} otherwise
   */
  @Override
  public boolean accept (Path path) {

    return namePattern.matcher(path.getFileName().toString()).matches();
  }
}
