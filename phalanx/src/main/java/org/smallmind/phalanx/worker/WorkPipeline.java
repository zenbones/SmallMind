/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.phalanx.worker;

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;

public class WorkPipeline<W extends Worker<T>, T> implements MetricConfigurationProvider {

  private enum State {STOPPED, STARTING, STARTED, STOPPING}

  private final AtomicReference<State> stateRef = new AtomicReference<>(State.STOPPED);
  private final MetricConfiguration metricConfiguration;
  private final WorkManager<W, T>[] workManagers;

  public WorkPipeline (MetricConfiguration metricConfiguration, WorkManager<W, T>... workManagers) {

    this.metricConfiguration = metricConfiguration;
    this.workManagers = workManagers;
  }

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  public void startUp (WorkerFactory<W, T> workerFactory)
    throws InterruptedException {

    if (stateRef.compareAndSet(State.STOPPED, State.STARTING)) {
      for (WorkManager<W, T> workManager : workManagers) {
        workManager.startUp(metricConfiguration, workerFactory);
      }
      stateRef.set(State.STARTED);
    } else {
      while (State.STARTING.equals(stateRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  public void execute (final T transfer)
    throws Exception {

    if (!State.STARTED.equals(stateRef.get())) {
      throw new WorkManagerException("%s is not in the 'started' State", this.getClass().getSimpleName());
    }

    InstrumentationManager.execute(new ChronometerInstrument(this, new MetricProperty("event", MetricType.ACQUIRE_WORKER.getDisplay())) {

      @Override
      public void withChronometer ()
        throws Exception {

        boolean success;

        do {
          success = workManagers[transfer.hashCode() % workManagers.length].execute(transfer);
        } while (State.STARTED.equals(stateRef.get()) && (!success));
        if (!success) {
          throw new WorkManagerException("%s is not in the 'started' State", this.getClass().getSimpleName());
        }
      }
    });
  }

  public void shutDown ()
    throws InterruptedException {

    if (stateRef.compareAndSet(State.STARTED, State.STOPPING)) {
      for (WorkManager<W, T> workManager : workManagers) {
        workManager.shutDown();
      }
      stateRef.set(State.STOPPED);
    } else {
      while (State.STOPPING.equals(stateRef.get())) {
        Thread.sleep(100);
      }
    }
  }
}
