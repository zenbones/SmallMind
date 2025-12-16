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
 * Mutable structure that tracks the head, tail, and size metadata for {@link IntrinsicRoster} instances.
 * Structures may chain to a parent to keep sublists synchronized with the original roster.
 *
 * @param <T> element type stored in the roster
 */
public class IntrinsicRosterStructure<T> {

  private IntrinsicRosterStructure<T> parent;
  private IntrinsicRosterNode<T> head;
  private IntrinsicRosterNode<T> tail;
  int size;

  /**
   * Creates an empty roster structure with no parent.
   */
  public IntrinsicRosterStructure () {

    size = 0;
  }

  /**
   * Creates a roster structure with explicitly supplied state and optional parent linkage.
   *
   * @param parent parent structure to keep synchronized
   * @param head   head node of the roster
   * @param tail   tail node of the roster
   * @param size   current element count
   */
  public IntrinsicRosterStructure (IntrinsicRosterStructure<T> parent, IntrinsicRosterNode<T> head, IntrinsicRosterNode<T> tail, int size) {

    this.parent = parent;
    this.head = head;
    this.tail = tail;
    this.size = size;
  }

  /**
   * @return head node of the roster
   */
  public IntrinsicRosterNode<T> getHead () {

    return head;
  }

  /**
   * Updates the head node, propagating to the parent when necessary.
   *
   * @param head new head node
   */
  public void setHead (IntrinsicRosterNode<T> head) {

    if ((parent != null) && parent.isHead(this.head)) {
      parent.setHead(head);
    }

    this.head = head;
  }

  /**
   * Tests whether the supplied node is the current head.
   *
   * @param node node to test
   * @return {@code true} when the node is the head
   */
  public boolean isHead (IntrinsicRosterNode<T> node) {

    return (head != null) && (node == head);
  }

  /**
   * @return tail node of the roster
   */
  public IntrinsicRosterNode<T> getTail () {

    return tail;
  }

  /**
   * Updates the tail node, propagating to the parent when necessary.
   *
   * @param tail new tail node
   */
  public void setTail (IntrinsicRosterNode<T> tail) {

    if ((parent != null) && parent.isTail(this.tail)) {
      parent.setTail(tail);
    }

    this.tail = tail;
  }

  /**
   * Tests whether the supplied node is the current tail.
   *
   * @param node node to test
   * @return {@code true} when the node is the tail
   */
  public boolean isTail (IntrinsicRosterNode<T> node) {

    return (tail != null) && (node == tail);
  }

  /**
   * Removes a node from the structure, adjusting head and tail references.
   *
   * @param prev    previous node
   * @param current node being removed
   * @param next    next node
   */
  public void evaporate (IntrinsicRosterNode<T> prev, IntrinsicRosterNode<T> current, IntrinsicRosterNode<T> next) {

    if (parent != null) {
      evaporate(prev, current, next);
    }

    if (size == 0) {
      head = prev;
      tail = next;
    } else if (head == current) {
      head = next;
    } else if (tail == current) {
      tail = prev;
    }
  }

  /**
   * Initializes the structure as a circular list containing a single element.
   *
   * @param element element to insert
   */
  public void ouroboros (T element) {

    IntrinsicRosterNode<T> added = new IntrinsicRosterNode<>(element, head, tail);

    if (head != null) {
      head.setNext(added);
    }
    if (tail != null) {
      tail.setPrev(added);
    }

    if (parent != null) {
      parent.reconstitute(added, head, tail);
    }

    head = tail = added;
    size = 1;
  }

  /**
   * Reconstitutes head and tail links when inserting into an existing structure, cascading to the parent if present.
   *
   * @param added newly added node
   * @param head  head prior to insertion
   * @param tail  tail prior to insertion
   */
  public void reconstitute (IntrinsicRosterNode<T> added, IntrinsicRosterNode<T> head, IntrinsicRosterNode<T> tail) {

    if (parent != null) {
      parent.reconstitute(added, head, tail);
    }

    if (head == null) {
      this.head = added;
    }
    if (tail == null) {
      this.tail = added;
    }

    size++;
  }

  /**
   * Clears the roster, severing links and resetting sizes in this structure and its parent.
   */
  public void clear () {

    if (size > 0) {
      head = head.getPrev();
      tail = tail.getNext();

      if (head != null) {
        head.setNext(tail);
      }
      if (tail != null) {
        tail.setPrev(head);
      }

      subtractSize(getSize());
    }
  }

  /**
   * @return current roster size
   */
  public int getSize () {

    return size;
  }

  /**
   * Adds to the tracked size and propagates the change to the parent.
   *
   * @param delta number of elements added
   */
  public void addSize (int delta) {

    if (parent != null) {
      parent.addSize(delta);
    }

    size += delta;
  }

  /**
   * Subtracts from the tracked size and propagates the change to the parent.
   *
   * @param delta number of elements removed
   */
  public void subtractSize (int delta) {

    if (parent != null) {
      parent.subtractSize(delta);
    }

    size -= delta;
  }

  /**
   * Increments the size and propagates the change to the parent.
   */
  public void incSize () {

    if (parent != null) {
      parent.incSize();
    }

    size++;
  }

  /**
   * Decrements the size and propagates the change to the parent.
   */
  public void decSize () {

    if (parent != null) {
      parent.decSize();
    }

    size--;
  }
}
