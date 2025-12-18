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
package org.smallmind.web.json;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

/**
 * Raised when a wildcard is encountered in a non-terminal position within a JSON path expression.
 * Wildcards are expected only at the end of a path; placing them earlier can produce querries that
 * can't be indexed and may only be resolved via table scans.
 */
public class NonTerminalWildcardException extends FormattedRuntimeException {

  /**
   * Creates the exception without a message, typically used when context supplies details elsewhere.
   */
  public NonTerminalWildcardException () {

  }

  /**
   * Creates the exception with a formatted message describing the invalid wildcard usage.
   *
   * @param message description of the error
   * @param args    optional message arguments for formatting
   */
  public NonTerminalWildcardException (String message, Object... args) {

    super(message, args);
  }
}
