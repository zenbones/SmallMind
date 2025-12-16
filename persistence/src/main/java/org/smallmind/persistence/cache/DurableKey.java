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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import org.smallmind.persistence.Durable;

/**
 * Cache key wrapper that combines a durable class name with its identifier to form a unique string
 * key.
 */
public class DurableKey<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Serializable {

  private final Class<D> durableClass;
  private final String key;

  /**
   * Builds a key string from the durable's simple class name and identifier.
   *
   * @param durableClass durable type
   * @param id           durable identifier
   */
  public DurableKey (Class<D> durableClass, I id) {

    this.durableClass = durableClass;

    key = durableClass.getSimpleName() + "=" + id;
  }

  /**
   * @return durable class this key refers to
   */
  public Class<D> getDurableClass () {

    return durableClass;
  }

  /**
   * @return cacheable key string combining class and id
   */
  public String getKey () {

    return key;
  }

  /**
   * @return id portion of the key as a string
   */
  public String getIdAsString () {

    return key.substring(key.indexOf('=') + 1);
  }

  /**
   * Returns the cache key string.
   *
   * @return string representation of this durable key
   */
  @Override
  public String toString () {

    return key;
  }

  /**
   * Hashes based on the generated key string.
   *
   * @return hash code of the key
   */
  @Override
  public int hashCode () {

    return key.hashCode();
  }

  /**
   * Compares by key string to determine equality.
   *
   * @param obj object to compare
   * @return {@code true} when the other object is a {@link DurableKey} with the same key
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof DurableKey) && key.equals(((DurableKey<?, ?>)obj).getKey());
  }
}
