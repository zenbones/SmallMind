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
package org.smallmind.web.grizzly.strategy;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.localization.LogMessages;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.phalanx.worker.WorkManager;

public class WorkerPoolIOStrategy extends WorkManager<IOStrategyWorker, IOPackage> implements IOStrategy {

  private static final Logger logger = Grizzly.logger(WorkerPoolIOStrategy.class);

  public WorkerPoolIOStrategy (MetricConfiguration metricConfiguration, int concurrencyLimit) {

    super(metricConfiguration, IOStrategyWorker.class, concurrencyLimit);
  }

  @Override
  public boolean executeIoEvent (Connection connection, IOEvent ioEvent)
    throws IOException {

    return executeIoEvent(connection, ioEvent, true);
  }

  @Override
  public boolean executeIoEvent (final Connection connection, final IOEvent ioEvent, final boolean ioEventEnabled)
    throws IOException {

    try {
      execute(new IOPackage(connection, ioEvent, ioEventEnabled));
    } catch (Exception exception) {
      logger.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_IOSTRATEGY_UNCAUGHT_EXCEPTION(), exception);
      connection.closeSilently();
    }
    return true;
  }

  @Override
  public Executor getThreadPoolFor (final Connection connection, final IOEvent ioEvent) {

    return null;
  }

  @Override
  public ThreadPoolConfig createDefaultWorkerPoolConfig (final Transport transport) {

    return null;
  }
}
