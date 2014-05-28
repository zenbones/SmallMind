/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tuple<K, V> implements Serializable, Cloneable {

  private ArrayList<K> keys;
  private ArrayList<V> values;

  public Tuple () {

    keys = new ArrayList<K>();
    values = new ArrayList<V>();
  }

  public synchronized void clear () {

    keys.clear();
    values.clear();
  }

  public synchronized void add (Tuple<K, V> tuple) {

    for (int count = 0; count < tuple.size(); count++) {
      addPair(tuple.getKey(count), tuple.getValue(count));
    }
  }

  public synchronized void addPair (K key, V value) {

    keys.add(key);
    values.add(value);
  }

  public synchronized void addPair (int index, K key, V value) {

    keys.add(index, key);
    values.add(index, value);
  }

  public synchronized void setPair (K key, V value) {

    int keyIndex;

    if ((keyIndex = keys.indexOf(key)) < 0) {
      addPair(key, value);
    }
    else {
      setValue(keyIndex, value);
    }
  }

  public synchronized void setPair (int index, K key, V value) {

    int keyIndex;

    if ((keyIndex = keys.indexOf(key)) < 0) {
      addPair(index, key, value);
    }
    else {
      setValue(keyIndex, value);
    }
  }

  public synchronized void removeKey (K key) {

    int index;

    while ((index = keys.indexOf(key)) >= 0) {
      removePair(index);
    }
  }

  public synchronized V removePair (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return removePair(index);
    }

    return null;
  }

  public synchronized V removePair (int index) {

    keys.remove(index);
    return values.remove(index);
  }

  public synchronized void setValue (int index, V value) {

    values.set(index, value);
  }

  public synchronized void setValue (K key, V value) {

    values.set(keys.indexOf(key), value);
  }

  public synchronized int size () {

    return keys.size();
  }

  public synchronized K getKey (int index) {

    return keys.get(index);
  }

  public synchronized List<K> getKeys () {

    return keys;
  }

  public synchronized Set<K> getUniqueKeys () {

    HashSet<K> uniqueSet;

    uniqueSet = new HashSet<K>();
    for (int count = 0; count < size(); count++) {
      uniqueSet.add(keys.get(count));
    }

    return uniqueSet;
  }

  public synchronized int indexOfKey (K key) {

    return keys.indexOf(key);
  }

  public synchronized V getValue (int index) {

    return values.get(index);
  }

  public synchronized V getValue (K key) {

    int index;

    if ((index = keys.indexOf(key)) >= 0) {
      return values.get(index);
    }

    return null;
  }

  public synchronized List<V> getValues (K key) {

    ArrayList<V> allValues;

    if (keys.indexOf(key) < 0) {
      return null;
    }

    allValues = new ArrayList<V>();
    for (int count = 0; count < size(); count++) {
      if ((keys.get(count)).equals(key)) {
        allValues.add(values.get(count));
      }
    }

    return allValues;
  }

  public synchronized boolean isKey (K key) {

    return keys.contains(key);
  }

  public synchronized boolean isKeyValuePair (K key, V value) {

    for (int count = 0; count < size(); count++) {
      if ((keys.get(count)).equals(key)) {
        if ((values.get(count)).equals(value)) {
          return true;
        }
      }
    }

    return false;
  }

  public synchronized Object clone () {

    Tuple<K, V> tuple = new Tuple<K, V>();

    for (int count = 0; count < size(); count++) {
      tuple.addPair(getKey(count), getValue(count));
    }

    return tuple;
  }

  public synchronized String toString () {

    StringBuilder dataBuilder;

    dataBuilder = new StringBuilder("[" + String.valueOf(size()) + "](");
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

}
