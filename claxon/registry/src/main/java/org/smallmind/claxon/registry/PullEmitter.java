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
package org.smallmind.claxon.registry;

/**
 * Abstract base class for {@link Emitter} implementations that operate in a pull-based
 * model. Rather than receiving pushed measurements from the registry, a pull emitter
 * exposes an {@link #emit()} method that is called by an external consumer (such as a
 * monitoring endpoint or scrape target) to retrieve the current metric payload on demand.
 *
 * @param <T> the type of the payload produced when {@link #emit()} is invoked
 */
public abstract class PullEmitter<T> implements Emitter {

  /**
   * Identifies this emitter as operating in the pull model, always returning
   * {@link EmitterMethod#PULL}.
   *
   * @return {@link EmitterMethod#PULL}
   */
  @Override
  public EmitterMethod getEmitterMethod () {

    return EmitterMethod.PULL;
  }

  /**
   * Produces the current metric payload when the emitter is polled by an external consumer.
   * Implementations should collect and format the relevant metric data and return it as the
   * parameterized type {@code T}.
   *
   * @return the current metric payload; the exact type and structure are determined by the
   * concrete implementation
   */
  public abstract T emit ();
}
