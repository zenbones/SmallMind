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
package org.smallmind.quorum.pool.complex;

/**
 * A time-delayed trigger that initiates deconstruction of a pooled component when a
 * configured limit expires.
 * <p>
 * Each fuse is associated with a single {@link ComponentPin} via a
 * {@link DeconstructionCoordinator}. A fuse becomes active when its ignition time is set by
 * calling {@link #setIgnitionTime(long)}, which simultaneously registers it with the
 * {@link DeconstructionQueue}. The queue's background thread polls every second and calls
 * {@link #ignite()} on any fuse whose time has elapsed.
 * <p>
 * Fuses are assigned a monotonically increasing ordinal at construction time, used to break
 * ties when two fuses have the same ignition time.
 * <p>
 * Concrete subclasses implement:
 * <ul>
 *   <li>{@link #isPrejudicial()} — whether ignition should force-terminate a component
 *       that is still checked out;</li>
 *   <li>{@link #free()} — called when the component is returned to the free queue;</li>
 *   <li>{@link #serve()} — called when the component is handed to a caller.</li>
 * </ul>
 */
public abstract class DeconstructionFuse {

  private final DeconstructionQueue deconstructionQueue;
  private final DeconstructionCoordinator deconstructionCoordinator;
  private final int ordinal;

  private long ignitionTime;

  /**
   * Registers this fuse with the given queue and coordinator, and obtains a unique ordinal
   * for ordering within the queue.
   *
   * @param deconstructionQueue       the queue that will call {@link #ignite()} when the fuse
   *                                  expires
   * @param deconstructionCoordinator the coordinator that orchestrates multi-fuse interactions
   *                                  and ultimately triggers pin removal
   */
  public DeconstructionFuse (DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    this.deconstructionQueue = deconstructionQueue;
    this.deconstructionCoordinator = deconstructionCoordinator;

    ordinal = deconstructionQueue.nextOrdinal();
  }

  /**
   * Returns whether igniting this fuse should trigger prejudicial (forced) removal of the
   * component, even when it is currently checked out by a caller.
   *
   * @return {@code true} for a prejudicial fuse (e.g. processing timeout);
   * {@code false} for a non-prejudicial fuse (e.g. idle or lease timeout)
   */
  public abstract boolean isPrejudicial ();

  /**
   * Responds to the component being placed back on the free queue.
   * <p>
   * Typical implementations either schedule ignition (idle-timeout fuses) or cancel any
   * pending ignition (processing-timeout fuses).
   */
  public abstract void free ();

  /**
   * Responds to the component being handed to a caller.
   * <p>
   * Typical implementations either schedule ignition (processing-timeout fuses) or cancel
   * any pending ignition (idle-timeout fuses).
   */
  public abstract void serve ();

  /**
   * Returns the ordinal assigned to this fuse at construction time, used to impose a
   * deterministic ordering among fuses registered at the same millisecond.
   *
   * @return the unique ordinal value
   */
  public int getOrdinal () {

    return ordinal;
  }

  /**
   * Returns the wall-clock time (milliseconds since the epoch) at which this fuse is
   * scheduled to ignite.
   *
   * @return the ignition timestamp in milliseconds
   */
  public long getIgnitionTime () {

    return ignitionTime;
  }

  /**
   * Sets the ignition time and registers this fuse with the {@link DeconstructionQueue}.
   * <p>
   * If the fuse was previously registered, calling this method again will add a second entry
   * to the queue; callers should call {@link #abort()} first to remove any prior registration.
   *
   * @param ignitionTime the absolute wall-clock time in milliseconds at which to ignite
   */
  public void setIgnitionTime (long ignitionTime) {

    this.ignitionTime = ignitionTime;
    deconstructionQueue.add(this);
  }

  /**
   * Removes this fuse from the {@link DeconstructionQueue}, cancelling a scheduled ignition.
   * Safe to call when the fuse is not currently queued.
   */
  public void abort () {

    deconstructionQueue.remove(this);
  }

  /**
   * Notifies the {@link DeconstructionCoordinator} that this fuse has expired, passing
   * {@link #isPrejudicial()} to govern whether the resulting pin removal is forced.
   */
  public void ignite () {

    deconstructionCoordinator.ignite(this, isPrejudicial());
  }

  /**
   * Returns the existential stack trace held by the coordinator, providing context about
   * which thread acquired the component associated with this fuse.
   *
   * @return the stack trace elements of the acquiring thread, or {@code null} if not tracked
   */
  public StackTraceElement[] getExistentialStackTrace () {

    return deconstructionCoordinator.getExistentialStackTrace();
  }
}
