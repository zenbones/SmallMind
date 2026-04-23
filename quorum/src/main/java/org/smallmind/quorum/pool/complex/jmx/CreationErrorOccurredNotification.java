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
package org.smallmind.quorum.pool.complex.jmx;

import java.util.concurrent.atomic.AtomicLong;
import javax.management.Notification;

/**
 * JMX notification emitted by {@link ComponentPoolMonitor} when a component pool error
 * event is received.
 * <p>
 * Each instance carries the exception that caused the pool error and is assigned a globally
 * unique, monotonically increasing sequence number. The notification type is {@link #TYPE}
 * ({@value #TYPE}), which can be used in {@link javax.management.NotificationFilter}
 * implementations to select only error notifications.
 */
public class CreationErrorOccurredNotification extends Notification {

  private static final AtomicLong SEQUNCE_NUMBER = new AtomicLong(0);

  /**
   * Notification type string identifying creation-error notifications.
   */
  public static final String TYPE = "ERROR_OCCURRED";

  private final Exception exception;

  /**
   * Creates the notification with the given source and cause.
   * <p>
   * The sequence number is auto-incremented from a class-level counter; the timestamp is
   * set to the current wall-clock time.
   *
   * @param source    the MBean object that is the source of this notification (typically
   *                  the {@link javax.management.ObjectName} of the
   *                  {@link ComponentPoolMonitor})
   * @param exception the exception that triggered the pool error event
   */
  public CreationErrorOccurredNotification (Object source, Exception exception) {

    super(TYPE, source, SEQUNCE_NUMBER.incrementAndGet(), System.currentTimeMillis());

    this.exception = exception;
  }

  /**
   * Returns the exception that caused the pool error.
   *
   * @return the exception
   */
  public Exception getException () {

    return exception;
  }
}
