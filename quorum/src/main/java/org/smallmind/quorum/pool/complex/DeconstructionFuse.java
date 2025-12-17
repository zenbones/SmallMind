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
 * Represents a countdown fuse that coordinates delayed destruction of pooled components.
 * Each fuse is ordered and can be ignited, aborted, or freed based on pool state.
 */
public abstract class DeconstructionFuse {

  private final DeconstructionQueue deconstructionQueue;
  private final DeconstructionCoordinator deconstructionCoordinator;
  private final int ordinal;

  private long ignitionTime;

  /**
   * Creates a fuse that will register itself with the provided queue and coordinator.
   *
   * @param deconstructionQueue       queue tracking pending fuses
   * @param deconstructionCoordinator coordinator invoked when a fuse ignites
   */
  public DeconstructionFuse (DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    this.deconstructionQueue = deconstructionQueue;
    this.deconstructionCoordinator = deconstructionCoordinator;

    ordinal = deconstructionQueue.nextOrdinal();
  }

  /**
   * Indicates whether triggering this fuse should be treated as prejudicial destruction.
   *
   * @return {@code true} if the component should be destroyed with prejudice
   */
  public abstract boolean isPrejudicial ();

  /**
   * Releases any resources held by the fuse.
   */
  public abstract void free ();

  /**
   * Activates the fuse's serve behavior (e.g., check state or schedule).
   */
  public abstract void serve ();

  /**
   * Returns the ordering value assigned to the fuse.
   *
   * @return ordinal position
   */
  public int getOrdinal () {

    return ordinal;
  }

  /**
   * Returns the time at which the fuse is scheduled to ignite.
   *
   * @return ignition timestamp in milliseconds
   */
  public long getIgnitionTime () {

    return ignitionTime;
  }

  /**
   * Sets the ignition time and registers the fuse with the queue.
   *
   * @param ignitionTime timestamp in milliseconds
   */
  public void setIgnitionTime (long ignitionTime) {

    this.ignitionTime = ignitionTime;
    deconstructionQueue.add(this);
  }

  /**
   * Cancels the fuse before it ignites.
   */
  public void abort () {

    deconstructionQueue.remove(this);
  }

  /**
   * Notifies the coordinator that the fuse should ignite.
   */
  public void ignite () {

    deconstructionCoordinator.ignite(this, isPrejudicial());
  }

  /**
   * Exposes the stack trace associated with the owning component.
   *
   * @return stack trace for diagnostic purposes
   */
  public StackTraceElement[] getExistentialStackTrace () {

    return deconstructionCoordinator.getExistentialStackTrace();
  }
}
