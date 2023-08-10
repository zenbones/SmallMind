/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.bayeux.cometd.message;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class ListLike extends NodeBacked implements List<Object> {

  private final ArrayNode node;
  private final int startIndex;
  private int modification;
  private int endIndex;

  public ListLike (NodeBacked parent, ArrayNode node) {

    super(parent);

    this.node = node;

    startIndex = 0;
    endIndex = getNode().size();
  }

  public ListLike (ListLike listLike, int startIndex, int endIndex) {

    super(listLike.getParent());

    if ((startIndex < 0) || endIndex > listLike.size()) {
      throw new IndexOutOfBoundsException();
    } else {
      this.node = listLike.node;
      this.startIndex = startIndex;
      this.endIndex = endIndex;

      modification = listLike.modification;
    }
  }

  public ArrayNode getNode () {

    return node;
  }

  @Override
  public String writeAsString ()
    throws JsonProcessingException {

    if ((startIndex == 0) && (endIndex == node.size())) {

      return JsonCodec.writeAsString(node);
    } else {

      ArrayNode subNode = JsonNodeFactory.instance.arrayNode();

      for (int index = startIndex; index < endIndex; index++) {
        subNode.add(node.get(index));
      }

      return JsonCodec.writeAsString(subNode);
    }
  }

  public void modify () {

    modification += 1;
  }

  private int getModification () {

    return modification;
  }

  @Override
  public int size () {

    return endIndex - startIndex;
  }

  @Override
  public boolean isEmpty () {

    return endIndex == startIndex;
  }

  @Override
  public boolean add (Object obj) {

    getNode().insert(endIndex, in(obj));
    endIndex++;
    modify();
    mutate();

    return true;
  }

  @Override
  public boolean remove (Object obj) {

    int index;

    if ((index = indexOf(obj)) >= 0) {
      getNode().remove(startIndex + index);
      endIndex--;
      modify();
      mutate();

      return true;
    }

    return false;
  }

  @Override
  public Object get (int index) {

    if ((index < 0) || (index >= (endIndex - startIndex))) {
      throw new IndexOutOfBoundsException(index);
    } else {

      return out(this, getNode().get(startIndex + index));
    }
  }

  @Override
  public Object set (int index, Object element) {

    if ((index < 0) || (index >= (endIndex - startIndex))) {
      throw new IndexOutOfBoundsException(index);
    } else {
      modify();
      mutate();

      return getNode().set(startIndex + index, in(element));
    }
  }

  @Override
  public void add (int index, Object element) {

    if ((index < 0) || (index > (endIndex - startIndex))) {
      throw new IndexOutOfBoundsException(index);
    } else {
      endIndex++;
      modify();
      mutate();

      getNode().insert(startIndex + index, in(element));
    }
  }

  @Override
  public Object remove (int index) {

    if ((index < 0) || (index >= (endIndex - startIndex))) {
      throw new IndexOutOfBoundsException(index);
    } else {

      JsonNode removedNode;

      if ((removedNode = getNode().remove(startIndex + index)) != null) {
        endIndex--;
        modify();
        mutate();

        return out(null, removedNode);
      } else {

        return null;
      }
    }
  }

  @Override
  public int indexOf (Object value) {

    JsonNode convertedValue = in(value);

    for (int index = startIndex; index < endIndex; index++) {
      if (getNode().get(index).equals(convertedValue)) {

        return index - startIndex;
      }
    }

    return -1;
  }

  @Override
  public int lastIndexOf (Object value) {

    JsonNode convertedValue = in(value);

    for (int index = endIndex - 1; index >= startIndex; index--) {
      if (getNode().get(index).equals(convertedValue)) {

        return index - startIndex;
      }
    }

    return -1;
  }

  @Override
  public boolean contains (Object value) {

    return indexOf(value) >= 0;
  }

  @Override
  public boolean containsAll (Collection<?> c) {

    for (Object element : c) {
      if (!contains(element)) {

        return false;
      }
    }

    return true;
  }

  @Override
  public boolean addAll (Collection<?> c) {

    for (Object element : c) {
      getNode().insert(endIndex++, in(element));
    }

    if (!c.isEmpty()) {
      modify();
      mutate();

      return true;
    } else {

      return false;
    }
  }

  @Override
  public boolean addAll (int index, Collection<?> c) {

    if ((index < 0) || (index > (endIndex - startIndex))) {
      throw new IndexOutOfBoundsException(index);
    } else {

      int currentIndex = index;

      for (Object element : c) {
        getNode().insert(startIndex + currentIndex, in(element));
        endIndex++;
        currentIndex++;
      }

      if (!c.isEmpty()) {
        modify();
        mutate();

        return true;
      } else {

        return false;
      }
    }
  }

  @Override
  public boolean removeAll (Collection<?> c) {

    boolean changed = false;

    for (Object element : c) {

      int index;

      if ((index = indexOf(element)) >= 0) {
        getNode().remove(startIndex + index);
        endIndex--;
        changed = true;
      }
    }

    if (changed) {
      modify();
      mutate();

      return true;
    } else {

      return false;
    }
  }

  @Override
  public boolean retainAll (Collection<?> c) {

    LinkedList<Integer> removalList = new LinkedList<>();
    HashSet<JsonNode> matchingSet = new HashSet<>();

    for (Object element : c) {
      matchingSet.add(in(element));
    }

    for (int index = endIndex - 1; index >= startIndex; index--) {
      if (!matchingSet.contains(getNode().get(index))) {
        removalList.add(index);
      }
    }

    if (!removalList.isEmpty()) {
      for (int index : removalList) {
        getNode().remove(index);
        endIndex--;
      }

      modify();
      mutate();

      return true;
    } else {

      return false;
    }
  }

  @Override
  public void clear () {

    boolean changed = false;

    while (endIndex > startIndex) {
      getNode().remove(startIndex);
      endIndex--;
      changed = true;
    }

    if (changed) {
      modify();
      mutate();
    }
  }

  @Override
  public Object[] toArray () {

    Object[] array = new Object[endIndex - startIndex];

    for (int index = startIndex; index < endIndex; index++) {
      array[index - startIndex] = out(this, getNode().get(index));
    }

    return array;
  }

  @Override
  public <T> T[] toArray (T[] a) {

    return (T[])toArray();
  }

  @Override
  public Iterator<Object> iterator () {

    return new ListLikeListIterator(this);
  }

  @Override
  public ListIterator<Object> listIterator () {

    return new ListLikeListIterator(this);
  }

  @Override
  public ListIterator<Object> listIterator (int index) {

    return new ListLikeListIterator(this, index);
  }

  @Override
  public List<Object> subList (int fromIndex, int toIndex) {

    return new ListLike(this, fromIndex, toIndex);
  }

  private static class ListLikeListIterator implements ListIterator<Object> {

    private final ListLike listLike;
    private int index;
    private int expectedModCount;

    public ListLikeListIterator (ListLike listLike) {

      this(listLike, 0);
    }

    public ListLikeListIterator (ListLike listLike, int index) {

      if (listLike == null) {
        throw new NullPointerException();
      } else if ((index < 0) || (index > listLike.size())) {
        throw new IndexOutOfBoundsException(index);
      } else {

        this.listLike = listLike;
        this.index = index;

        expectedModCount = listLike.getModification();
      }
    }

    @Override
    public boolean hasNext () {

      return index < listLike.size();
    }

    @Override
    public Object next () {

      if (expectedModCount != listLike.getModification()) {
        throw new ConcurrentModificationException();
      } else if (index >= listLike.size()) {
        throw new NoSuchElementException();
      } else {

        return listLike.get(index++);
      }
    }

    @Override
    public boolean hasPrevious () {

      return index > 0;
    }

    @Override
    public Object previous () {

      if (expectedModCount != listLike.getModification()) {
        throw new ConcurrentModificationException();
      } else if (index <= 0) {
        throw new NoSuchElementException();
      } else {

        return listLike.get(--index);
      }
    }

    @Override
    public int nextIndex () {

      return index;
    }

    @Override
    public int previousIndex () {

      return (index > 0) ? index - 1 : 0;
    }

    @Override
    public void remove () {

      if (expectedModCount != listLike.getModification()) {
        throw new ConcurrentModificationException();
      } else {
        listLike.remove(index);
        expectedModCount = listLike.getModification();
      }
    }

    @Override
    public void set (Object obj) {

      if (expectedModCount != listLike.getModification()) {
        throw new ConcurrentModificationException();
      } else {
        listLike.set(index, obj);
        expectedModCount = listLike.getModification();
      }
    }

    @Override
    public void add (Object obj) {

      if (expectedModCount != listLike.getModification()) {
        throw new ConcurrentModificationException();
      } else {
        listLike.add(obj);
        expectedModCount = listLike.getModification();
      }
    }
  }
}
