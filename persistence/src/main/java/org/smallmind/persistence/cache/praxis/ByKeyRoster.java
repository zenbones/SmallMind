/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.persistence.cache.praxis;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ORMDao;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ByKeyRoster<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Roster<D> {

  private Roster<DurableKey<I, D>> keyRoster;
  private Class<D> durableClass;

  public ByKeyRoster (Class<D> durableClass, Roster<DurableKey<I, D>> keyRoster) {

    this.durableClass = durableClass;
    this.keyRoster = keyRoster;
  }

  private ORMDao<I, D> getORMDao () {

    ORMDao<I, D> ormDao;

    if ((ormDao = DaoManager.get(durableClass)) == null) {
      throw new CacheOperationException("Unable to locate an implementation of ORMDao within DaoManager for the requested durable(%s)", durableClass.getSimpleName());
    }

    return ormDao;
  }

  private D getDurable (DurableKey<I, D> durableKey) {

    if (durableKey == null) {

      return null;
    }

    int equalsPos;

    if ((equalsPos = durableKey.getKey().indexOf('=')) < 0) {
      throw new CacheOperationException("Invalid durable key(%s)", durableKey);
    }

    return getORMDao().get(getORMDao().getIdFromString(durableKey.getKey().substring(equalsPos + 1)));
  }

  public Class<D> getDurableClass () {

    return durableClass;
  }

  public Roster<DurableKey<I, D>> getInternalRoster () {

    return keyRoster;
  }

  public int size () {

    return keyRoster.size();
  }

  public boolean isEmpty () {

    return keyRoster.isEmpty();
  }

  public boolean contains (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyRoster.contains(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  public Object[] toArray () {

    return toArray(null);
  }

  public <T> T[] toArray (T[] a) {

    Object[] elements;
    Object[] keyArray = keyRoster.toArray();
    D durable;
    int index = 0;

    elements = ((a != null) && (a.length >= keyArray.length)) ? a : (Object[])Array.newInstance((a == null) ? durableClass : a.getClass().getComponentType(), keyArray.length);
    for (Object key : keyArray) {
      if ((durable = getDurable((DurableKey<I, D>)key)) != null) {
        elements[index++] = durable;
      }
    }

    return (T[])elements;
  }

  public D get (int index) {

    return getDurable(keyRoster.get(index));
  }

  public D set (int index, D durable) {

    return getDurable(keyRoster.set(index, new DurableKey<I, D>(durableClass, durable.getId())));
  }

  public void addFirst (D durable) {

    keyRoster.addFirst(new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public boolean add (D durable) {

    return keyRoster.add(new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public void add (int index, D durable) {

    keyRoster.add(index, new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public boolean remove (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyRoster.remove(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  public D removeLast () {

    return getDurable(keyRoster.removeLast());
  }

  public D remove (int index) {

    return getDurable(keyRoster.remove(index));
  }

  public boolean containsAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (!durableClass.isAssignableFrom(obj.getClass())) {

        return false;
      }

      keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
    }

    return keySet.containsAll(keySet);
  }

  public boolean addAll (Collection<? extends D> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.addAll(keySet);
  }

  public boolean addAll (int index, Collection<? extends D> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.addAll(index, keySet);
  }

  public boolean removeAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.removeAll(keySet);
  }

  public boolean retainAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.retainAll(keySet);
  }

  public void clear () {

    keyRoster.clear();
  }

  public int indexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyRoster.indexOf(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  public int lastIndexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyRoster.lastIndexOf(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  public Iterator<D> iterator () {

    return new ByKeyRosterIterator<I, D>(getORMDao(), keyRoster.listIterator());
  }

  public ListIterator<D> listIterator () {

    return new ByKeyRosterIterator<I, D>(getORMDao(), keyRoster.listIterator());
  }

  public ListIterator<D> listIterator (int index) {

    return new ByKeyRosterIterator<I, D>(getORMDao(), keyRoster.listIterator(index));
  }

  public List<D> subList (int fromIndex, int toIndex) {

    return new ByKeyRoster<I, D>(durableClass, (IntrinsicRoster<DurableKey<I, D>>)keyRoster.subList(fromIndex, toIndex));
  }
}
