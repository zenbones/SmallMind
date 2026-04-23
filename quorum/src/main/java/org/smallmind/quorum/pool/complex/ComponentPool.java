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

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.Pool;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;

/**
 * Full-featured component pool that adds validation, deconstruction timeouts, JMX monitoring,
 * Claxon metrics, and event notification to the base pooling machinery.
 * <p>
 * The pool delegates all pin lifecycle work to an internal {@link ComponentPinManager} and
 * exposes a simple three-operation API to callers:
 * <ul>
 *   <li>{@link #getComponent()} — acquires a component, blocking up to the configured wait
 *       time if none is immediately available;</li>
 *   <li>{@link #returnInstance(ComponentInstance)} — returns a healthy component so the pool
 *       can lend it to the next caller;</li>
 *   <li>{@link #terminateInstance(ComponentInstance)} — permanently discards a component,
 *       triggering replacement if the pool is below its minimum size.</li>
 * </ul>
 * Event listeners registered via {@link #addComponentPoolEventListener} receive error and
 * lease-time notifications. Acquisition wait time is instrumented with a Claxon speedometer
 * tagged {@link ClaxonTag#WAITED}.
 *
 * @param <C> the type of component dispensed by this pool
 */
public class ComponentPool<C> extends Pool {

  private final ConcurrentLinkedQueue<ComponentPoolEventListener> componentPoolEventListenerQueue = new ConcurrentLinkedQueue<ComponentPoolEventListener>();
  private final ComponentInstanceFactory<C> componentInstanceFactory;
  private final ComponentPinManager<C> componentPinManager;
  private final String name;
  private ComplexPoolConfig complexPoolConfig = new ComplexPoolConfig();

