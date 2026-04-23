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
package org.smallmind.claxon.registry.meter;

import org.smallmind.claxon.registry.Clock;

/**
 * Factory abstraction responsible for constructing {@link Meter} instances on behalf
 * of the Claxon registry.
 *
 * <p>Implementations receive a registry-supplied {@link Clock} at construction time so
 * that time-sensitive meters (such as {@link Tachometer} or {@link Trace}) can be
 * initialised with the same clock as the rest of the registry. Implementations that
 * do not require a clock (such as {@link GaugeBuilder} or {@link TallyBuilder}) are
 * free to ignore the parameter.</p>
 *
 * <p>This interface is marked {@code @FunctionalInterface} so that simple,
 * clock-agnostic builders can be expressed as lambda expressions or method references.</p>
 *
 * @param <M> the concrete {@link Meter} type produced by this builder
 */
@FunctionalInterface
public interface MeterBuilder<M extends Meter> {

  /**
   * Builds and returns a new {@link Meter} instance of type {@code M}.
   *
   * @param clock the registry clock to be used by time-sensitive meters; may be
   *              ignored by implementations that do not require a clock
   * @return a fully initialised {@link Meter} ready to receive updates
   */
  M build (Clock clock);
}
