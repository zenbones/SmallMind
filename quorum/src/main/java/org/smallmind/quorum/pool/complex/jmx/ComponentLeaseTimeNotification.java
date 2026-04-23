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
 * JMX notification emitted by {@link ComponentPoolMonitor} each time a component is returned
 * to the pool and lease-time reporting is enabled.
 * <p>
 * Each instance carries the lease duration in nanoseconds and is assigned a globally unique,
 * monotonically increasing sequence number. The notification type is {@link #TYPE}
 * ({@value #TYPE}), which can be used in {@link javax.management.NotificationFilter}
 * implementations to select only lease-time notifications.
 */
public class ComponentLeaseTimeNotification extends Notification {

  private static final AtomicLong SEQUNCE_NUMBER = new AtomicLong(0);

  /**
   * Notification type string identifying lease-time notifications.
   */
  public static final String TYPE = "LEASE_TIME";

  private final long leaseTimeNanos;

  /**
   * Creates the notification with the given source and lease duration.
   * <p>
   * The sequence number is auto-incremented from a class-level counter; the timestamp is
   * set to the current wall-clock time.
   *
   * @param source         the MBean object that is the source of this notification (typically
   *                       the {@link javax.management.ObjectName} of the
   *                       {@link ComponentPoolMonitor})
   * @param leaseTimeNanos the duration in nanoseconds for which the component was leased
   */
  public ComponentLeaseTimeNotification (Object source, long leaseTimeNanos) {

    super(TYPE, source, SEQUNCE_NUMBER.incrementAndGet(), System.currentTimeMillis());

    this.leaseTimeNanos = leaseTimeNanos;
  }

  /**
   * Returns the lease duration in nanoseconds carried by this notification.
   *
   * @return the lease time in nanoseconds
   */
  public long getLeaseTimeNanos () {

    return leaseTimeNanos;
  }
}
