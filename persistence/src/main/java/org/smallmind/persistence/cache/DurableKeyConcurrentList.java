/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.quorum.util.ConcurrentList;
import org.terracotta.annotations.HonorTransient;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
@HonorTransient
public class DurableKeyConcurrentList<I extends Serializable & Comparable<I>, D extends Durable<I>> implements List<D> {

  private transient AtomicReference<ORMDao<I, D>> ormDaoRef;
  private transient boolean ormReferenced;

  private ConcurrentList<DurableKey<I, D>> keyList;
  private Class<D> durableClass;

  private DurableKeyConcurrentList () {

    ormReferenced = false;
    ormDaoRef = new AtomicReference<ORMDao<I, D>>();
  }

  protected DurableKeyConcurrentList (DurableKeyConcurrentList<I, D> durableKeyConcurrentList) {

    this(durableKeyConcurrentList.getDurableClass(), new ConcurrentList<DurableKey<I, D>>(durableKeyConcurrentList.getKeyList()));
  }

  public DurableKeyConcurrentList (Class<D> durableClass, ConcurrentList<DurableKey<I, D>> keyList) {

    this();

    this.durableClass = durableClass;
    this.keyList = keyList;
  }

  private ORMDao<I, D> getORMDao () {

    if (!ormReferenced) {

      ORMDao<I, D> ormDao;

      if ((ormDao = DaoManager.get(durableClass)) == null) {
        throw new CacheOperationException("Unable to locate an implementation of ORMDao within DaoManager for the requested durable(%s)", durableClass.getSimpleName());
      }

      if (ormDaoRef.compareAndSet(null, ormDao)) {
        ormReferenced = true;
      }
    }

    return ormDaoRef.get();
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

  private Class<D> getDurableClass () {

    return durableClass;
  }

  private ConcurrentList<DurableKey<I, D>> getKeyList () {

    return keyList;
  }

  public int size () {

    return keyList.size();
  }

  public boolean isEmpty () {

    return keyList.isEmpty();
  }

  public boolean contains (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyList.contains(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  public Object[] toArray () {

    return toArray(null);
  }

  public <T> T[] toArray (T[] a) {

    Object[] elements;
    Object[] keyArray = keyList.toArray();
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

    return getDurable(keyList.get(index));
  }

  public D set (int index, D durable) {

    return getDurable(keyList.set(index, new DurableKey<I, D>(durableClass, durable.getId())));
  }

  public void addFirst (D durable) {

    keyList.addFirst(new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public boolean add (D durable) {

    return keyList.add(new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public void add (int index, D durable) {

    keyList.add(index, new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public boolean remove (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyList.remove(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  public D removeLast () {

    return getDurable(keyList.removeLast());
  }

  public D remove (int index) {

    return getDurable(keyList.remove(index));
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

    return keyList.addAll(keySet);
  }

  public boolean addAll (int index, Collection<? extends D> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyList.addAll(index, keySet);
  }

  public boolean removeAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyList.removeAll(keySet);
  }

  public boolean retainAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<DurableKey<I, D>>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyList.retainAll(keySet);
  }

  public void clear () {

    keyList.clear();
  }

  public int indexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyList.indexOf(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  public int lastIndexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyList.lastIndexOf(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  public Iterator<D> iterator () {

    return new DurableKeyConcurrentListIterator<I, D>(getORMDao(), keyList.listIterator());
  }

  public ListIterator<D> listIterator () {

    return new DurableKeyConcurrentListIterator<I, D>(getORMDao(), keyList.listIterator());
  }

  public ListIterator<D> listIterator (int index) {

    return new DurableKeyConcurrentListIterator<I, D>(getORMDao(), keyList.listIterator(index));
  }

  public List<D> subList (int fromIndex, int toIndex) {

    return new DurableKeyConcurrentList<I, D>(durableClass, (ConcurrentList<DurableKey<I, D>>)keyList.subList(fromIndex, toIndex));
  }
}
