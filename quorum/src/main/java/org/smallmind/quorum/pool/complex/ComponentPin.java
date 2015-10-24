/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.pool.PoolManager;
import org.smallmind.quorum.pool.instrument.MetricInteraction;

public class ComponentPin<C> {

  private final ComponentPool<C> componentPool;
  private final ComponentInstance<C> componentInstance;
  private final AtomicBoolean terminated = new AtomicBoolean(false);
  private DeconstructionCoordinator deconstructionCoordinator;
  private long leaseStartNanos;

  protected ComponentPin (ComponentPool<C> componentPool, DeconstructionQueue deconstructionQueue, ComponentInstance<C> componentInstance) {

    this.componentPool = componentPool;
    this.componentInstance = componentInstance;

    if (componentPool.getComplexPoolConfig().requiresDeconstruction()) {
      deconstructionCoordinator = new DeconstructionCoordinator(componentPool, deconstructionQueue, this);
      deconstructionCoordinator.free();
    }
  }

  protected ComponentInstance<C> getComponentInstance () {

    return componentInstance;
  }

  protected C serve ()
    throws Exception {

    try {

      return componentInstance.serve();
    } finally {
      if (deconstructionCoordinator != null) {
        deconstructionCoordinator.serve();
      }

      leaseStartNanos = Clocks.EPOCH.getClock().getTimeNanoseconds();
    }
  }

  protected void free () {

    long leaseTime = Clocks.EPOCH.getClock().getTimeNanoseconds() - leaseStartNanos;

    if (componentPool.getComplexPoolConfig().isReportLeaseTimeNanos()) {
      componentPool.reportLeaseTimeNanos(leaseTime);
    }

    InstrumentationManager.instrumentWithChronometer(PoolManager.getPool(), leaseTime, TimeUnit.NANOSECONDS, new MetricProperty("pool", componentPool.getPoolName()), new MetricProperty("event", MetricInteraction.PROCESSING.getDisplay()));

    if (deconstructionCoordinator != null) {
      deconstructionCoordinator.free();
    }
  }

  protected boolean isTerminated () {

    return terminated.get();
  }

  protected void fizzle () {

    if (terminated.compareAndSet(false, true)) {
      if (deconstructionCoordinator != null) {
        deconstructionCoordinator.abort();
      }
    }
  }

  protected void kaboom (boolean withPrejudice) {

    if (terminated.compareAndSet(false, true)) {
      componentPool.removePin(this, withPrejudice);
    }
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return componentInstance.getExistentialStackTrace();
  }
}
