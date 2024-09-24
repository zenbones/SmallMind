/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.Pool;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;

public class ComponentPool<C> extends Pool {

  private final ConcurrentLinkedQueue<ComponentPoolEventListener> componentPoolEventListenerQueue = new ConcurrentLinkedQueue<ComponentPoolEventListener>();
  private final ComponentInstanceFactory<C> componentInstanceFactory;
  private final ComponentPinManager<C> componentPinManager;
  private final String name;
  private ComplexPoolConfig complexPoolConfig = new ComplexPoolConfig();

  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory) {

    this.name = name;
    this.componentInstanceFactory = componentInstanceFactory;

    componentPinManager = new ComponentPinManager<C>(this);
  }

  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory, ComplexPoolConfig complexPoolConfig) {

    this(name, componentInstanceFactory);

    this.complexPoolConfig = complexPoolConfig;
  }

  public String getPoolName () {

    return name;
  }

  public ComponentInstanceFactory<C> getComponentInstanceFactory () {

    return componentInstanceFactory;
  }

  public ComplexPoolConfig getComplexPoolConfig () {

    return complexPoolConfig;
  }

  public ComponentPool<C> setComplexPoolConfig (ComplexPoolConfig complexPoolConfig) {

    this.complexPoolConfig = complexPoolConfig;

    return this;
  }

  public StackTrace[] getExistentialStackTraces () {

    return componentPinManager.getExistentialStackTraces();
  }

  public void addComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPoolEventListenerQueue.add(listener);
  }

  public void removeComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPoolEventListenerQueue.remove(listener);
  }

  public void reportErrorOccurred (Exception exception) {

    ErrorReportingComponentPoolEvent<?> poolEvent = new ErrorReportingComponentPoolEvent<C>(this, exception);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportErrorOccurred(poolEvent);
    }
  }

  public void reportLeaseTimeNanos (long leaseTimeNanos) {

    LeaseTimeReportingComponentPoolEvent<?> poolEvent = new LeaseTimeReportingComponentPoolEvent<C>(this, leaseTimeNanos);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportLeaseTime(poolEvent);
    }
  }

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

  public C getComponent ()
    throws ComponentPoolException {

    try {

      return Instrument.with(ComponentPool.class, SpeedometerBuilder.instance(), new Tag("pool", getPoolName()), new Tag("event", ClaxonTag.WAITED.getDisplay())).on(
        () -> componentPinManager.serve().serve()
      );
    } catch (Throwable throwable) {
      throw new ComponentPoolException(throwable);
    }
  }

  public void returnInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.process(componentInstance, true);
  }

  public void terminateInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.terminate(componentInstance, true, true);
  }

  public void removePin (ComponentPin<C> componentPin, boolean withPrejudice) {

    componentPinManager.remove(componentPin, false, withPrejudice, true);
  }

  public void killAllProcessing () {

    componentPinManager.killAllProcessing();
  }

  public int getPoolSize () {

    return componentPinManager.getPoolSize();
  }

  public int getFreeSize () {

    return componentPinManager.getFreeSize();
  }

  public int getProcessingSize () {

    return componentPinManager.getProcessingSize();
  }
}
