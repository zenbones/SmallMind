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
 * Hash-based {@link Bag} implementation tracking element multiplicities.
 *
 * @param <T> element type
 */
public class HashBag<T> implements Bag<T> {

  private final HashMap<T, Integer> internalMap;
  private int size;

  /**
   * Creates an empty bag.
   */
  public HashBag () {

    internalMap = new HashMap<>();
    size = 0;
  }

  /**
   * Seeds the bag with the contents of a collection (each occurrence counted once).
   *
   * @param c initial elements
   */
  public HashBag (Collection<? extends T> c) {

    this();

    addAll(c);
  }

  /**
   * Seeds the bag by copying multiplicities from another bag.
   *
   * @param b source bag
   */
  public HashBag (Bag<? extends T> b) {

    internalMap = new HashMap<>();
    size = b.size();

    for (Map.Entry<? extends T, Integer> entry : b.entrySet()) {
      internalMap.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * @return total element count including multiplicity
   */
  @Override
  public int size () {

    return size;
  }

  /**
   * @return {@code true} when the bag has no elements
   */
  @Override
  public boolean isEmpty () {

    return size == 0;
  }

  /**
   * Returns the multiplicity for a given element.
   *
   * @param t element to query
   * @return count or {@code null} if absent
   */
  public Integer get (T t) {

    return internalMap.get(t);
  }

  /**
   * @return {@code true} if at least one instance of the object exists in the bag
   */
  @Override
  public boolean contains (Object obj) {

    return internalMap.containsKey(obj);
  }

  /**
   * Tests whether all elements in the collection are present at least once.
   */
  @Override
  public boolean containsAll (Collection<?> c) {

    return (containsAll(new HashBag<>(c)));
  }

  /**
   * Tests whether this bag contains all elements (respecting multiplicity) of another bag.
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
   * Adds one occurrence of the element.
   */
  @Override
  public boolean add (T t) {

    return add(t, 1);
  }

  /**
   * Adds the element with a specified multiplicity.
   *
   * @param multiple number of occurrences to add; must be positive
   * @throws IllegalStateException if {@code multiple < 1}
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
   * Removes a single occurrence of the element.
   */
  @Override
  public boolean remove (Object obj) {

    return remove((T)obj, 1);
  }

  /**
   * Removes up to {@code multiple} occurrences of the element.
   *
   * @param t        element to remove
   * @param multiple number of occurrences to remove; must be positive
   * @return {@code true} if the bag changed
   * @throws IllegalStateException if {@code multiple < 1}
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
   * Adds each element of the collection once.
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
   * Removes one occurrence of each element present in the collection.
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
   * Retains only those elements also present in the provided collection (by multiplicity).
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
   * Removes all entries from the bag.
   */
  @Override
  public void clear () {

    internalMap.clear();
    size = 0;
  }

  /**
   * @return set of distinct keys contained in the bag
   */
  @Override
  public Set<T> keySet () {

    return internalMap.keySet();
  }

  /**
   * @return entries mapping each key to its multiplicity
   */
  public Set<Map.Entry<T, Integer>> entrySet () {

    return internalMap.entrySet();
  }

  /**
   * Iterates over all elements, repeating values according to their multiplicity.
   */
  @Override
  public Iterator<T> iterator () {

    return new BagIterator();
  }

  /**
   * @return array containing each element occurrence
   */
  @Override
  public T[] toArray () {

    return (T[])toArray(new Object[size]);
  }

  /**
   * Populates the supplied array (or a new one) with each element occurrence.
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
   * Hash code is derived from the internal map of counts.
   */
  @Override
  public int hashCode () {

    return internalMap.hashCode();
  }

  /**
   * Equality is delegated to {@link Objects#equals(Object, Object)} on this bag and the other object.
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
     * @return {@code true} if another occurrence exists
     */
    @Override
    public boolean hasNext () {

      return (count > 0) || keyIter.hasNext();
    }

    /**
     * Returns the next element occurrence, throwing {@link NoSuchElementException} when exhausted.
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
     * Removes one occurrence of the most recently returned element.
     *
     * @throws IllegalStateException if called before {@link #next()}
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
