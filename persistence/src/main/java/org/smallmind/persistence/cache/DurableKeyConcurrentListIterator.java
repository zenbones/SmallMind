package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.orm.ORMDao;

public class DurableKeyConcurrentListIterator<I extends Serializable & Comparable<I>, D extends Durable<I>, K extends DurableKey<I, D>> implements ListIterator<D> {

  private ORMDao<I, D> ormDao;
  private ListIterator<K> keyListIterator;
  private K nextKey;
  private K prevKey;
  private int nextIndex;
  private int prevIndex;

  public DurableKeyConcurrentListIterator (ORMDao<I, D> ormDao, ListIterator<K> keyListIterator) {

    this.ormDao = ormDao;
    this.keyListIterator = keyListIterator;

    setTrackingValues();
  }

  private D getDurable (K durableKey) {

    if (durableKey == null) {

      return null;
    }

    int equalsPos;

    if ((equalsPos = durableKey.getKey().indexOf('=')) < 0) {
      throw new CacheOperationException("Invalid durable key(%s)", durableKey);
    }

    return ormDao.get(ormDao.getIdFromString(durableKey.getKey().substring(equalsPos + 1)));
  }

  private void setTrackingValues () {

    nextKey = keyListIterator.hasNext() ? keyListIterator.next() : null;
    nextIndex = keyListIterator.nextIndex();
    prevKey = keyListIterator.hasPrevious() ? keyListIterator.previous() : null;
    prevIndex = keyListIterator.previousIndex();
  }

  public boolean hasNext () {

    return nextKey != null;
  }

  public D next () {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    D nextDurable = getDurable(nextKey);

    setTrackingValues();

    return nextDurable;
  }

  public boolean hasPrevious () {

    return prevKey != null;
  }

  public D previous () {

    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }

    D prevDurable = getDurable(prevKey);

    setTrackingValues();

    return prevDurable;
  }

  public int nextIndex () {

    return nextIndex;
  }

  public int previousIndex () {

    return prevIndex;
  }

  public void remove () {

    keyListIterator.remove();
  }

  public void set (D durable) {

    keyListIterator.set((K)new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }

  public void add (D durable) {

    keyListIterator.add((K)new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }
}
