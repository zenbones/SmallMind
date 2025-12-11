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

public class HashBag<T> implements Bag<T> {

  private final HashMap<T, Integer> internalMap;
  private int size;

  public HashBag () {

    internalMap = new HashMap<>();
    size = 0;
  }

  public HashBag (Collection<? extends T> c) {

    this();

    addAll(c);
  }

  public HashBag (Bag<? extends T> b) {

    internalMap = new HashMap<>();
    size = b.size();

    for (Map.Entry<? extends T, Integer> entry : b.entrySet()) {
      internalMap.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public int size () {

    return size;
  }

  @Override
  public boolean isEmpty () {

    return size == 0;
  }

  public Integer get (T t) {

    return internalMap.get(t);
  }

  @Override
  public boolean contains (Object obj) {

    return internalMap.containsKey(obj);
  }

  @Override
  public boolean containsAll (Collection<?> c) {

    return (containsAll(new HashBag<>(c)));
  }

  public boolean containsAll (Bag<?> b) {

    for (Map.Entry<?, Integer> containedEntry : b.entrySet()) {

      Integer count;

      if (((count = internalMap.get(containedEntry.getKey())) == null) || (containedEntry.getValue() > count)) {

        return false;
      }
    }

    return true;
  }

  @Override
  public boolean add (T t) {

    return add(t, 1);
  }

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

  @Override
  public boolean remove (Object obj) {

    return remove((T)obj, 1);
  }

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

  @Override
  public void clear () {

    internalMap.clear();
    size = 0;
  }

  @Override
  public Set<T> keySet () {

    return internalMap.keySet();
  }

  public Set<Map.Entry<T, Integer>> entrySet () {

    return internalMap.entrySet();
  }

  @Override
  public Iterator<T> iterator () {

    return new BagIterator();
  }

  @Override
  public T[] toArray () {

    return (T[])toArray(new Object[size]);
  }

  @Override
  public <T1> T1[] toArray (T1[] a) {

    Iterator<T> iterator = new BagIterator();
    int index = 0;

    while (iterator.hasNext()) {
      a[index++] = (T1)iterator.next();
    }

    return a;
  }

  @Override
  public int hashCode () {

    return internalMap.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Bag<?>) && Objects.equals(this, obj);
  }

  private class BagIterator implements Iterator<T> {

    private final Iterator<T> keyIter = internalMap.keySet().iterator();
    private T key = null;
    private int count = 0;

    @Override
    public boolean hasNext () {

      return (count > 0) || keyIter.hasNext();
    }

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
