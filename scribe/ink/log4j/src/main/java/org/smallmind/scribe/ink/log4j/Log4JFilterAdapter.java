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
 * Scribe {@link Filter} that wraps a Log4j2 {@link org.apache.logging.log4j.core.Filter}, treating
 * any result other than {@link org.apache.logging.log4j.core.Filter.Result#DENY} as a pass.
 */
public class Log4JFilterAdapter implements Filter {

  private final org.apache.logging.log4j.core.Filter filter;

  /**
   * Builds an adapter that delegates filtering decisions to the given native Log4j2 filter.
   *
   * @param filter the Log4j2 filter to wrap
   */
  public Log4JFilterAdapter (org.apache.logging.log4j.core.Filter filter) {

    this.filter = filter;
  }

  /**
   * Returns the native Log4j2 filter that this adapter wraps.
   *
   * @return the wrapped Log4j2 filter
   */
  protected org.apache.logging.log4j.core.Filter getNativeFilter () {

    return filter;
  }

  /**
   * Returns {@code true} if the Log4j2 filter does not return {@link org.apache.logging.log4j.core.Filter.Result#DENY}
   * for the event obtained by casting the record's native log entry to a {@link LogEvent}.
   *
   * @param record the candidate scribe record to evaluate
   * @return {@code true} if the Log4j2 filter permits the record to be logged
   */
  public boolean willLog (Record<?> record) {

    return filter.filter((LogEvent)record.getNativeLogEntry()) != org.apache.logging.log4j.core.Filter.Result.DENY;
  }

  /**
   * Returns the hash code of the wrapped native Log4j2 filter.
   *
   * @return hash code delegated to the native filter
   */
  public int hashCode () {

    return filter.hashCode();
  }

  /**
   * Compares this adapter for equality by comparing the underlying native filter; unwraps the other
   * object if it is also a {@link Log4JFilterAdapter}.
   *
   * @param obj the object to compare against
   * @return {@code true} if both adapters wrap the same native filter, or the native filter equals {@code obj}
   */
  public boolean equals (Object obj) {

    if (obj instanceof Log4JFilterAdapter) {
      return filter.equals(((Log4JFilterAdapter)obj).getNativeFilter());
    }

    return filter.equals(obj);
  }
}
