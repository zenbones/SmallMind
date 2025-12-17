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

import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Record;

/**
 * Adapts a Log4j2 {@link org.apache.logging.log4j.core.Filter} to the scribe {@link Filter} interface.
 */
public class Log4JFilterAdapter implements Filter {

  private final org.apache.logging.log4j.core.Filter filter;

  /**
   * Creates an adapter around the provided Log4j2 filter.
   *
   * @param filter the native filter
   */
  public Log4JFilterAdapter (org.apache.logging.log4j.core.Filter filter) {

    this.filter = filter;
  }

  /**
   * Returns the wrapped Log4j2 filter.
   *
   * @return the native filter
   */
  protected org.apache.logging.log4j.core.Filter getNativeFilter () {

    return filter;
  }

  /**
   * Determines whether the supplied record should be logged by delegating to the Log4j2 filter.
   *
   * @param record candidate record
   * @return {@code true} if logging should proceed
   */
  public boolean willLog (Record<?> record) {

    return filter.filter((LogEvent)record.getNativeLogEntry()) != org.apache.logging.log4j.core.Filter.Result.DENY;
  }

  /**
   * Computes the hash code based on the wrapped filter.
   *
   * @return the filter hash code
   */
  public int hashCode () {

    return filter.hashCode();
  }

  /**
   * Compares this adapter to another object based on the underlying filter.
   *
   * @param obj object to compare against
   * @return {@code true} if the wrapped filters are equal
   */
  public boolean equals (Object obj) {

    if (obj instanceof Log4JFilterAdapter) {
      return filter.equals(((Log4JFilterAdapter)obj).getNativeFilter());
    }

    return filter.equals(obj);
  }
}
