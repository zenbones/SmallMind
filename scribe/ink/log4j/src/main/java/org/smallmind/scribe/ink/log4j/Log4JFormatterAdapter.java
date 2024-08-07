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
package org.smallmind.scribe.ink.log4j;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.Formatter;
import org.smallmind.scribe.pen.Record;

public class Log4JFormatterAdapter implements Formatter {

  private final Layout<LogEvent> layout;

  public Log4JFormatterAdapter (Layout<LogEvent> layout) {

    this.layout = layout;
  }

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
