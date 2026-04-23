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
 * Describes the data-flow direction used by an {@link Emitter} to obtain metric readings
 * from the registry.
 *
 * <p>The registry's collection thread calls {@link Emitter#record} on every bound emitter
 * regardless of this value; the constant is provided so that higher-level tooling or
 * adapter code can make routing decisions based on the emitter's preferred model.
 */
public enum EmitterMethod {

  /**
   * The registry actively pushes readings to the emitter on each collection interval.
   * Suitable for fire-and-forget transports such as UDP-based StatsD or HTTP ingest APIs.
   */
  PUSH,

  /**
   * The emitter exposes readings for an external system to retrieve on demand.
   * Suitable for scrape-based systems such as Prometheus.
   */
  PULL
}
