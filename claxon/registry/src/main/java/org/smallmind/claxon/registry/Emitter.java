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
 * Receives collected meter readings and forwards them to an external monitoring system.
 *
 * <p>Each emitter bound to a {@link ClaxonRegistry} is invoked once per collection interval
 * for every meter that produced non-empty readings. Implementations are responsible for
 * serialising and transmitting the data in whatever format the target system requires.
 *
 * @see ClaxonRegistry#bind(String, Emitter)
 * @see EmitterMethod
 */
public interface Emitter {

  /**
   * Returns the data-flow direction supported by this emitter, indicating whether
   * the registry pushes readings to it or whether it pulls readings on demand.
   *
   * @return the {@link EmitterMethod} describing this emitter's data-flow direction
   */
  EmitterMethod getEmitterMethod ();

  /**
   * Receives and transmits a set of {@link Quantity} readings for the named meter.
   *
   * <p>This method is called by the registry's collection thread. Implementations should
   * handle transmission failures gracefully; unchecked exceptions will be caught and
   * logged by the registry, but checked exceptions declared here are also permitted.
   *
   * @param meterName  logical name of the meter whose readings are being emitted
   * @param tags       combined tag array for this emission, or {@code null} when no tags are configured
   * @param quantities the non-empty array of {@link Quantity} values recorded by the meter
   * @throws Exception if the emitter cannot transmit the data to the target system
   */
  void record (String meterName, Tag[] tags, Quantity[] quantities)
    throws Exception;
}
