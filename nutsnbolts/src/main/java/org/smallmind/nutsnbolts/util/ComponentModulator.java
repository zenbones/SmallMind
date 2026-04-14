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
 * Thread-safe status gate around a {@link ComponentStatus} value.
 *
 * Callers can update the current status directly or wait until the status enters or leaves a supplied set without
 * adding their own polling loop.
 */
public class ComponentModulator {

  private final AtomicReference<ComponentStatus> statusRef;

  /**
   * Creates a modulator with an initial status of {@link ComponentStatus#STOPPED}.
   */
  public ComponentModulator () {

    this(ComponentStatus.STOPPED);
  }

  /**
   * Creates a modulator with the given initial status.
   *
   * @param status initial component status
   */
  public ComponentModulator (ComponentStatus status) {

    this.statusRef = new AtomicReference<>(status);
  }

  /**
   * Returns the current status without blocking.
   *
   * @return current component status
   */
  public final ComponentStatus get () {

    return statusRef.get();
  }

  /**
   * Replaces the current status and wakes any threads waiting in {@link #awaitIn(ComponentStatus...)} or
   * {@link #awaitNotIn(ComponentStatus...)}.
   *
   * @param status replacement status
   */
  public final synchronized void set (ComponentStatus status) {

    statusRef.set(status);

    notifyAll();
  }

  /**
   * Atomically changes the current status when it matches the expected value.
   *
   * Waiting threads are notified only when the update succeeds.
   *
   * @param expectedStatus status required for the update to proceed
   * @param newStatus replacement status to install on success
   * @return {@code true} when the status changed
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
   * Waits until the current status matches one of the supplied values.
   *
   * @param statuses acceptable statuses
   * @return the matching current status
   * @throws InterruptedException if the waiting thread is interrupted
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
   * Waits until the current status no longer matches any of the supplied values.
   *
   * @param statuses statuses to avoid
   * @return the first current status that is not in the supplied set
   * @throws InterruptedException if the waiting thread is interrupted
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
   * Tests whether the current status matches one of the supplied values.
   *
   * @param statuses candidate statuses
   * @return the current status when it matches, otherwise {@code null}
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
   * Tests whether the current status differs from all of the supplied values.
   *
   * @param statuses statuses to reject
   * @return the current status when it differs from every supplied value, otherwise {@code null}
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
