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
 * A doubly linked node used internally by {@link IntrinsicRoster} to hold an element and its neighbors.
 * Element access and mutation are synchronized to allow safe concurrent use of the node itself,
 * while structural changes (link rewiring) are governed by the enclosing roster's lock.
 *
 * @param <T> the element type
 */
public class IntrinsicRosterNode<T> {

  private IntrinsicRosterNode<T> prev;
  private IntrinsicRosterNode<T> next;
  private T obj;

  /**
   * Creates a node containing the supplied element, linked to the given predecessor and successor.
   *
   * @param obj  the element to store
   * @param prev the previous node in the list, or {@code null} when this is the head
   * @param next the next node in the list, or {@code null} when this is the tail
   */
  public IntrinsicRosterNode (T obj, IntrinsicRosterNode<T> prev, IntrinsicRosterNode<T> next) {

    this.obj = obj;
    this.prev = prev;
    this.next = next;
  }

  /**
   * Returns the element stored in this node.
   *
   * @return the stored element
   */
  public synchronized T getObj () {

    return obj;
  }

  /**
   * Replaces the element stored in this node.
   *
   * @param obj the new element
   */
  public synchronized void setObj (T obj) {

    this.obj = obj;
  }

  /**
   * Tests whether the stored element equals the supplied object, treating identity as equality.
   *
   * @param something the object to compare against the stored element
   * @return {@code true} when the objects are the same instance or are equal by {@link Object#equals}
   */
  public synchronized boolean objEquals (Object something) {

    return (obj == something) || ((obj != null) && obj.equals(something));
  }

  /**
   * Returns the previous node in the list.
   *
   * @return the predecessor node, or {@code null} when this is the head
   */
  public IntrinsicRosterNode<T> getPrev () {

    return prev;
  }

  /**
   * Sets the previous node reference.
   *
   * @param prev the new predecessor node
   */
  public void setPrev (IntrinsicRosterNode<T> prev) {

    this.prev = prev;
  }

  /**
   * Returns the next node in the list.
   *
   * @return the successor node, or {@code null} when this is the tail
   */
  public IntrinsicRosterNode<T> getNext () {

    return next;
  }

  /**
   * Sets the next node reference.
   *
   * @param next the new successor node
   */
  public void setNext (IntrinsicRosterNode<T> next) {

    this.next = next;
  }
}
