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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;

/**
 * Internal handle that pairs a {@link ComponentInstance} with the pool's tracking metadata.
 * <p>
 * Each pin wraps exactly one {@link ComponentInstance} and records:
 * <ul>
 *   <li>a lease-start timestamp (set on every {@link #serve()} call) used to measure lease
 *       duration when the component is returned;</li>
 *   <li>an optional {@link DeconstructionCoordinator} that fires when any configured timeout
 *       (idle, lease, or processing) expires;</li>
 *   <li>a terminated flag that prevents double-removal and guards against concurrent
 *       deconstruction races.</li>
 * </ul>
 * Instances are held in {@link ComponentPinManager}'s backing map and free queue, and are
 * never exposed directly to pool callers.
 *
 * @param <C> the type of component managed by the wrapped {@link ComponentInstance}
 */
public class ComponentPin<C> {

  private final ComponentPool<C> componentPool;
  private final ComponentInstance<C> componentInstance;
  private final AtomicBoolean terminated = new AtomicBoolean(false);
  private DeconstructionCoordinator deconstructionCoordinator;
  private long leaseStartNanos;

  /**
   * Creates a pin wrapping {@code componentInstance} and, when the pool's configuration
   * requires deconstruction, initialises a {@link DeconstructionCoordinator} and calls
   * {@link DeconstructionCoordinator#free()} to start idle-timeout tracking.
   *
   * @param componentPool       the pool that owns this pin
   * @param deconstructionQueue the shared queue used to schedule fuse ignitions
   * @param componentInstance   the component instance being wrapped
   */
  protected ComponentPin (ComponentPool<C> componentPool, DeconstructionQueue deconstructionQueue, ComponentInstance<C> componentInstance) {

    this.componentPool = componentPool;
    this.componentInstance = componentInstance;

    if (componentPool.getComplexPoolConfig().requiresDeconstruction()) {
      deconstructionCoordinator = new DeconstructionCoordinator(componentPool, deconstructionQueue, this);
      deconstructionCoordinator.free();
    }
  }

  /**
   * Returns the {@link ComponentInstance} wrapped by this pin.
   *
   * @return the component instance
   */
  protected ComponentInstance<C> getComponentInstance () {

    return componentInstance;
  }

  /**
   * Hands the component to a caller, recording the lease start timestamp and notifying the
   * deconstruction coordinator that the component is now being served.
   * <p>
   * The lease-start timestamp is captured in the {@code finally} block so it is always set
   * even if {@link ComponentInstance#serve()} throws.
   *
   * @return the underlying component value returned by {@link ComponentInstance#serve()}
   * @throws Exception if {@link ComponentInstance#serve()} throws
   */
  protected C serve ()
    throws Exception {

    try {

      return componentInstance.serve();
    } finally {
      if (deconstructionCoordinator != null) {
        deconstructionCoordinator.serve();
      }

      leaseStartNanos = System.nanoTime();
    }
  }

  /**
   * Called when a component is returned to the pool.
   * <p>
   * Computes the lease duration from the stored start time, updates Claxon metrics,
   * optionally fires the lease-time reporting event, and resets the deconstruction coordinator
   * to idle-timeout tracking mode.
   */
  protected void free () {

    long leaseTime = System.nanoTime() - leaseStartNanos;

    if (componentPool.getComplexPoolConfig().isReportLeaseTimeNanos()) {
      componentPool.reportLeaseTimeNanos(leaseTime);
    }

    Instrument.with(ComponentPin.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("pool", componentPool.getPoolName()), new Tag("event", ClaxonTag.RELEASED.getDisplay())).update(leaseTime, TimeUnit.NANOSECONDS);

    if (deconstructionCoordinator != null) {
      deconstructionCoordinator.free();
    }
  }

  /**
   * Returns {@code true} if this pin has been permanently removed from service, either by
   * a deconstruction fuse igniting or by the pool manager terminating it.
   *
   * @return {@code true} after the first call to {@link #fizzle()} or {@link #kaboom(boolean)}
   */
  protected boolean isTerminated () {

    return terminated.get();
  }

  /**
   * Marks this pin as terminated and aborts any pending deconstruction fuses without
   * removing the pin from the pool's backing structures.
   * <p>
   * This is the "quiet" termination path: the caller ({@link ComponentPinManager}) is
   * responsible for closing the instance and updating bookkeeping. Called at most once;
   * subsequent calls are no-ops.
   */
  protected void fizzle () {

    if (terminated.compareAndSet(false, true)) {
      if (deconstructionCoordinator != null) {
        deconstructionCoordinator.abort();
      }
    }
  }

  /**
   * Requests that the owning pool remove this pin from service, called by a deconstruction
   * fuse when its timer fires.
   * <p>
   * Guards against double-removal via the {@code terminated} flag. Called at most once;
   * subsequent calls are no-ops.
   *
   * @param withPrejudice {@code true} if the removal should be treated as forced termination
   *                      (e.g. a processing-timeout fuse), {@code false} for graceful removal
   *                      (e.g. idle or lease fuses)
   */
  protected void kaboom (boolean withPrejudice) {

    if (terminated.compareAndSet(false, true)) {
      componentPool.removePin(this, withPrejudice);
    }
  }

  /**
   * Returns the existential stack trace captured when this component was last served, if
   * existential awareness is enabled in the pool configuration.
   *
   * @return the stack trace of the acquiring thread, or {@code null} if not tracked
   */
  public StackTraceElement[] getExistentialStackTrace () {

    return componentInstance.getExistentialStackTrace();
  }
}
