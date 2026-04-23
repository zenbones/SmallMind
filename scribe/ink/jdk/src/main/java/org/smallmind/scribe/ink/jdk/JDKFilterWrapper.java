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
 * JUL {@link java.util.logging.Filter} that wraps a scribe {@link Filter}, enabling scribe filters
 * to be installed on a JUL {@link java.util.logging.Logger} via {@link java.util.logging.Logger#setFilter}.
 */
public class JDKFilterWrapper implements java.util.logging.Filter {

  private final Filter filter;

  /**
   * Builds a JUL filter that delegates all decisions to the given scribe filter.
   *
   * @param filter the scribe filter to wrap
   */
  public JDKFilterWrapper (Filter filter) {

    this.filter = filter;
  }

  /**
   * Returns the scribe filter that this wrapper delegates to.
   *
   * @return the wrapped scribe filter
   */
  public Filter getInnerFilter () {

    return filter;
  }

  /**
   * Extracts the scribe {@link org.smallmind.scribe.pen.Record} from the JUL record via
   * {@link RecordWrapper} and delegates the logging decision to the wrapped scribe filter.
   *
   * @param record the JUL record to evaluate; must implement {@link RecordWrapper}
   * @return {@code true} if the scribe filter permits the record to be logged
   */
  public boolean isLoggable (LogRecord record) {

    return filter.willLog(((RecordWrapper)record).getRecord());
  }

  /**
   * Returns the hash code of the wrapped scribe filter.
   *
   * @return hash code delegated to the wrapped filter
   */
  public int hashCode () {

    return filter.hashCode();
  }

  /**
   * Compares this wrapper for equality by comparing the underlying scribe filter; unwraps the other
   * object if it is also a {@link JDKFilterWrapper}.
   *
   * @param obj the object to compare against
   * @return {@code true} if both wrappers delegate to the same scribe filter, or the filter equals {@code obj}
   */
  public boolean equals (Object obj) {

    if (obj instanceof JDKFilterWrapper) {
      return filter.equals(((JDKFilterWrapper)obj).getInnerFilter());
    }

    return filter.equals(obj);
  }
}
