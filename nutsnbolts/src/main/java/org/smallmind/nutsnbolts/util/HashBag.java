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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link HashMap}-backed {@link Bag} implementation that tracks the multiplicity of each distinct element.
 *
 * @param <T> the element type
 */
public class HashBag<T> implements Bag<T> {

  private final HashMap<T, Integer> internalMap;
  private int size;

  /**
   * Constructs an empty bag.
   */
  public HashBag () {

    internalMap = new HashMap<>();
    size = 0;
  }

  /**
   * Constructs a bag populated with each element of the given collection, counting each occurrence once.
   *
   * @param c the initial elements to add
   */
  public HashBag (Collection<? extends T> c) {

    this();

    addAll(c);
  }

  /**
   * Constructs a bag by copying all elements and their multiplicities from another bag.
   *
   * @param b the source bag whose elements and counts are copied
   */
  public HashBag (Bag<? extends T> b) {

    internalMap = new HashMap<>();
    size = b.size();

    for (Map.Entry<? extends T, Integer> entry : b.entrySet()) {
      internalMap.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns the total number of element occurrences in this bag, counting multiplicities.
   *
   * @return the total occurrence count
   */
  @Override
  public int size () {

    return size;
  }

  /**
   * Returns {@code true} if this bag contains no elements.
   *
   * @return {@code true} if the bag is empty
   */
  @Override
  public boolean isEmpty () {

    return size == 0;
  }

  /**
   * Returns the number of occurrences of the given element in this bag.
   *
   * @param t the element to query
   * @return the occurrence count, or {@code null} if the element is not present
   */
  public Integer get (T t) {

    return internalMap.get(t);
  }

  /**
   * Returns {@code true} if this bag contains at least one occurrence of the given object.
   *
   * @param obj the object to test for presence
   * @return {@code true} if the object is present
   */
  @Override
  public boolean contains (Object obj) {

    return internalMap.containsKey(obj);
  }

  /**
   * Returns {@code true} if this bag contains at least one occurrence of every element in the given collection.
   *
   * @param c the collection of elements to test
   * @return {@code true} if all elements are present
   */
  @Override
  public boolean containsAll (Collection<?> c) {

    return (containsAll(new HashBag<>(c)));
  }

  /**
   * Returns {@code true} if this bag contains at least as many occurrences of each element as the given bag.
   *
   * @param b the bag of elements and multiplicities to test
   * @return {@code true} if this bag satisfies all multiplicity requirements of the given bag
   */
  public boolean containsAll (Bag<?> b) {

    for (Map.Entry<?, Integer> containedEntry : b.entrySet()) {

      Integer count;

      if (((count = internalMap.get(containedEntry.getKey())) == null) || (containedEntry.getValue() > count)) {

        return false;
      }
    }

    return true;
  }

  /**
   * Adds one occurrence of the given element to this bag.
   *
   * @param t the element to add
   * @return {@code true} always, as the bag always changes when an element is added
   */
  @Override
  public boolean add (T t) {

    return add(t, 1);
  }

  /**
   * Adds the specified number of occurrences of the given element to this bag.
   *
   * @param t        the element to add
   * @param multiple the number of occurrences to add; must be at least 1
   * @return {@code true} always, as the bag always changes when occurrences are added
   * @throws IllegalStateException if {@code multiple} is less than 1
   */
  @Override
  public boolean add (T t, int multiple) {

    if (multiple < 1) {
      throw new IllegalStateException(multiple + " < 0");
    } else {

      Integer count;

      if ((count = internalMap.get(t)) == null) {
        internalMap.put(t, multiple);
      } else {
        internalMap.put(t, count + multiple);
      }

      size += multiple;

      return true;
    }
  }

  /**
   * Removes one occurrence of the given element from this bag.
   *
   * @param obj the element to remove
   * @return {@code true} if the bag contained at least one occurrence of the element
   */
  @Override
  public boolean remove (Object obj) {

    return remove((T)obj, 1);
  }

  /**
   * Removes up to the specified number of occurrences of the given element from this bag.
   *
   * @param t        the element to remove
   * @param multiple the number of occurrences to remove; must be at least 1
   * @return {@code true} if the bag changed as a result of the call
   * @throws IllegalStateException if {@code multiple} is less than 1
   */
  @Override
  public boolean remove (T t, int multiple) {

    if (multiple < 1) {
      throw new IllegalStateException(multiple + " < 0");
    } else {

      Integer count;

      if ((count = internalMap.get(t)) != null) {
        if (multiple >= count) {
          internalMap.remove(t);
        } else {
          internalMap.put(t, count - multiple);
        }

        size -= Math.min(count, multiple);

        return true;
      }

      return false;
    }
  }

  /**
   * Adds one occurrence of each element in the given collection to this bag.
   *
   * @param c the collection of elements to add
   * @return {@code true} if the bag changed as a result of the call
   */
  @Override
  public boolean addAll (Collection<? extends T> c) {

    if (!c.isEmpty()) {
      for (T t : c) {
        add(t);
      }

      return true;
    }

    return false;
  }

  /**
   * Removes one occurrence of each element present in the given collection from this bag.
   *
   * @param c the collection of elements to remove
   * @return {@code true} if the bag changed as a result of the call
   */
  @Override
  public boolean removeAll (Collection<?> c) {

    boolean changed = false;

    for (Object obj : c) {
      if (remove(obj)) {
        changed = true;
      }
    }

    return changed;
  }

  /**
   * Retains only the elements present in the given collection, reducing multiplicities to match those of the collection.
   *
   * @param c the collection of elements to retain
   * @return {@code true} if the bag changed as a result of the call
   */
  @Override
  public boolean retainAll (Collection<?> c) {

    HashBag retainedBag = new HashBag<>(c);
    boolean changed = false;

    for (Map.Entry<T, Integer> internalEntry : internalMap.entrySet()) {

      Integer retainedCount;

      if ((retainedCount = retainedBag.get(internalEntry.getKey())) == null) {
        internalMap.remove(internalEntry.getKey());
        changed = true;
      } else {

        int delta;

        if ((delta = internalEntry.getValue() - retainedCount) > 0) {
          internalMap.put(internalEntry.getKey(), retainedCount);
          size -= delta;
          changed = true;
        }
      }
    }

    return changed;
  }

  /**
   * Removes all elements and resets the bag to an empty state.
   */
  @Override
  public void clear () {

    internalMap.clear();
    size = 0;
  }

  /**
   * Returns the set of distinct elements currently in this bag.
   *
   * @return the set of unique element keys
   */
  @Override
  public Set<T> keySet () {

    return internalMap.keySet();
  }

  /**
   * Returns the set of entries mapping each distinct element to its occurrence count.
   *
   * @return the element-to-count entry set
   */
  public Set<Map.Entry<T, Integer>> entrySet () {

    return internalMap.entrySet();
  }

  /**
   * Returns an iterator over all element occurrences in this bag, yielding each element once per its multiplicity.
   *
   * @return an iterator over all occurrences
   */
  @Override
  public Iterator<T> iterator () {

    return new BagIterator();
  }

  /**
   * Returns an array containing all element occurrences in this bag, with each element repeated its multiplicity number of times.
   *
   * @return an array of all occurrences
   */
  @Override
  public T[] toArray () {

    return (T[])toArray(new Object[size]);
  }

  /**
   * Fills the given array (or a new one of the same type) with all element occurrences in this bag.
   *
   * @param a    the array to fill, or a template for a new array if it is not large enough
   * @param <T1> the component type of the array
   * @return the array containing all occurrences
   */
  @Override
  public <T1> T1[] toArray (T1[] a) {

    Iterator<T> iterator = new BagIterator();
    int index = 0;

    while (iterator.hasNext()) {
      a[index++] = (T1)iterator.next();
    }

    return a;
  }

  /**
   * Returns a hash code derived from the internal map of element-to-count pairs.
   *
   * @return the hash code
   */
  @Override
  public int hashCode () {

    return internalMap.hashCode();
  }

  /**
   * Returns {@code true} if the given object is a {@link Bag} equal to this bag according to {@link Objects#equals(Object, Object)}.
   *
   * @param obj the object to compare
   * @return {@code true} if the objects are equal
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Bag<?>) && Objects.equals(this, obj);
  }

  private class BagIterator implements Iterator<T> {

    private final Iterator<T> keyIter = internalMap.keySet().iterator();
    private T key = null;
    private int count = 0;

    /**
     * Returns {@code true} if there are more element occurrences to iterate over.
     *
     * @return {@code true} if there are remaining occurrences
     */
    @Override
    public boolean hasNext () {

      return (count > 0) || keyIter.hasNext();
    }

    /**
     * Returns the next element occurrence, advancing past one of the current element's remaining counts
     * and moving to the next distinct element when those counts are exhausted.
     *
     * @return the next element occurrence
     * @throws NoSuchElementException if no further occurrences remain
     */
    @Override
    public T next () {

      if (count == 0) {
        if (keyIter.hasNext()) {
          key = keyIter.next();
          count = internalMap.get(key);
        } else {
          key = null;
          count = 0;

          throw new NoSuchElementException();
        }
      }

      --count;

      return key;
    }

    /**
     * Removes one occurrence of the most recently returned element from the enclosing bag.
     *
     * @throws IllegalStateException if {@link #next()} has not yet been called
     */
    @Override
    public void remove () {

      if (key == null) {
        throw new IllegalStateException();
      }

      HashBag.this.remove(key);

      if (count == 0) {
        key = null;
      }
    }
  }
}
