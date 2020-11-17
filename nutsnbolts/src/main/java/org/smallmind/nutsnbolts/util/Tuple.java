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

public class Tuple<K, V> implements Serializable, Cloneable, Iterable<Pair<K, V>> {

  private final ArrayList<K> keys;
  private final ArrayList<V> values;
  private int version = 0;

  public Tuple () {

    keys = new ArrayList<>();
    values = new ArrayList<>();
  }

  public void clear () {

    version++;
    keys.clear();
    values.clear();
  }

  public void add (Tuple<K, V> tuple) {

    version++;
    for (int count = 0; count < tuple.size(); count++) {
      addPair(tuple.getKey(count), tuple.getValue(count));
    }
  }

  public void addPair (K key, V value) {

    version++;
    keys.add(key);
    values.add(value);
  }

  public void addPair (int index, K key, V value) {

    version++;
    keys.add(index, key);
    values.add(index, value);
  }

  public void setPair (K key, V value) {

    int keyIndex;

    version++;
    if ((keyIndex = keys.indexOf(key)) < 0) {
      addPair(key, value);
    } else {
      setValue(keyIndex, value);
    }
  }

  public void setPair (int index, K key, V value) {

    int keyIndex;

    version++;
    if ((keyIndex = keys.indexOf(key)) < 0) {
      addPair(index, key, value);
    } else {
      setValue(keyIndex, value);
    }
  }

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

  public V removePair (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return removePair(index);
    }

    return null;
  }

  public V removePair (int index) {

    version++;
    keys.remove(index);
    return values.remove(index);
  }

  public void setValue (int index, V value) {

    version++;
    values.set(index, value);
  }

  public void setValue (K key, V value) {

    version++;
    values.set(keys.indexOf(key), value);
  }

  public int size () {

    return keys.size();
  }

  public K getKey (int index) {

    return keys.get(index);
  }

  public List<K> getKeys () {

    return keys;
  }

  public Set<K> getUniqueKeys () {

    HashSet<K> uniqueSet;

    uniqueSet = new HashSet<K>();
    for (int count = 0; count < size(); count++) {
      uniqueSet.add(keys.get(count));
    }

    return uniqueSet;
  }

  public int indexOfKey (K key) {

    return keys.indexOf(key);
  }

  public V getValue (int index) {

    return values.get(index);
  }

  public V getValue (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return values.get(index);
    }

    return null;
  }

  public List<V> getValues (K key) {

    ArrayList<V> allValues;

    if (keys.indexOf(key) < 0) {
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

  public boolean containsKey (K key) {

    return keys.contains(key);
  }

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

  @Override
  public Iterator<Pair<K, V>> iterator () {

    return new PairIterator();
  }

  public Object clone () {

    Tuple<K, V> tuple = new Tuple<>();

    for (int count = 0; count < size(); count++) {
      tuple.addPair(getKey(count), getValue(count));
    }

    return tuple;
  }

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

    @Override
    public boolean hasNext () {

      return currIndex != keys.size();
    }

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
