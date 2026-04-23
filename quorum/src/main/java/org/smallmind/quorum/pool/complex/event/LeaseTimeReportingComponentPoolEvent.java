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
package org.smallmind.quorum.pool.complex.event;

import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * Event fired each time a component is returned to the pool when lease-time reporting is
 * enabled.
 * <p>
 * Delivered to {@link ComponentPoolEventListener#reportLeaseTime} when
 * {@link org.smallmind.quorum.pool.complex.ComplexPoolConfig#isReportLeaseTimeNanos()} is
 * {@code true} and a component's {@link org.smallmind.quorum.pool.complex.ComponentPin#free()}
 * is called. Carries the originating pool and the exact nanosecond lease duration, allowing
 * listeners such as the JMX monitor to emit metrics or notifications.
 *
 * @param <C> the type of component managed by the originating pool
 */
public class LeaseTimeReportingComponentPoolEvent<C> extends ComponentPoolEvent<C> {

  private final long leaseTimeNanos;

  /**
   * Creates a lease-time event for the given pool.
   *
   * @param componentPool  the pool from which the component was returned
   * @param leaseTimeNanos the duration in nanoseconds for which the component was held by
   *                       a caller
   */
  public LeaseTimeReportingComponentPoolEvent (ComponentPool<C> componentPool, long leaseTimeNanos) {

    super(componentPool);

    this.leaseTimeNanos = leaseTimeNanos;
  }

  /**
   * Returns the lease duration in nanoseconds.
   *
   * @return the lease time from {@link org.smallmind.quorum.pool.complex.ComponentPin#serve()}
   * to {@link org.smallmind.quorum.pool.complex.ComponentPin#free()}, measured with
   * {@link System#nanoTime()}
   */
  public long getLeaseTimeNanos () {

    return leaseTimeNanos;
  }
}
