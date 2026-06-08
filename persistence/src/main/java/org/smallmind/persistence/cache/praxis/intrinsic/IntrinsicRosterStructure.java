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
 * Mutable metadata record that tracks the head node, tail node, and element count for an
 * {@link IntrinsicRoster} or one of its sublist views. Child structures optionally chain to a
 * parent so that boundary and size changes propagate from sublist to parent roster.
 *
 * @param <T> the element type stored in the roster
 */
public class IntrinsicRosterStructure<T> {

  private IntrinsicRosterStructure<T> parent;
  private IntrinsicRosterNode<T> head;
  private IntrinsicRosterNode<T> tail;
  int size;

  /**
   * Creates an empty root structure with no parent.
   */
  public IntrinsicRosterStructure () {

    size = 0;
  }

  /**
   * Creates a structure with explicit state, optionally linked to a parent for change propagation.
   *
   * @param parent the parent structure to notify when boundaries or size change; may be {@code null}
   * @param head   the head node of this view
   * @param tail   the tail node of this view
   * @param size   the current number of elements in this view
   */
  public IntrinsicRosterStructure (IntrinsicRosterStructure<T> parent, IntrinsicRosterNode<T> head, IntrinsicRosterNode<T> tail, int size) {

    this.parent = parent;
    this.head = head;
    this.tail = tail;
    this.size = size;
  }

  /**
   * Returns the head node of this roster view.
   *
   * @return the head node, or {@code null} when the roster is empty
   */
  public IntrinsicRosterNode<T> getHead () {

    return head;
  }

  /**
   * Sets the head node, propagating the change to the parent when the old head was also the parent's head.
   *
   * @param head the new head node
   */
  public void setHead (IntrinsicRosterNode<T> head) {

    if ((parent != null) && parent.isHead(this.head)) {
      parent.setHead(head);
    }

    this.head = head;
  }

  /**
   * Returns {@code true} when the given node is currently the head of this view.
   *
   * @param node the node to test
   * @return {@code true} when {@code node} is the head
   */
  public boolean isHead (IntrinsicRosterNode<T> node) {

    return (head != null) && (node == head);
  }

  /**
   * Returns the tail node of this roster view.
   *
   * @return the tail node, or {@code null} when the roster is empty
   */
  public IntrinsicRosterNode<T> getTail () {

    return tail;
  }

  /**
   * Sets the tail node, propagating the change to the parent when the old tail was also the parent's tail.
   *
   * @param tail the new tail node
   */
  public void setTail (IntrinsicRosterNode<T> tail) {

    if ((parent != null) && parent.isTail(this.tail)) {
      parent.setTail(tail);
    }

    this.tail = tail;
  }

  /**
   * Returns {@code true} when the given node is currently the tail of this view.
   *
   * @param node the node to test
   * @return {@code true} when {@code node} is the tail
   */
  public boolean isTail (IntrinsicRosterNode<T> node) {

    return (tail != null) && (node == tail);
  }

  /**
   * Adjusts head and tail boundaries after a node is removed, propagating to the parent as needed.
   * When the size drops to zero both boundaries are set to the neighboring nodes. Otherwise, only the
   * boundary that pointed to the removed node is updated.
   *
   * @param prev    the predecessor of the removed node
   * @param current the node being removed
   * @param next    the successor of the removed node
   */
  public void evaporate (IntrinsicRosterNode<T> prev, IntrinsicRosterNode<T> current, IntrinsicRosterNode<T> next) {

    if (parent != null) {
      parent.evaporate(prev, current, next);
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
   * Initializes this structure with a single self-referencing node, used when inserting into an empty roster.
   * Changes are propagated to the parent via {@link #reconstitute} when a parent exists.
   *
   * @param element the element for the new node
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
   * Updates head and tail references after a new node is inserted, cascading to the parent when present.
   * A {@code null} head means this structure had no head before insertion; the same applies to the tail.
   *
   * @param added the newly inserted node
   * @param head  the head value before insertion (used to detect an empty-before-insertion state)
   * @param tail  the tail value before insertion (used to detect an empty-before-insertion state)
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
   * Removes all elements from this view by severing the span of nodes it owns, then propagates
   * the size reduction to the parent.
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
   * Returns the current element count for this view.
   *
   * @return the size
   */
  public int getSize () {

    return size;
  }

  /**
   * Increases the element count by {@code delta} and propagates the change to the parent.
   *
   * @param delta the number of elements added
   */
  public void addSize (int delta) {

    if (parent != null) {
      parent.addSize(delta);
    }

    size += delta;
  }

  /**
   * Decreases the element count by {@code delta} and propagates the change to the parent.
   *
   * @param delta the number of elements removed
   */
  public void subtractSize (int delta) {

    if (parent != null) {
      parent.subtractSize(delta);
    }

    size -= delta;
  }

  /**
   * Increments the element count by one and propagates the change to the parent.
   */
  public void incSize () {

    if (parent != null) {
      parent.incSize();
    }

    size++;
  }

  /**
   * Decrements the element count by one and propagates the change to the parent.
   */
  public void decSize () {

    if (parent != null) {
      parent.decSize();
    }

    size--;
  }
}
