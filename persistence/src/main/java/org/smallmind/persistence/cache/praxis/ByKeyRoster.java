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
package org.smallmind.persistence.cache.praxis;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;

/**
 * Roster implementation that stores {@link DurableKey} instances but exposes the corresponding
 * {@link Durable} objects on access. Elements are resolved lazily via an {@link ORMDao} to keep
 * cache payloads small while still supporting list semantics.
 *
 * @param <I> identifier type
 * @param <D> durable type
 */
public class ByKeyRoster<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Roster<D> {

  private final Roster<DurableKey<I, D>> keyRoster;
  private final Class<D> durableClass;
  private transient volatile ORMDao<I, D, ?, ?> ormDao;

  /**
   * Creates a roster that wraps a backing key roster for the provided durable class.
   *
   * @param durableClass durable type represented by the keys
   * @param keyRoster    underlying roster of keys
   */
  public ByKeyRoster (Class<D> durableClass, Roster<DurableKey<I, D>> keyRoster) {

    this.durableClass = durableClass;
    this.keyRoster = keyRoster;
  }

  /**
   * Lazily resolves the {@link ORMDao} used to hydrate durable instances.
   *
   * @return ORM DAO for the managed durable
   */
  private ORMDao<I, D, ?, ?> getORMDao () {

    if (ormDao == null) {
      if ((ormDao = OrmDaoManager.get(durableClass)) == null) {
        throw new CacheOperationException("Unable to locate an implementation of ORMDao within DaoManager for the requested durable(%s)", durableClass.getSimpleName());
      }
    }

    return ormDao;
  }

  /**
   * Retrieves each durable referenced by the roster keys, preferring the {@link VectoredDao} cache when present.
   *
   * @return list of hydrated durables in roster order
   */
  public List<D> prefetch () {

    ORMDao<I, D, ?, ?> ormDao;
    VectoredDao<I, D> vectoredDao;

    if ((vectoredDao = (ormDao = getORMDao()).getVectoredDao()) != null) {

      LinkedList<D> prefetchList = new LinkedList<D>();
      Map<DurableKey<I, D>, D> prefetchMap = vectoredDao.get(durableClass, keyRoster);

      for (DurableKey<I, D> durableKey : keyRoster) {

        D durable;

        if ((durable = prefetchMap.get(durableKey)) != null) {
          prefetchList.add(durable);
        } else if ((durable = ormDao.acquire(durableClass, ormDao.getIdFromString(durableKey.getIdAsString()))) != null) {
          prefetchList.add(durable);
        }
      }

      return prefetchList;
    }

    return new LinkedList<>(this);
  }

  /**
   * Resolves a durable instance for the given key.
   *
   * @param durableKey key identifying the durable
   * @return durable instance or {@code null} when the key is null
   */
  private D getDurable (DurableKey<I, D> durableKey) {

    if (durableKey == null) {

      return null;
    }

    D durable;

    if ((durable = getORMDao().get(getORMDao().getIdFromString(durableKey.getIdAsString()))) == null) {
      throw new CacheOperationException("Unable to locate the requested durable(%s) instance(%s)", durableKey.getDurableClass().getSimpleName(), durableKey.getIdAsString());
    }

    return durable;
  }

  /**
   * @return class of the durables represented by this roster
   */
  public Class<D> getDurableClass () {

    return durableClass;
  }

  /**
   * @return underlying roster of durable keys
   */
  public Roster<DurableKey<I, D>> getInternalRoster () {

    return keyRoster;
  }

  /**
   * @return number of elements in the roster
   */
  public int size () {

    return keyRoster.size();
  }

  /**
   * @return {@code true} when the roster contains no elements
   */
  public boolean isEmpty () {

    return keyRoster.isEmpty();
  }

