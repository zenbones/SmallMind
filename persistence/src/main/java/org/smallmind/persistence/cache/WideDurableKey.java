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
 * Cache key for wide vectors combining context, parent durable type/id, and child durable type.
 */
public class WideDurableKey<W extends Serializable & Comparable<W>, D extends Durable<?>> implements Serializable {

  private final Class<D> durableClass;
  private final String key;

  /**
   * Constructs a composite key tying a parent durable to its child durable type within a context.
   *
   * @param context      optional context string
   * @param parentClass  parent durable class
   * @param parentId     parent identifier
   * @param durableClass child durable class
   */
  public WideDurableKey (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass) {

    this.durableClass = durableClass;

    key = context + '.' + (parentClass.getSimpleName()) + '[' + durableClass.getSimpleName() + ']' + '=' + parentId;
  }

  /**
   * @return child durable class
   */
  public Class<D> getDurableClass () {

    return durableClass;
  }

  /**
   * @return composite cache key string
   */
  public String getKey () {

    return key;
  }

  /**
   * @return parent id portion of the key as string
   */
  public String getParentIdAsString () {

    return key.substring(key.indexOf('=') + 1);
  }

  /**
   * Returns the composed key string.
   *
   * @return string representation of this wide durable key
   */
  public String toString () {

    return key;
  }

  /**
   * Hashes based on the composite key string.
   *
   * @return hash code for this key
   */
  public int hashCode () {

    return key.hashCode();
  }

  /**
   * Keys are equal when their composite key strings match.
   *
   * @param obj object to compare
   * @return {@code true} when keys match
   */
  public boolean equals (Object obj) {

    return (obj instanceof WideDurableKey) && key.equals(((WideDurableKey)obj).getKey());
  }
}
