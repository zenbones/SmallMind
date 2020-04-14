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
package org.smallmind.claxon.registry;

import java.util.Observable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.smallmind.claxon.registry.meter.MeterBuilder;

public class Instrumentation {

  private ClaxonRegistry registry;
  private MeterBuilder<?> builder;
  private Tag[] tags;
  private String identifier;
  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  public Instrumentation (ClaxonRegistry registry, String identifier, MeterBuilder<?> builder, Tag... tags) {

    this.registry = registry;
    this.identifier = identifier;
    this.builder = builder;
    this.tags = tags;
  }

  public Instrumentation as (TimeUnit timeUnit) {

    this.timeUnit = timeUnit;

    return this;
  }

  public <O extends Observable> O track (O observable) {

    return registry.track(identifier, builder, observable, tags);
  }

  public <T> T track (T measured, Function<T, Long> measurement) {

    return registry.track(identifier, builder, measured, measurement, tags);
  }

  public void update (long value) {

    registry.instrument(registry.register(identifier, builder, tags), value);
  }

  public void on (SansResultExecutable sansResultExecutable)
    throws Throwable {

    registry.instrument(registry.register(identifier, builder, tags), timeUnit, sansResultExecutable);
  }

  public <T> T on (WithResultExecutable<T> withResultExecutable)
    throws Throwable {

    return registry.instrument(registry.register(identifier, builder, tags), timeUnit, withResultExecutable);
  }
}
