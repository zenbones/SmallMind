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
package org.smallmind.nutsnbolts.xml;

import org.smallmind.nutsnbolts.lang.FormattedException;

/**
 * Exception thrown when the {@link org.smallmind.nutsnbolts.xml.sax.ExtensibleSAXParser} encounters configuration or parsing problems.
 * Carries formatted messages for consistent diagnostics.
 */
public class ExtensibleSAXParserException extends FormattedException {

  /**
   * Creates an exception with no message or cause.
   */
  public ExtensibleSAXParserException () {

    super();
  }

  /**
   * Creates an exception with a formatted message.
   *
   * @param message message pattern
   * @param args    pattern arguments
   */
  public ExtensibleSAXParserException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Creates an exception with both a cause and a formatted message.
   *
   * @param throwable underlying cause
   * @param message   message pattern
   * @param args      pattern arguments
   */
  public ExtensibleSAXParserException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Creates an exception that wraps an underlying cause.
   *
   * @param throwable underlying cause
   */
  public ExtensibleSAXParserException (Throwable throwable) {

    super(throwable);
  }
}
