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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Ordered, serializable collection of key/value pairs that permits duplicate keys and preserves insertion order.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class Tuple<K, V> implements Serializable, Cloneable, Iterable<Pair<K, V>> {

  private final ArrayList<K> keys;
  private final ArrayList<V> values;
  private int version = 0;

  /**
   * Constructs an empty tuple.
   */
  public Tuple () {

    keys = new ArrayList<>();
    values = new ArrayList<>();
  }

  /**
   * Removes all key/value pairs from this tuple.
   */
  public void clear () {

    version++;
    keys.clear();
    values.clear();
  }

  /**
   * Appends all pairs from the supplied tuple to the end of this tuple in their original order.
   *
   * @param tuple the tuple whose pairs will be appended
   */
  public void add (Tuple<K, V> tuple) {

    version++;
    for (int count = 0; count < tuple.size(); count++) {
      addPair(tuple.getKey(count), tuple.getValue(count));
    }
  }

  /**
   * Appends a new key/value pair to the end of this tuple.
   *
   * @param key   key to append
   * @param value value to append
   */
  public void addPair (K key, V value) {

    version++;
    keys.add(key);
    values.add(value);
  }

  /**
   * Inserts a new key/value pair at the specified index, shifting subsequent pairs right.
   *
   * @param index zero-based position at which to insert the pair
   * @param key   key to insert
   * @param value value to insert
   */
  public void addPair (int index, K key, V value) {

    version++;
    keys.add(index, key);
    values.add(index, value);
  }

  /**
   * Updates the value for the first occurrence of the key, or appends a new pair if the key is absent.
   *
   * @param key   key to update or insert
   * @param value value to set
   */
  public void setPair (K key, V value) {

    int keyIndex;

    version++;
    if ((keyIndex = keys.indexOf(key)) < 0) {
      addPair(key, value);
    } else {
      setValue(keyIndex, value);
    }
  }

  /**
   * Updates the value for the first occurrence of the key, or inserts a new pair at the given index if the key is absent.
   *
   * @param index position at which to insert if the key is not already present
   * @param key   key to update or insert
   * @param value value to set
   */
  public void setPair (int index, K key, V value) {

    int keyIndex;

    version++;
    if ((keyIndex = keys.indexOf(key)) < 0) {
      addPair(index, key, value);
    } else {
      setValue(keyIndex, value);
    }
  }

  /**
   * Removes all pairs whose key is equal to the specified key.
   *
   * @param key the key whose pairs should be removed
   */
  public void removeKey (K key) {

    boolean modified = false;

    for (int index = keys.size() - 1; index >= 0; index--) {
      if (keys.get(index).equals(key)) {
        if (!modified) {
          version++;
          modified = true;
        }
        removePair(index);
      }
    }
  }

  /**
   * Removes the first pair whose key is equal to the specified key and returns its value.
   *
   * @param key key of the pair to remove
   * @return the removed value, or {@code null} if the key was not found
   */
  public V removePair (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return removePair(index);
    }

    return null;
  }

  /**
   * Removes the pair at the specified index and returns its value.
   *
   * @param index zero-based index of the pair to remove
   * @return the value of the removed pair
   */
  public V removePair (int index) {

    version++;
    keys.remove(index);
    return values.remove(index);
  }

  /**
   * Replaces the value at the specified index.
   *
   * @param index zero-based index of the value to replace
   * @param value the new value
   */
  public void setValue (int index, V value) {

    version++;
    values.set(index, value);
  }

  /**
   * Replaces the value for the first occurrence of the specified key.
   *
   * @param key   key whose first associated value should be replaced
   * @param value the new value
   */
  public void setValue (K key, V value) {

    version++;
    values.set(keys.indexOf(key), value);
  }

  /**
   * Returns the number of key/value pairs in this tuple.
   *
   * @return pair count
   */
  public int size () {

    return keys.size();
  }

  /**
   * Returns the key at the specified index.
   *
   * @param index zero-based index of the key to retrieve
   * @return the key at {@code index}
   */
  public K getKey (int index) {

    return keys.get(index);
  }

  /**
   * Returns the underlying key list, backed by this tuple.
   *
   * @return ordered list of all keys including duplicates
   */
  public List<K> getKeys () {

    return keys;
  }

  /**
   * Returns a set containing each distinct key that appears at least once in this tuple.
   *
   * @return set of unique keys
   */
  public Set<K> getUniqueKeys () {

    HashSet<K> uniqueSet;

    uniqueSet = new HashSet<K>();
    for (int count = 0; count < size(); count++) {
      uniqueSet.add(keys.get(count));
    }

    return uniqueSet;
  }

  /**
   * Returns the index of the first occurrence of the specified key, or {@code -1} if not present.
   *
   * @param key the key to search for
   * @return zero-based index, or {@code -1} if absent
   */
  public int indexOfKey (K key) {

    return keys.indexOf(key);
  }

  /**
   * Returns the value at the specified index.
   *
   * @param index zero-based index of the value to retrieve
   * @return the value at {@code index}
   */
  public V getValue (int index) {

    return values.get(index);
  }

  /**
   * Returns the value associated with the first occurrence of the specified key, or {@code null} if the key is absent.
   *
   * @param key the key whose value should be returned
   * @return the first matching value, or {@code null} if not found
   */
  public V getValue (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return values.get(index);
    }

    return null;
  }

  /**
   * Returns all values associated with the specified key in insertion order, or {@code null} if the key is not present.
   *
   * @param key the key whose values should be returned
   * @return list of all values for {@code key}, or {@code null} if the key is absent
   */
  public List<V> getValues (K key) {

    ArrayList<V> allValues;

    if (!keys.contains(key)) {
      return null;
    }

    allValues = new ArrayList<>();
    for (int count = 0; count < size(); count++) {
      if ((keys.get(count)).equals(key)) {
        allValues.add(values.get(count));
      }
    }

    return allValues;
  }

  /**
   * Returns {@code true} if this tuple contains at least one pair with the specified key.
   *
   * @param key key to test
   * @return {@code true} if the key is present
   */
  public boolean containsKey (K key) {

    return keys.contains(key);
  }

  /**
   * Returns {@code true} if this tuple contains at least one pair whose key and value both match the supplied arguments.
   *
   * @param key   key to search for
   * @param value value to match against pairs with that key
   * @return {@code true} if a matching key/value pair exists
   */
  public boolean containsKeyValuePair (K key, V value) {

    for (int count = 0; count < size(); count++) {
      if ((keys.get(count)).equals(key)) {
        if ((values.get(count)).equals(value)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Collapses this tuple into a {@link Map} where each key is mapped to a list of all its associated values in insertion order.
   *
   * @return map from each key to its ordered list of values
   */
  public Map<K, List<V>> asMap () {

    Map<K, List<V>> map = new HashMap<>();

    for (int count = 0; count < size(); count++) {

      List<V> valueList;

      if ((valueList = map.get(keys.get(count))) == null) {
        map.put(keys.get(count), valueList = new LinkedList<>());
      }

      valueList.add(values.get(count));
    }

    return map;
  }

  /**
   * Returns an iterator over all pairs in insertion order.
   *
   * @return iterator yielding each {@link Pair} in order
   */
  @Override
  public Iterator<Pair<K, V>> iterator () {

    return new PairIterator();
  }

  /**
   * Creates and returns a shallow copy of this tuple containing the same key and value references in the same order.
   *
   * @return a new tuple with the same pairs
   */
  public Object clone () {

    Tuple<K, V> tuple = new Tuple<>();

    for (int count = 0; count < size(); count++) {
      tuple.addPair(getKey(count), getValue(count));
    }

    return tuple;
  }

  /**
   * Returns a string in the format {@code [size](key=value;...)} listing all pairs in insertion order.
   *
   * @return human-readable representation of this tuple
   */
  public String toString () {

    StringBuilder dataBuilder;

    dataBuilder = new StringBuilder("[" + size() + "](");
    for (int count = 0; count < size(); count++) {
      if (count > 0) {
        dataBuilder.append(";");
      }

      dataBuilder.append(getKey(count));
      dataBuilder.append("=");
      dataBuilder.append(getValue(count));
    }
    dataBuilder.append(")");
    return dataBuilder.toString();
  }

  private class PairIterator implements Iterator<Pair<K, V>> {

    int currIndex = 0;
    int lastIndex = -1;
    int version = Tuple.this.version;

    /**
     * Returns {@code true} if there are more pairs to iterate.
     *
     * @return {@code true} if another pair exists
     */
    @Override
    public boolean hasNext () {

      return currIndex != keys.size();
    }

    /**
     * Returns the next pair in insertion order.
     *
     * @return the next {@link Pair}
     * @throws NoSuchElementException          if no more pairs remain
     * @throws ConcurrentModificationException if the tuple was structurally modified since this iterator was created
     */
    @Override
    public Pair<K, V> next () {

      if (version != Tuple.this.version) {
        throw new ConcurrentModificationException();
      } else if (currIndex > keys.size()) {
        throw new NoSuchElementException();
      }
      try {

        return new Pair<>(keys.get(lastIndex = currIndex), values.get(currIndex++));
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        throw new ConcurrentModificationException();
      }
    }

    /**
     * Removes the last pair returned by {@link #next()}.
     *
     * @throws IllegalStateException           if {@link #next()} has not yet been called
     * @throws ConcurrentModificationException if the tuple was structurally modified since this iterator was created
     */
    @Override
    public void remove () {

      if (lastIndex < 0) {
        throw new IllegalStateException();
      }
      if (version != Tuple.this.version) {
        throw new ConcurrentModificationException();
      }

      try {
        removePair(lastIndex);
        currIndex = lastIndex;
        lastIndex = -1;
        version = Tuple.this.version;
      } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        throw new ConcurrentModificationException();
      }
    }
  }
}