  /**
   * Creates a pool with the given name and factory, using default configuration.
   *
   * @param name                     a unique label for this pool used in metrics and JMX
   *                                 object names
   * @param componentInstanceFactory factory that creates and manages component instances
   */
  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory) {

    this.name = name;
    this.componentInstanceFactory = componentInstanceFactory;

    componentPinManager = new ComponentPinManager<C>(this);
  }

  /**
   * Creates a pool with the given name, factory, and explicit configuration.
   *
   * @param name                     a unique label for this pool
   * @param componentInstanceFactory factory that creates and manages component instances
   * @param complexPoolConfig        configuration controlling sizes, timeouts, and feature
   *                                 flags
   */
  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory, ComplexPoolConfig complexPoolConfig) {

    this(name, componentInstanceFactory);

    this.complexPoolConfig = complexPoolConfig;
  }

  /**
   * Returns the name of this pool as provided at construction time.
   *
   * @return the pool name
   */
  public String getPoolName () {

    return name;
  }

  /**
   * Returns the {@link ComponentInstanceFactory} used to create component instances.
   *
   * @return the factory
   */
  public ComponentInstanceFactory<C> getComponentInstanceFactory () {

    return componentInstanceFactory;
  }

  /**
   * Returns the current pool configuration.
   *
   * @return the active {@link ComplexPoolConfig}
   */
  public ComplexPoolConfig getComplexPoolConfig () {

    return complexPoolConfig;
  }

  /**
   * Replaces the pool configuration at runtime.
   * <p>
   * Because all configuration fields are atomic, changes take effect for subsequent
   * operations without requiring a restart.
   *
   * @param complexPoolConfig the new configuration; must not be {@code null}
   * @return this pool, for fluent chaining
   */
  public ComponentPool<C> setComplexPoolConfig (ComplexPoolConfig complexPoolConfig) {

    this.complexPoolConfig = complexPoolConfig;

    return this;
  }

  /**
   * Returns the acquisition stack traces of all components currently checked out by callers,
   * when existential awareness is enabled in the configuration.
   *
   * @return an array of {@link StackTrace} objects; empty when no checked-out components have
   * a recorded trace
   */
  public StackTrace[] getExistentialStackTraces () {

    return componentPinManager.getExistentialStackTraces();
  }

  /**
   * Registers a listener to receive pool events.
   * <p>
   * Listeners are stored in a {@link ConcurrentLinkedQueue} so this method is safe to call
   * from any thread without external synchronization.
   *
   * @param listener the listener to add; must not be {@code null}
   */
  public void addComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPoolEventListenerQueue.add(listener);
  }

  /**
   * Unregisters a previously registered listener.
   *
   * @param listener the listener to remove; no-op if not present
   */
  public void removeComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPoolEventListenerQueue.remove(listener);
  }

  /**
   * Broadcasts an error event to all registered listeners.
   * <p>
   * Called by {@link ComponentPinManager} when a component instance terminates unexpectedly.
   *
   * @param exception the exception that caused the error
   */
  public void reportErrorOccurred (Exception exception) {

    ErrorReportingComponentPoolEvent<?> poolEvent = new ErrorReportingComponentPoolEvent<C>(this, exception);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportErrorOccurred(poolEvent);
    }
  }

  /**
   * Broadcasts a lease-time event to all registered listeners.
   * <p>
   * Called by {@link ComponentPin#free()} when a component is returned and
   * {@link ComplexPoolConfig#isReportLeaseTimeNanos()} is enabled.
   *
   * @param leaseTimeNanos the duration, in nanoseconds, for which the component was leased
   */
  public void reportLeaseTimeNanos (long leaseTimeNanos) {

    LeaseTimeReportingComponentPoolEvent<?> poolEvent = new LeaseTimeReportingComponentPoolEvent<C>(this, leaseTimeNanos);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportLeaseTime(poolEvent);
    }
  }

  /**
   * Starts the pool by invoking the factory lifecycle and pre-warming the component set.
   * <p>
   * Calls {@link ComponentInstanceFactory#initialize()}, then
   * {@link ComponentPinManager#startup()}, then {@link ComponentInstanceFactory#startup()}.
   *
   * @throws ComponentPoolException if any step in the startup sequence fails
   */
  public void startup ()
    throws ComponentPoolException {

    try {
      componentInstanceFactory.initialize();
    } catch (Exception exception) {
      throw new ComponentPoolException(exception);
    }

    componentPinManager.startup();

    try {
      componentInstanceFactory.startup();
    } catch (Exception exception) {
      throw new ComponentPoolException(exception);
    }
  }

  /**
   * Shuts down the pool by reversing the startup sequence.
   * <p>
   * Calls {@link ComponentInstanceFactory#shutdown()}, then
   * {@link ComponentPinManager#shutdown()} (which terminates all components), then
   * {@link ComponentInstanceFactory#deconstruct()}.
   *
   * @throws ComponentPoolException if any step in the shutdown sequence fails
   */
  public void shutdown ()
    throws ComponentPoolException {

    try {
      componentInstanceFactory.shutdown();
    } catch (Exception exception) {
      throw new ComponentPoolException(exception);
    }

    componentPinManager.shutdown();

    try {
      componentInstanceFactory.deconstruct();
    } catch (Exception exception) {
      throw new ComponentPoolException(exception);
    }
  }

  /**
   * Acquires a component from the pool, blocking up to the configured acquire wait time if no
   * component is immediately available.
   * <p>
   * The acquisition is instrumented with a Claxon speedometer tagged {@link ClaxonTag#WAITED}.
   *
   * @return a component ready for use; the caller must eventually call
   * {@link #returnInstance(ComponentInstance)} or
   * {@link #terminateInstance(ComponentInstance)}
   * @throws ComponentPoolException if the pool has not been started, if the wait is interrupted,
   *                                if the maximum wait time is exceeded, or if component creation
   *                                fails
   */
  public C getComponent ()
    throws ComponentPoolException {

    try {

      return Instrument.with(ComponentPool.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("pool", getPoolName()), new Tag("event", ClaxonTag.WAITED.getDisplay())).on(
        () -> componentPinManager.serve().serve()
      );
    } catch (Throwable throwable) {
      throw new ComponentPoolException(throwable);
    }
  }

  /**
   * Returns a healthy component instance to the pool so it can be leased again.
   * <p>
   * Triggers lease-time metrics and resets the deconstruction coordinator for the pin.
   *
   * @param componentInstance the instance to return; must have been obtained from this pool
   */
  public void returnInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.process(componentInstance, true);
  }

  /**
   * Permanently removes a component instance from the pool and triggers replacement if
   * the pool is below its minimum size.
   * <p>
   * Called by {@link org.smallmind.quorum.namespace.pool.JavaContextComponentInstance} when
   * a communication failure makes the component unusable.
   *
   * @param componentInstance the instance to terminate; must have been obtained from this pool
   */
  public void terminateInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.terminate(componentInstance, true, true);
  }

  /**
   * Removes the given pin from the pool's backing structures.
   * <p>
   * Called by {@link ComponentPin#kaboom(boolean)} when a deconstruction fuse fires. The
   * {@code withPrejudice} flag is forwarded from the fuse to control whether the removal is
   * forced even when the pin is not on the free queue.
   *
   * @param componentPin  the pin to remove
   * @param withPrejudice {@code true} to force removal when the pin is currently processing
   */
  public void removePin (ComponentPin<C> componentPin, boolean withPrejudice) {

    componentPinManager.remove(componentPin, false, withPrejudice, true);
  }

  /**
   * Forcibly terminates all component instances that are currently checked out.
   */
  public void killAllProcessing () {

    componentPinManager.killAllProcessing();
  }

  /**
   * Returns the total number of component instances managed by this pool, including both
   * free (idle) and processing (checked out) instances.
   *
   * @return the total pool size
   */
  public int getPoolSize () {

    return componentPinManager.getPoolSize();
  }

  /**
   * Returns the number of component instances currently sitting on the free queue, available
   * for immediate acquisition.
   *
   * @return the number of idle components
   */
  public int getFreeSize () {

    return componentPinManager.getFreeSize();
  }

  /**
   * Returns the number of component instances currently checked out by callers.
   *
   * @return the number of components in the processing state
   */
  public int getProcessingSize () {

    return componentPinManager.getProcessingSize();
  }
}
