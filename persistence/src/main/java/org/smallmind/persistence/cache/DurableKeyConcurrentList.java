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
public class DurableKeyConcurrentList<I extends Serializable & Comparable<I>, D extends Durable<I>, K extends DurableKey<I, D>> implements List<D> {

  private transient AtomicReference<ORMDao<I, D>> ormDaoRef;
  private transient boolean ormReferenced;

  private ConcurrentList<K> keyList;
  private Class<D> durableClass;

  public DurableKeyConcurrentList () {

    ormReferenced = false;
    ormDaoRef = new AtomicReference<ORMDao<I, D>>();
  }

  public DurableKeyConcurrentList (Class<D> durableClass, ConcurrentList<K> keyList) {

    this();

    this.durableClass = durableClass;
    this.keyList = keyList;
  }

  private ORMDao<I, D> getORMDao () {

    if (!ormReferenced) {
      if (ormDaoRef.compareAndSet(null, DaoManager.get(durableClass))) {
        ormReferenced = true;
      }
    }

    return ormDaoRef.get();
  }

  private D getDurable (K durableKey) {

    if (durableKey == null) {

      return null;
    }

    int equalsPos;

    if ((equalsPos = durableKey.getKey().indexOf('=')) < 0) {
      throw new CacheOperationException("Invalid durable key(%s)", durableKey);
    }

    return getORMDao().get(getORMDao().getIdFromString(durableKey.getKey().substring(equalsPos + 1)));
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
      if ((durable = getDurable((K)key)) != null) {
        elements[index++] = durable;
      }
    }

    return (T[])elements;
  }

  public D get (int index) {

    return getDurable(keyList.get(index));
  }

  public D set (int index, D durable) {

    return getDurable(keyList.set(index, (K)new DurableKey<I, D>(durableClass, durable.getId())));
  }

  public boolean add (D durable) {

    return keyList.add((K)new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public void add (int index, D durable) {

    keyList.add(index, (K)new DurableKey<I, D>(durableClass, durable.getId()));
  }

  public boolean remove (Object obj) {

    return durableClass.isAssignableFrom(obj.getClass()) && keyList.remove(new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
  }

  public D remove (int index) {

    return getDurable(keyList.remove(index));
  }

  public boolean containsAll (Collection<?> c) {

    HashSet<K> keySet = new HashSet<K>();

    for (Object obj : c) {
      if (!durableClass.isAssignableFrom(obj.getClass())) {

        return false;
      }

      keySet.add((K)new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
    }

    return keySet.containsAll(keySet);
  }

  public boolean addAll (Collection<? extends D> c) {

    HashSet<K> keySet = new HashSet<K>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add((K)new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyList.addAll(keySet);
  }

  public boolean addAll (int index, Collection<? extends D> c) {

    HashSet<K> keySet = new HashSet<K>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add((K)new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyList.addAll(index, keySet);
  }

  public boolean removeAll (Collection<?> c) {

    HashSet<K> keySet = new HashSet<K>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add((K)new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
      }
    }

    return keyList.removeAll(keySet);
  }

  public boolean retainAll (Collection<?> c) {

    HashSet<K> keySet = new HashSet<K>();

    for (Object obj : c) {
      if (durableClass.isAssignableFrom(obj.getClass())) {
        keySet.add((K)new DurableKey<I, D>(durableClass, durableClass.cast(obj).getId()));
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

    return new DurableKeyConcurrentListIterator<I, D, K>(getORMDao(), keyList.listIterator());
  }

  public ListIterator<D> listIterator () {

    return new DurableKeyConcurrentListIterator<I, D, K>(getORMDao(), keyList.listIterator());
  }

  public ListIterator<D> listIterator (int index) {

    return new DurableKeyConcurrentListIterator<I, D, K>(getORMDao(), keyList.listIterator(index));
  }

  public List<D> subList (int fromIndex, int toIndex) {

    return new DurableKeyConcurrentList<I, D, K>(durableClass, (ConcurrentList<K>)keyList.subList(fromIndex, toIndex));
  }
}
