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
package org.smallmind.phalanx.wire;

/**
 * Adapter that marshals between transported values and business objects.
 *
 * @param <V> transport representation type
 * @param <B> business object type
 */
public abstract class WireAdapter<V, B> {

  /**
   * Protected constructor to limit instantiation to subclasses.
   */
  protected WireAdapter () {

  }

  /**
   * Returns the transport value type this adapter handles.
   *
   * @return class of the serialized form
   */
  public abstract Class<V> getValueType ();

  /**
   * Converts the transported value into the business object.
   *
   * @param obj serialized value
   * @return unmarshalled business object
   * @throws Exception if conversion fails
   */
  public abstract B unmarshal (V obj)
    throws Exception;

  /**
   * Converts the business object into the transport value representation.
   *
   * @param obj business object to marshal
   * @return serialized value
   * @throws Exception if conversion fails
   */
  public abstract V marshal (B obj)
    throws Exception;
}
