/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.scribe.pen.LoggerManager;

public class MaxProcessingTimeDeconstructionFuse extends DeconstructionFuse {

  private final ComponentPool<?> componentPool;
  private final AtomicInteger generation = new AtomicInteger(0);
  private final AtomicInteger generationServed = new AtomicInteger(0);

  protected MaxProcessingTimeDeconstructionFuse (ComponentPool<?> componentPool, DeconstructionQueue deconstructionQueue, DeconstructionCoordinator deconstructionCoordinator) {

    super(deconstructionQueue, deconstructionCoordinator);

    this.componentPool = componentPool;
  }

  @Override
  public boolean isPrejudicial () {

    return true;
  }

  @Override
  public synchronized void free () {

    generation.incrementAndGet();
    abort();
  }

  @Override
  public void serve () {

    generationServed.set(generation.incrementAndGet());
    setIgnitionTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(componentPool.getComplexPoolConfig().getMaxProcessingTimeSeconds()));
  }

  @Override
  public synchronized void ignite () {

    if (generationServed.get() == generation.get()) {

      StackTraceElement[] stackTraceElements;

      super.ignite();

      if (((stackTraceElements = getExistentialStackTrace()) != null) && (stackTraceElements.length > 0)) {
        LoggerManager.getLogger(MaxProcessingTimeDeconstructionFuse.class).warn(Arrays.toString(getExistentialStackTrace()));
      }
    }
  }
}