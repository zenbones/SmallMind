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
package org.smallmind.persistence;

import java.io.Serializable;

/**
 * Exposes metadata and id conversion helpers for DAOs that manage {@link Durable} entities.
 *
 * @param <I> identifier type
 * @param <D> durable type
 */
public interface ManagedDao<I extends Serializable & Comparable<I>, D extends Durable<I>> {

  /**
   * Provides a name that identifies the source of metrics emitted by this DAO.
   *
   * @return the metric source name
   */
  String getMetricSource ();

  /**
   * Returns the durable class managed by this DAO.
   *
   * @return the durable type
   */
  Class<D> getManagedClass ();

  /**
   * Returns the identifier class used by the managed durable.
   *
   * @return the identifier type
   */
  Class<I> getIdClass ();

  /**
   * Parses the supplied string into an identifier instance appropriate for this DAO.
   *
   * @param value the string representation of the id
   * @return the parsed identifier value
   */
  I getIdFromString (String value);

  /**
   * Extracts the identifier from a durable instance.
   *
   * @param durable the durable to inspect
   * @return the identifier value, possibly {@code null} for transient objects
   */
  I getId (D durable);
}
