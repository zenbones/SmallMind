/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.ink.log4j;

import java.util.Collection;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Formatter;
import org.smallmind.scribe.pen.Record;

public class Log4JFormatterAdapter implements Formatter {

  private Layout layout;

  public Log4JFormatterAdapter (Layout layout) {

    this.layout = layout;
  }

  public String format (Record record, Collection<Filter> filterCollection) {

    StringBuilder formatBuilder = new StringBuilder();
    String header;
    String footer;

    header = layout.getHeader();
    if (header != null) {
      formatBuilder.append(header);
    }

    formatBuilder.append(layout.format((LoggingEvent)record.getNativeLogEntry()));

    footer = layout.getFooter();
    if (footer != null) {
      formatBuilder.append(footer);
    }

    return formatBuilder.toString();
  }
}
