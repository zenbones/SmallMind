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

/**
 * Functional interface that acts as a supplier of {@link MeterBuilder} instances.
 *
 * <p>Implementations are used by {@link MeterFactory} to defer construction of a
 * {@link MeterBuilder} until it is actually needed, allowing builder state to be
 * freshly initialized for each meter that is created.</p>
 *
 * @param <M> the concrete {@link Meter} type that the produced builder will construct
 */
@FunctionalInterface
public interface BuilderConstructor<M extends Meter> {

  /**
   * Constructs and returns a new {@link MeterBuilder} ready to build a meter of type {@code M}.
   *
   * @return a freshly constructed {@link MeterBuilder} instance
   */
  MeterBuilder<M> construct ();
}
