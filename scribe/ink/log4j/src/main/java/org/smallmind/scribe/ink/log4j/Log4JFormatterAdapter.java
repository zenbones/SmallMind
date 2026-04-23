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
package org.smallmind.scribe.ink.log4j;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.Formatter;
import org.smallmind.scribe.pen.Record;

/**
 * Scribe {@link Formatter} that delegates to a Log4j2 {@link Layout}, prepending any header bytes
 * and appending any footer bytes that the layout produces, and decoding all output as UTF-8.
 */
public class Log4JFormatterAdapter implements Formatter {

  private final Layout<LogEvent> layout;

  /**
   * Builds a scribe formatter that delegates rendering to the given native Log4j2 layout.
   *
   * @param layout the Log4j2 layout to wrap
   */
  public Log4JFormatterAdapter (Layout<LogEvent> layout) {

    this.layout = layout;
  }

  /**
   * Formats the record by converting the layout's optional header bytes, the event body bytes,
   * and the optional footer bytes to UTF-8 strings and concatenating them.
   *
   * @param record the scribe record to format; its native log entry must be a {@link LogEvent}
   * @return the concatenated UTF-8 header, body, and footer produced by the Log4j2 layout
   */
  public String format (Record<?> record) {

    StringBuilder formatBuilder = new StringBuilder();
    byte[] header;
    byte[] footer;

    header = layout.getHeader();
    if (header != null) {
      formatBuilder.append(new String(header, StandardCharsets.UTF_8));
    }

    formatBuilder.append(new String(layout.toByteArray((LogEvent)record.getNativeLogEntry()), StandardCharsets.UTF_8));

    footer = layout.getFooter();
    if (footer != null) {
      formatBuilder.append(new String(footer, StandardCharsets.UTF_8));
    }

    return formatBuilder.toString();
  }
}
