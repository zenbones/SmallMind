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
 * Ordered collection of key/value pairs allowing duplicate keys while preserving insertion order.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class Tuple<K, V> implements Serializable, Cloneable, Iterable<Pair<K, V>> {

  private final ArrayList<K> keys;
  private final ArrayList<V> values;
  private int version = 0;

  /**
   * Creates an empty tuple.
   */
  public Tuple () {

    keys = new ArrayList<>();
    values = new ArrayList<>();
  }

  /**
   * Removes all pairs.
   */
  public void clear () {

    version++;
    keys.clear();
    values.clear();
  }

  /**
   * Appends all pairs from another tuple.
   *
   * @param tuple tuple whose entries will be appended
   */
  public void add (Tuple<K, V> tuple) {

    version++;
    for (int count = 0; count < tuple.size(); count++) {
      addPair(tuple.getKey(count), tuple.getValue(count));
    }
  }

  /**
   * Adds a pair to the end.
   *
   * @param key   key to add
   * @param value value to add
   */
  public void addPair (K key, V value) {

    version++;
    keys.add(key);
    values.add(value);
  }

  /**
   * Inserts a pair at a given index.
   *
   * @param index position to insert
   * @param key   key to add
   * @param value value to add
   */
  public void addPair (int index, K key, V value) {

    version++;
    keys.add(index, key);
    values.add(index, value);
  }

  /**
   * Sets the value for an existing key or adds a new pair if the key is absent.
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
   * Sets a value at a specific index, matching or inserting the key as needed.
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
   * Removes all occurrences of a key.
   *
   * @param key key to remove
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
   * Removes the first pair matching the key.
   *
   * @param key key to remove
   * @return removed value or {@code null} if not found
   */
  public V removePair (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return removePair(index);
    }

    return null;
  }

  /**
   * Removes the pair at the specified index.
   *
   * @param index index of the pair to remove
   * @return removed value
   */
  public V removePair (int index) {

    version++;
    keys.remove(index);
    return values.remove(index);
  }

  /**
   * Sets the value at the specified index.
   *
   * @param index index to update
   * @param value new value
   */
  public void setValue (int index, V value) {

    version++;
    values.set(index, value);
  }

  /**
   * Sets the value for the first occurrence of the key.
   *
   * @param key   key to update
   * @param value new value
   */
  public void setValue (K key, V value) {

    version++;
    values.set(keys.indexOf(key), value);
  }

  /**
   * @return number of pairs
   */
  public int size () {

    return keys.size();
  }

  /**
   * Returns the key at the specified index.
   */
  public K getKey (int index) {

    return keys.get(index);
  }

  /**
   * @return list view of keys (backed by the tuple)
   */
  public List<K> getKeys () {

    return keys;
  }

  /**
   * @return set of unique keys present
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
   * Returns the first index of the given key, or -1 if absent.
   */
  public int indexOfKey (K key) {

    return keys.indexOf(key);
  }

  /**
   * Returns the value at a given index.
   */
  public V getValue (int index) {

    return values.get(index);
  }

  /**
   * Returns the value for the first occurrence of the key, or {@code null} if absent.
   */
  public V getValue (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return values.get(index);
    }

    return null;
  }

  /**
   * Returns all values matching the supplied key, or {@code null} if the key is not present.
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
   * @return {@code true} if the key exists in any position
   */
  public boolean containsKey (K key) {

    return keys.contains(key);
  }

  /**
   * Tests whether the tuple contains the specific key/value pair.
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
   * Collapses the tuple into a map where each key maps to a list of values preserving order.
   *
   * @return map from key to list of values
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
   * @return iterator over pairs in insertion order
   */
  @Override
  public Iterator<Pair<K, V>> iterator () {

    return new PairIterator();
  }

  /**
   * Produces a shallow clone of the tuple (keys/values are not cloned).
   */
  public Object clone () {

    Tuple<K, V> tuple = new Tuple<>();

    for (int count = 0; count < size(); count++) {
      tuple.addPair(getKey(count), getValue(count));
    }

    return tuple;
  }

  /**
   * Formats the tuple as "[size](key=value;...)".
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
     * @return {@code true} if another pair exists
     */
    @Override
    public boolean hasNext () {

      return currIndex != keys.size();
    }

    /**
     * Returns the next pair or throws if exhausted or structurally modified.
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
     * Removes the last-returned pair.
     *
     * @throws IllegalStateException           if next() has not been called
     * @throws ConcurrentModificationException if the tuple was modified
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
