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
 * A {@link MeterBuilder} adapter that delegates meter construction to a
 * {@link BuilderConstructor}, allowing a fresh {@link MeterBuilder} to be created
 * for each meter that is built.
 *
 * <p>This indirection is useful when the same logical meter type must be registered
 * multiple times with independent builder state. Rather than sharing a single
 * pre-configured builder instance, a {@code MeterFactory} calls
 * {@link BuilderConstructor#construct()} each time {@link #build(Clock)} is invoked,
 * producing a brand-new builder whose defaults can then be applied before the meter
 * is constructed.</p>
 *
 * @param <M> the concrete {@link Meter} type produced by this factory
 */
public class MeterFactory<M extends Meter> implements MeterBuilder<M> {

  /**
   * The supplier used to obtain a fresh {@link MeterBuilder} on every
   * {@link #build(Clock)} invocation.
   */
  private final BuilderConstructor<M> factory;

  /**
   * Creates a {@code MeterFactory} that will delegate to the provided
   * {@link BuilderConstructor}.
   *
   * @param factory the constructor that produces a new {@link MeterBuilder} each time
   *                {@link #build(Clock)} is called; must not be {@code null}
   */
  public MeterFactory (BuilderConstructor<M> factory) {

    this.factory = factory;
  }

  /**
   * Convenience factory method that wraps a {@link BuilderConstructor} in a
   * {@code MeterFactory} without requiring explicit type parameters at the call site.
   *
   * @param factory the constructor that produces new {@link MeterBuilder} instances
   * @param <M>     the concrete {@link Meter} type
   * @return a new {@code MeterFactory} backed by {@code factory}
   */
  public static <M extends Meter> MeterFactory<M> instance (BuilderConstructor<M> factory) {

    return new MeterFactory<>(factory);
  }

  /**
   * Builds a meter by first obtaining a fresh {@link MeterBuilder} from the
   * {@link BuilderConstructor} and then invoking {@link MeterBuilder#build(Clock)}
   * on it with the supplied clock.
   *
   * @param clock the registry clock forwarded to the newly constructed builder
   * @return a fully initialised meter of type {@code M}
   */
  @Override
  public M build (Clock clock) {

    return factory.construct().build(clock);
  }
}
