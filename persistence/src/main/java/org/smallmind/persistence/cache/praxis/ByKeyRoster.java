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
 * {@link Roster} implementation that stores {@link DurableKey} instances internally but exposes
 * the corresponding {@link Durable} objects on access, resolving them lazily via an {@link ORMDao}.
 * This keeps cache payloads compact while still presenting full list semantics to callers.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByKeyRoster<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Roster<D> {

  private final Roster<DurableKey<I, D>> keyRoster;
  private final Class<D> durableClass;
  private transient volatile ORMDao<I, D, ?, ?> ormDao;

  /**
   * Creates a roster that wraps the provided key roster and resolves durables of the given class.
   *
   * @param durableClass the durable type whose instances the keys reference
   * @param keyRoster    the underlying roster of {@link DurableKey} values
   */
  public ByKeyRoster (Class<D> durableClass, Roster<DurableKey<I, D>> keyRoster) {

    this.durableClass = durableClass;
    this.keyRoster = keyRoster;
  }

  /**
   * Lazily resolves and caches the {@link ORMDao} used to hydrate durable instances from their keys.
   *
   * @return the ORM DAO for the managed durable class
   * @throws CacheOperationException when no DAO is registered for the durable class
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
   * Hydrates all durables referenced by the roster keys, preferring the {@link VectoredDao} cache when available.
   * Keys not found in the vector cache are fetched individually via the ORM DAO.
   *
   * @return an ordered list of hydrated durables; absent entries are silently skipped
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
   * Resolves a single durable from its key.
   *
   * @param durableKey the key identifying the durable; returns {@code null} when this parameter is {@code null}
   * @return the hydrated durable
   * @throws CacheOperationException when the durable cannot be found in the backing store
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
   * Returns the class of the durables managed by this roster.
   *
   * @return the durable class
   */
  public Class<D> getDurableClass () {

    return durableClass;
  }

  /**
   * Returns the underlying key roster backing this instance.
   *
   * @return the roster of {@link DurableKey} values
   */
  public Roster<DurableKey<I, D>> getInternalRoster () {

    return keyRoster;
  }

  /**
   * Returns the number of elements in this roster.
   *
   * @return the element count
   */
  public int size () {

    return keyRoster.size();
  }

  /**
   * Returns {@code true} when this roster contains no elements.
   *
   * @return {@code true} if empty
   */
  public boolean isEmpty () {

    return keyRoster.isEmpty();
  }

  /**
   * Tests whether this roster contains a durable equal to the supplied object.
   *
   * @param obj the candidate object
   * @return {@code true} when a corresponding key exists in the key roster
   */
  public boolean contains (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyRoster.contains(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  /**
   * Returns an array of all hydrated durables in roster order.
   *
   * @return array of durables
   */
  @Override
  public Object[] toArray () {

    return toArray((Object[])null);
  }

  /**
   * Copies hydrated durables into the provided array, allocating a new array of the appropriate size when needed.
   *
   * @param a   destination array, or {@code null} to allocate a new one
   * @param <T> the component type of the array
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
   * Returns the hydrated durable at the specified position.
   *
   * @param index the zero-based position
   * @return the durable at the given index
   */
  public D get (int index) {

    return getDurable(keyRoster.get(index));
  }

  /**
   * Replaces the durable at the given index and returns the previously stored durable.
   *
   * @param index   the position to update
   * @param durable the new durable value
   * @return the durable that was previously stored at the index
   */
  public D set (int index, D durable) {

    return getDurable(keyRoster.set(index, new DurableKey<>(durableClass, durable.getId())));
  }

  /**
   * Inserts a durable at the beginning of the roster.
   *
   * @param durable the durable to prepend
   */
  public void addFirst (D durable) {

    keyRoster.addFirst(new DurableKey<>(durableClass, durable.getId()));
  }

  /**
   * Appends a durable to the end of the roster.
   *
   * @param durable the durable to append
   * @return {@code true} when the roster changes
   */
  public boolean add (D durable) {

    return keyRoster.add(new DurableKey<>(durableClass, durable.getId()));
  }

  /**
   * Inserts a durable at the specified position.
   *
   * @param index   the insertion index
   * @param durable the durable to insert
   */
  public void add (int index, D durable) {

    keyRoster.add(index, new DurableKey<>(durableClass, durable.getId()));
  }

  /**
   * Removes the first occurrence of the specified durable.
   *
   * @param obj the durable to remove
   * @return {@code true} when an element was removed
   */
  public boolean remove (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyRoster.remove(new DurableKey<>(durableClass, durableClass.cast(obj).getId()));
  }

  /**
   * Removes and returns the last durable in the roster.
   *
   * @return the removed durable
   */
  public D removeLast () {

    return getDurable(keyRoster.removeLast());
  }

  /**
   * Removes and returns the durable at the given index.
   *
   * @param index the position to remove
   * @return the durable that was at the index
   */
  public D remove (int index) {

    return getDurable(keyRoster.remove(index));
  }

  /**
   * Tests whether all durables in the provided collection are present in this roster.
   *
   * @param c the collection of durables to check
   * @return {@code true} when every element in {@code c} is found
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
   * Appends all durables from the provided collection to this roster.
   *
   * @param c the collection of durables to add
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
   * Inserts all durables from the provided collection starting at the specified index.
   *
   * @param index the insertion point
   * @param c     the collection of durables to add
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
   * Removes all durables contained in the provided collection from this roster.
   *
   * @param c the collection of durables to remove
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
   * Retains only those durables that are present in the provided collection.
   *
   * @param c the collection of durables to retain
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
   * Removes all elements from this roster.
   */
  public void clear () {

    keyRoster.clear();
  }

  /**
   * Returns the index of the first occurrence of the specified durable.
   *
   * @param obj the durable to locate
   * @return the zero-based index, or {@code -1} when not found
   */
  public int indexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyRoster.indexOf(new DurableKey<>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  /**
   * Returns the index of the last occurrence of the specified durable.
   *
   * @param obj the durable to locate
   * @return the zero-based index, or {@code -1} when not found
   */
  public int lastIndexOf (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) ? keyRoster.lastIndexOf(new DurableKey<>(durableClass, durableClass.cast(obj).getId())) : -1;
  }

  /**
   * Returns an iterator that hydrates each durable from its stored key as it is traversed.
   *
   * @return an iterator over the hydrated durables
   */
  public Iterator<D> iterator () {

    return new ByKeyRosterIterator<>(getORMDao(), keyRoster.listIterator());
  }

  /**
   * Returns a list iterator that hydrates each durable from its stored key as it is traversed.
   *
   * @return a list iterator over the hydrated durables
   */
  public ListIterator<D> listIterator () {

    return new ByKeyRosterIterator<>(getORMDao(), keyRoster.listIterator());
  }

  /**
   * Returns a list iterator starting at the given index that hydrates durables from their stored keys.
   *
   * @param index the starting position
   * @return a positioned list iterator over the hydrated durables
   */
  public ListIterator<D> listIterator (int index) {

    return new ByKeyRosterIterator<>(getORMDao(), keyRoster.listIterator(index));
  }

  /**
   * Returns a view of this roster between the specified indices.
   *
   * @param fromIndex the start index, inclusive
   * @param toIndex   the end index, exclusive
   * @return a {@link ByKeyRoster} over the requested range
   */
  public List<D> subList (int fromIndex, int toIndex) {

    return new ByKeyRoster<>(durableClass, (IntrinsicRoster<DurableKey<I, D>>)keyRoster.subList(fromIndex, toIndex));
  }
}
