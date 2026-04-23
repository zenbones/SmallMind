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
package org.smallmind.nutsnbolts.util;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe holder for a {@link ComponentStatus} that allows callers to set the status or block until
 * it enters or leaves a specified set of values.
 */
public class ComponentModulator {

  private final AtomicReference<ComponentStatus> statusRef;

  /**
   * Constructs a modulator with an initial status of {@link ComponentStatus#STOPPED}.
   */
  public ComponentModulator () {

    this(ComponentStatus.STOPPED);
  }

  /**
   * Constructs a modulator with the specified initial status.
   *
   * @param status the initial component status
   */
  public ComponentModulator (ComponentStatus status) {

    this.statusRef = new AtomicReference<>(status);
  }

  /**
   * Returns the current status without blocking.
   *
   * @return the current component status
   */
  public final ComponentStatus get () {

    return statusRef.get();
  }

  /**
   * Sets the current status unconditionally and notifies all threads waiting in
   * {@link #awaitIn(ComponentStatus...)} or {@link #awaitNotIn(ComponentStatus...)}.
   *
   * @param status the new component status
   */
  public final synchronized void set (ComponentStatus status) {

    statusRef.set(status);

    notifyAll();
  }

  /**
   * Atomically sets the status to {@code newStatus} if the current status equals {@code expectedStatus},
   * notifying waiting threads only on a successful update.
   *
   * @param expectedStatus the status that must be current for the update to proceed
   * @param newStatus      the replacement status to install on success
   * @return {@code true} if the status was updated; {@code false} if the current status did not match
   */
  public final synchronized boolean compareAndSet (ComponentStatus expectedStatus, ComponentStatus newStatus) {

    if (statusRef.compareAndSet(expectedStatus, newStatus)) {
      notifyAll();

      return true;
    } else {

      return false;
    }
  }

  /**
   * Blocks the calling thread until the current status is one of the supplied values.
   *
   * @param statuses the set of acceptable statuses to wait for
   * @return the current status at the moment it matched one of the supplied values
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public final synchronized ComponentStatus awaitIn (ComponentStatus... statuses)
    throws InterruptedException {

    ComponentStatus status;

    while ((status = isIn(statuses)) == null) {
      wait();
    }

    return status;
  }

  /**
   * Blocks the calling thread until the current status is not any of the supplied values.
   *
   * @param statuses the set of statuses that should no longer be current
   * @return the current status at the moment it differed from all supplied values
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public final synchronized ComponentStatus awaitNotIn (ComponentStatus... statuses)
    throws InterruptedException {

    ComponentStatus status;

    while ((status = notIn(statuses)) == null) {
      wait();
    }

    return status;
  }

  /**
   * Returns the current status if it matches one of the supplied values, or {@code null} if it does not.
   *
   * @param statuses the candidate statuses to check against
   * @return the current status if it is in the supplied set, otherwise {@code null}
   */
  private ComponentStatus isIn (ComponentStatus... statuses) {

    ComponentStatus currentStatus = statusRef.get();

    for (ComponentStatus status : statuses) {
      if (status.equals(currentStatus)) {

        return currentStatus;
      }
    }

    return null;
  }

  /**
   * Returns the current status if it does not match any of the supplied values, or {@code null} if it does.
   *
   * @param statuses the statuses to test against
   * @return the current status if it differs from every supplied value, otherwise {@code null}
   */
  private ComponentStatus notIn (ComponentStatus... statuses) {

    ComponentStatus currentStatus = statusRef.get();

    for (ComponentStatus status : statuses) {
      if (status.equals(currentStatus)) {

        return null;
      }
    }

    return currentStatus;
  }
}