  /**
   * Determines whether the roster contains the supplied durable.
   *
   * @param obj candidate durable
   * @return {@code true} when the corresponding key exists
   */
  public boolean contains (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyRoster.contains(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  /**
   * @return array of hydrated durables in roster order
   */
  @Override
  public Object[] toArray () {

    return toArray((Object[])null);
  }

  /**
   * Copies roster contents into the provided array, hydrating durables from their keys.
   *
   * @param a   destination array or {@code null} to allocate a new one
   * @param <T> array element type
   * @return populated array containing the roster durables
   */
  @Override
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

  /**
   * Retrieves the durable at the specified position.
   *
   * @param index position of the element to return
   * @return hydrated durable at the index
   */
  public D get (int index) {

    return getDurable(keyRoster.get(index));
  }

  /**
   * Replaces the durable at the given index and returns the previous value.
   *
   * @param index   position of the element to replace
   * @param durable new durable value
   * @return previously stored durable
   */
  public D set (int index, D durable) {

    return getDurable(keyRoster.set(index, new DurableKey<>(durableClass, durable.getId())));
  }

  /**
   * Inserts a durable at the beginning of the roster.
   *
   * @param durable durable to add
   */
  public void addFirst (D durable) {

    keyRoster.addFirst(new DurableKey<>(durableClass, durable.getId()));
  }

  /**
   * Appends a durable to the roster.
   *
   * @param durable durable to add
   * @return {@code true} when the roster changes
   */
  public boolean add (D durable) {

    return keyRoster.add(new DurableKey<>(durableClass, durable.getId()));
  }

  /**
   * Inserts a durable at the specified index.
   *
   * @param index   insertion index
   * @param durable durable to add
   */
  public void add (int index, D durable) {

    keyRoster.add(index, new DurableKey<>(durableClass, durable.getId()));
  }

  /**
   * Removes the first occurrence of the supplied durable.
   *
   * @param obj durable to remove
   * @return {@code true} when an element was removed
   */
  public boolean remove (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyRoster.remove(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
  }

  /**
   * Removes and returns the last durable in the roster.
   *
   * @return removed durable
   */
  public D removeLast () {

    return getDurable(keyRoster.removeLast());
  }

  /**
   * Removes and returns the durable at the given index.
   *
   * @param index index of the element to remove
   * @return removed durable
   */
  public D remove (int index) {

    return getDurable(keyRoster.remove(index));
  }

  /**
   * Tests whether all supplied durables are present in the roster.
   *
   * @param c collection of durables
   * @return {@code true} when every durable exists
   */
  public boolean containsAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<>();

    for (Object obj : c) {
      if (!durableClass.isAssignableFrom(obj.getClass())) {

        return false;
      }

      keySet.add(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
    }

    return keySet.containsAll(keySet);
  }

  /**
   * Adds every durable in the provided collection.
   *
   * @param c collection of durables to add
   * @return {@code true} when the roster changes
   */
  public boolean addAll (Collection<? extends D> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.addAll(keySet);
  }

  /**
   * Inserts all durables from the provided collection starting at the given index.
   *
   * @param index insertion point
   * @param c     collection of durables to add
   * @return {@code true} when the roster changes
   */
  public boolean addAll (int index, Collection<? extends D> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.addAll(index, keySet);
  }

  /**
   * Removes all durables contained in the provided collection.
   *
   * @param c collection of durables to remove
   * @return {@code true} when the roster changes
   */
  public boolean removeAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.removeAll(keySet);
  }

  /**
   * Retains only the durables contained in the provided collection.
   *
   * @param c collection of durables to retain
   * @return {@code true} when the roster changes
   */
  public boolean retainAll (Collection<?> c) {

    HashSet<DurableKey<I, D>> keySet = new HashSet<>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyRoster.retainAll(keySet);
  }

  /**
   * Clears all elements from the roster.
   */
  public void clear () {

    keyRoster.clear();
  }

  /**
   * Finds the index of the first occurrence of a durable.
   *
   * @param obj durable to locate
   * @return index or {@code -1} when not found
   */
  public int indexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyRoster.indexOf(new DurableKey<>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  /**
   * Finds the index of the last occurrence of a durable.
   *
   * @param obj durable to locate
   * @return index or {@code -1} when not found
   */
  public int lastIndexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyRoster.lastIndexOf(new DurableKey<>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  /**
   * @return iterator that hydrates durables from stored keys
   */
  public Iterator<D> iterator () {

    return new ByKeyRosterIterator<>(getORMDao(), keyRoster.listIterator());
  }

  /**
   * @return list iterator that hydrates durables from stored keys
   */
  public ListIterator<D> listIterator () {

    return new ByKeyRosterIterator<>(getORMDao(), keyRoster.listIterator());
  }

  /**
   * @param index starting position for the iterator
   * @return list iterator beginning at the supplied index
   */
  public ListIterator<D> listIterator (int index) {

    return new ByKeyRosterIterator<>(getORMDao(), keyRoster.listIterator(index));
  }

  /**
   * Creates a new roster representing the requested slice of this roster.
   *
   * @param fromIndex start index inclusive
   * @param toIndex   end index exclusive
   * @return sub roster containing the specified range
   */
  public List<D> subList (int fromIndex, int toIndex) {

    return new ByKeyRoster<>(durableClass, (IntrinsicRoster<DurableKey<I, D>>)keyRoster.subList(fromIndex, toIndex));
  }
}
