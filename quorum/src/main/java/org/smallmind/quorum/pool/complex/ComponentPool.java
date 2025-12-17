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
 * Pool implementation that coordinates {@link ComponentInstance}s using pins and emits events/metrics.
 *
 * @param <C> component type managed by the pool
 */
public class ComponentPool<C> extends Pool {

  private final ConcurrentLinkedQueue<ComponentPoolEventListener> componentPoolEventListenerQueue = new ConcurrentLinkedQueue<ComponentPoolEventListener>();
  private final ComponentInstanceFactory<C> componentInstanceFactory;
  private final ComponentPinManager<C> componentPinManager;
  private final String name;
  private ComplexPoolConfig complexPoolConfig = new ComplexPoolConfig();

  /**
   * Constructs a pool with the provided name and instance factory using default configuration.
   *
   * @param name                     pool name for metrics and identification
   * @param componentInstanceFactory factory used to create new component instances
   */
  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory) {

    this.name = name;
    this.componentInstanceFactory = componentInstanceFactory;

    componentPinManager = new ComponentPinManager<C>(this);
  }

  /**
   * Constructs a pool with the provided name, instance factory, and configuration.
   *
   * @param name                     pool name for metrics and identification
   * @param componentInstanceFactory factory used to create new component instances
   * @param complexPoolConfig        configuration to apply
   */
  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory, ComplexPoolConfig complexPoolConfig) {

    this(name, componentInstanceFactory);

    this.complexPoolConfig = complexPoolConfig;
  }

  /**
   * Returns the name of the pool.
   *
   * @return pool name
   */
  public String getPoolName () {

    return name;
  }

  /**
   * Accessor for the instance factory.
   *
   * @return factory used to build component instances
   */
  public ComponentInstanceFactory<C> getComponentInstanceFactory () {

    return componentInstanceFactory;
  }

  /**
   * Returns the current pool configuration.
   *
   * @return configuration
   */
  public ComplexPoolConfig getComplexPoolConfig () {

    return complexPoolConfig;
  }

  /**
   * Updates the pool configuration.
   *
   * @param complexPoolConfig new configuration
   * @return this pool
   */
  public ComponentPool<C> setComplexPoolConfig (ComplexPoolConfig complexPoolConfig) {

    this.complexPoolConfig = complexPoolConfig;

    return this;
  }

  /**
   * Returns stack traces for components currently checked out when existential tracking is enabled.
   *
   * @return array of stack traces
   */
  public StackTrace[] getExistentialStackTraces () {

    return componentPinManager.getExistentialStackTraces();
  }

  /**
   * Adds a listener for pool events.
   *
   * @param listener listener to register
   */
  public void addComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPoolEventListenerQueue.add(listener);
  }

  /**
   * Removes a previously registered pool event listener.
   *
   * @param listener listener to remove
   */
  public void removeComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPoolEventListenerQueue.remove(listener);
  }

  /**
   * Notifies listeners of an error that occurred within the pool.
   *
   * @param exception exception that occurred
   */
  public void reportErrorOccurred (Exception exception) {

    ErrorReportingComponentPoolEvent<?> poolEvent = new ErrorReportingComponentPoolEvent<C>(this, exception);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportErrorOccurred(poolEvent);
    }
  }

  /**
   * Notifies listeners of the lease time for a component.
   *
   * @param leaseTimeNanos lease duration in nanoseconds
   */
  public void reportLeaseTimeNanos (long leaseTimeNanos) {

    LeaseTimeReportingComponentPoolEvent<?> poolEvent = new LeaseTimeReportingComponentPoolEvent<C>(this, leaseTimeNanos);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportLeaseTime(poolEvent);
    }
  }

  /**
   * Starts the pool by initializing factories and pin manager.
   *
   * @throws ComponentPoolException if initialization fails
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
   * Shuts down the pool and deconstructs all resources.
   *
   * @throws ComponentPoolException if shutdown fails
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
   * Obtains a component from the pool, instrumenting wait times.
   *
   * @return component instance
   * @throws ComponentPoolException if acquisition fails
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
   * Returns an instance to the pool, marking it for reuse.
   *
   * @param componentInstance component instance to return
   */
  public void returnInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.process(componentInstance, true);
  }

  /**
   * Terminates an instance and optionally replaces it.
   *
   * @param componentInstance instance to terminate
   */
  public void terminateInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.terminate(componentInstance, true, true);
  }

  /**
   * Removes the provided pin from service.
   *
   * @param componentPin  pin to remove
   * @param withPrejudice whether removal should force termination
   */
  public void removePin (ComponentPin<C> componentPin, boolean withPrejudice) {

    componentPinManager.remove(componentPin, false, withPrejudice, true);
  }

  /**
   * Terminates all components currently processing.
   */
  public void killAllProcessing () {

    componentPinManager.killAllProcessing();
  }

  /**
   * Returns the total size of the pool.
   *
   * @return number of managed components
   */
  public int getPoolSize () {

    return componentPinManager.getPoolSize();
  }

  /**
   * Returns the number of free components.
   *
   * @return number of available components
   */
  public int getFreeSize () {

    return componentPinManager.getFreeSize();
  }

  /**
   * Returns the number of components currently checked out.
   *
   * @return number of processing components
   */
  public int getProcessingSize () {

    return componentPinManager.getProcessingSize();
  }
}
