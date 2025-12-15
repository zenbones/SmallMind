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
package org.smallmind.nutsnbolts.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * Multiset-like collection supporting multiplicity for elements.
 *
 * @param <E> element type
 */
public interface Bag<E> extends Collection<E> {

  /**
   * Adds an element with the given multiplicity.
   *
   * @param e        element to add
   * @param multiple number of occurrences to add
   * @return {@code true} if the bag changed
   */
  boolean add (E e, int multiple);

  /**
   * Removes up to the specified multiplicity of the element.
   *
   * @param e        element to remove
   * @param multiple number of occurrences to remove
   * @return {@code true} if the bag changed
   */
  boolean remove (E e, int multiple);

  /**
   * @return set of distinct elements contained in the bag
   */
  Set<E> keySet ();

  /**
   * @return set of entries mapping element to multiplicity
   */
  Set<Map.Entry<E, Integer>> entrySet ();

  /**
   * Creates a spliterator reporting the {@link Spliterator#DISTINCT} characteristic over the bag's elements.
   *
   * @return spliterator traversing unique elements
   */
  @Override
  default Spliterator<E> spliterator () {

    return Spliterators.spliterator(this, Spliterator.DISTINCT);
  }
}
