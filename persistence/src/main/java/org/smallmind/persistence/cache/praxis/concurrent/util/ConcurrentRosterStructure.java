/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.praxis.concurrent.util;

import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ConcurrentRosterStructure<T> {

  private ConcurrentRosterStructure<T> parent;
  private ConcurrentRosterNode<T> head;
  private ConcurrentRosterNode<T> tail;
  int size;

  public ConcurrentRosterStructure () {

    size = 0;
  }

  public ConcurrentRosterStructure (ConcurrentRosterStructure<T> parent, ConcurrentRosterNode<T> head, ConcurrentRosterNode<T> tail, int size) {

    this.parent = parent;
    this.head = head;
    this.tail = tail;
    this.size = size;
  }

  public ConcurrentRosterNode<T> getHead () {

    return head;
  }

  public void setHead (ConcurrentRosterNode<T> head) {

    if ((parent != null) && parent.isHead(this.head)) {
      parent.setHead(head);
    }

    this.head = head;
  }

  public boolean isHead (ConcurrentRosterNode<T> node) {

    return (head != null) && (node == head);
  }

  public ConcurrentRosterNode<T> getTail () {

    return tail;
  }

  public void setTail (ConcurrentRosterNode<T> tail) {

    if ((parent != null) && parent.isTail(this.tail)) {
      parent.setTail(tail);
    }

    this.tail = tail;
  }

  public boolean isTail (ConcurrentRosterNode<T> node) {

    return (tail != null) && (node == tail);
  }

  public void evaporate (ConcurrentRosterNode<T> prev, ConcurrentRosterNode<T> current, ConcurrentRosterNode<T> next) {

    if (parent != null) {
      evaporate(prev, current, next);
    }

    if (size == 0) {
      head = prev;
      tail = next;
    }
    else if (head == current) {
      head = next;
    }
    else if (tail == current) {
      tail = prev;
    }
  }

  public void ouroboros (T element) {

    ConcurrentRosterNode<T> added = new ConcurrentRosterNode<T>(element, head, tail);

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

  public void reconstitute (ConcurrentRosterNode<T> added, ConcurrentRosterNode<T> head, ConcurrentRosterNode<T> tail) {

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

  public int getSize () {

    return size;
  }

  public void addSize (int delta) {

    if (parent != null) {
      parent.addSize(delta);
    }

    size += delta;
  }

  public void subtractSize (int delta) {

    if (parent != null) {
      parent.subtractSize(delta);
    }

    size -= delta;
  }

  public void incSize () {

    if (parent != null) {
      parent.incSize();
    }

    size++;
  }

  public void decSize () {

    if (parent != null) {
      parent.decSize();
    }

    size--;
  }
}
