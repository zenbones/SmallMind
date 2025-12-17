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
package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

/**
 * Wraps a scribe {@link Filter} so it can be installed as a JUL {@link java.util.logging.Filter}.
 */
public class JDKFilterWrapper implements java.util.logging.Filter {

  private final Filter filter;

  /**
   * Creates a wrapper around the given scribe filter.
   *
   * @param filter the filter to wrap
   */
  public JDKFilterWrapper (Filter filter) {

    this.filter = filter;
  }

  /**
   * Returns the wrapped scribe filter.
   *
   * @return the underlying filter
   */
  public Filter getInnerFilter () {

    return filter;
  }

  /**
   * Applies the wrapped filter to the supplied JUL record.
   *
   * @param record JUL record carrying the scribe record wrapper
   * @return {@code true} if logging should proceed
   */
  public boolean isLoggable (LogRecord record) {

    return filter.willLog(((RecordWrapper)record).getRecord());
  }

  /**
   * Delegates hash code computation to the wrapped filter.
   *
   * @return the hash code of the underlying filter
   */
  public int hashCode () {

    return filter.hashCode();
  }

  /**
   * Compares this wrapper to another object based on the wrapped filter.
   *
   * @param obj object to compare against
   * @return {@code true} if the wrapped filters are equal
   */
  public boolean equals (Object obj) {

    if (obj instanceof JDKFilterWrapper) {
      return filter.equals(((JDKFilterWrapper)obj).getInnerFilter());
    }

    return filter.equals(obj);
  }
}
