/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import javax.management.ObjectName;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.PoolManager;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.jmx.ComponentPoolMonitor;
import org.smallmind.quorum.pool.instrument.MetricInteraction;

public class ComponentPool<C> {

  private final ConcurrentLinkedQueue<ComponentPoolEventListener> componentPoolEventListenerQueue = new ConcurrentLinkedQueue<ComponentPoolEventListener>();
  private final ComponentInstanceFactory<C> componentInstanceFactory;
  private final ComponentPinManager<C> componentPinManager;
  private final String name;
  private ComplexPoolConfig complexPoolConfig = new ComplexPoolConfig();

  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory)
    throws ComponentPoolException {

    MetricConfiguration metricConfiguration;
    MetricRegistry metricRegistry;

    this.name = name;
    this.componentInstanceFactory = componentInstanceFactory;

    componentPinManager = new ComponentPinManager<C>(this);

    if ((PoolManager.getPool() != null) && ((metricConfiguration = PoolManager.getPool().getMetricConfiguration()) != null) && metricConfiguration.isInstrumented() && ((metricRegistry = InstrumentationManager.getMetricRegistry()) != null) && (metricRegistry.getServer() != null)) {
      try {
        metricRegistry.getServer().registerMBean(new ComponentPoolMonitor(this), new ObjectName(metricConfiguration.getMetricDomain().getDomain() + ":" + "pool=" + name));
      } catch (Exception exception) {
        throw new ComponentPoolException(exception);
      }
    }
  }

  public ComponentPool (String name, ComponentInstanceFactory<C> componentInstanceFactory, ComplexPoolConfig complexPoolConfig)
    throws ComponentPoolException {

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

    ErrorReportingComponentPoolEvent poolEvent = new ErrorReportingComponentPoolEvent<C>(this, exception);

    for (ComponentPoolEventListener listener : componentPoolEventListenerQueue) {
      listener.reportErrorOccurred(poolEvent);
    }
  }

  public void reportLeaseTimeNanos (long leaseTimeNanos) {

    LeaseTimeReportingComponentPoolEvent poolEvent = new LeaseTimeReportingComponentPoolEvent<C>(this, leaseTimeNanos);

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

      return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<C>(PoolManager.getPool().getMetricConfiguration(), new MetricProperty("pool", getPoolName()), new MetricProperty("event", MetricInteraction.WAITING.getDisplay())) {

        @Override
        public C withChronometer () throws Exception {

          return componentPinManager.serve().serve();
        }
      });
    } catch (Throwable throwable) {
      throw new ComponentPoolException(throwable);
    }
  }

  public void returnInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.process(componentInstance);
  }

  public void terminateInstance (ComponentInstance<C> componentInstance) {

    componentPinManager.terminate(componentInstance);
  }

  public void removePin (ComponentPin<C> componentPin, boolean withPrejudice) {

    componentPinManager.remove(componentPin, withPrejudice);
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
