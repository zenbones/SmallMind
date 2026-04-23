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
package org.smallmind.claxon.registry.aop;

import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;

/**
 * Strategy interface that converts a JSON configuration string into a
 * {@link MeterBuilder} for use by the {@link InstrumentedAspect}.
 *
 * <p>Implementations are instantiated by the aspect via their public no-argument constructor
 * on each intercepted invocation, so they must be stateless (or safe for repeated single-use
 * construction). The {@link #parse(String)} method receives the literal value of
 * {@link Instrumented#json()} and is responsible for deserialising it into whatever builder
 * parameters the target meter type requires.</p>
 *
 * <p>This interface is marked {@link FunctionalInterface} and can therefore be implemented as
 * a lambda or method reference where appropriate.</p>
 *
 * @param <M> the concrete {@link Meter} type produced by the builder returned from
 *            {@link #parse(String)}
 */
@FunctionalInterface
public interface InstrumentedParser<M extends Meter> {

  /**
   * Parses {@code json} and returns a fully configured {@link MeterBuilder} ready to construct
   * a meter of type {@code M}.
   *
   * @param json the JSON string from {@link Instrumented#json()}; may be {@code "{}"} for
   *             parsers that require no configuration
   * @return a non-{@code null} {@link MeterBuilder} configured according to {@code json}
   * @throws Exception if {@code json} cannot be parsed or is missing required fields;
   *                   the aspect will propagate this as an unchecked {@link InstrumentationException}
   */
  MeterBuilder<M> parse (String json)
    throws Exception;
}
