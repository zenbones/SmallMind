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
package org.smallmind.persistence.cache.praxis.intrinsic;

/**
 * Doubly linked node used by {@link IntrinsicRoster} to store elements and their neighbors.
 *
 * @param <T> element type
 */
public class IntrinsicRosterNode<T> {

  private IntrinsicRosterNode<T> prev;
  private IntrinsicRosterNode<T> next;
  private T obj;

  /**
   * Creates a node containing the supplied element with links to adjacent nodes.
   *
   * @param obj  stored element
   * @param prev previous node
   * @param next next node
   */
  public IntrinsicRosterNode (T obj, IntrinsicRosterNode<T> prev, IntrinsicRosterNode<T> next) {

    this.obj = obj;
    this.prev = prev;
    this.next = next;
  }

  /**
   * @return stored element
   */
  public synchronized T getObj () {

    return obj;
  }

  /**
   * Updates the stored element.
   *
   * @param obj new element
   */
  public synchronized void setObj (T obj) {

    this.obj = obj;
  }

  /**
   * Compares the stored element with the supplied object.
   *
   * @param something object to compare
   * @return {@code true} when the element matches
   */
  public synchronized boolean objEquals (Object something) {

    return (obj == something) || ((obj != null) && obj.equals(something));
  }

  /**
   * @return previous node in the list
   */
  public IntrinsicRosterNode<T> getPrev () {

    return prev;
  }

  /**
   * Sets the previous node reference.
   *
   * @param prev previous node
   */
  public void setPrev (IntrinsicRosterNode<T> prev) {

    this.prev = prev;
  }

  /**
   * @return next node in the list
   */
  public IntrinsicRosterNode<T> getNext () {

    return next;
  }

  /**
   * Sets the next node reference.
   *
   * @param next next node
   */
  public void setNext (IntrinsicRosterNode<T> next) {

    this.next = next;
  }
}
