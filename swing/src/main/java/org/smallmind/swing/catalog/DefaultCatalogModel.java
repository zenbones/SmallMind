/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.swing.catalog;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DefaultCatalogModel<D> implements CatalogModel<D>, Serializable {

  private transient WeakEventListenerList<CatalogDataListener> listenerList;
  private List<D> dataList;

  public DefaultCatalogModel () {

    this(new ArrayList<D>());
  }

  public DefaultCatalogModel (List<D> dataList) {

    this.dataList = dataList;

    listenerList = new WeakEventListenerList<CatalogDataListener>();
  }

  private void readObject (ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    listenerList = new WeakEventListenerList<CatalogDataListener>();
  }

  public synchronized void addCatalogDataListener (CatalogDataListener catalogDataListener) {

    listenerList.addListener(catalogDataListener);
  }

  public synchronized void removeCatalogDataListener (CatalogDataListener catalogDataListener) {

    listenerList.removeListener(catalogDataListener);
  }

  public synchronized int getSize () {

    return dataList.size();
  }

  public synchronized boolean isEmpty () {

    return dataList.isEmpty();
  }

  public synchronized int indexOf (D element) {

    return dataList.indexOf(element);
  }

  public synchronized void clear () {

    int size;

    if ((size = dataList.size()) > 0) {
      dataList.clear();
      fireIntervalRemoved(0, size - 1);
    }
  }

  public synchronized D getElementAt (int index) {

    return dataList.get(index);
  }

  public synchronized void setElement (int index, D item) {

    dataList.set(index, item);
    fireIntervalChanged(index, index);
  }

  public synchronized void addElement (int index, D item) {

    dataList.add(index, item);
    fireItemAdded(index);
  }

  public synchronized int addElement (D item) {

    int index;

    index = dataList.size();
    dataList.add(item);
    fireItemAdded(index);

    return index;
  }

  public synchronized int addElement (D item, Comparator<D> comparator) {

    Iterator<D> dataIter;
    int index = 0;

    dataIter = dataList.iterator();
    while (dataIter.hasNext()) {
      if (comparator.compare(item, dataIter.next()) < 0) {
        break;
      }
      else {
        index++;
      }
    }

    addElement(index, item);

    return index;
  }

  public synchronized void removeElement (int index) {

    dataList.remove(index);
    fireIntervalRemoved(index, index);
  }

  public synchronized int removeElement (D element) {

    int index;

    index = dataList.indexOf(element);
    removeElement(index);

    return index;
  }

  public synchronized void shiftElementUp (int index) {

    D item;

    if (index > 0) {
      item = dataList.remove(index);
      fireIntervalRemoved(index, index);
      dataList.add(index - 1, item);
      fireItemAdded(index - 1);
    }
  }

  public synchronized void shiftElementDown (int index) {

    D item;

    if (index < (dataList.size() - 1)) {
      item = dataList.remove(index);
      fireIntervalRemoved(index, index);
      dataList.add(index + 1, item);
      fireItemAdded(index + 1);
    }
  }

  public synchronized void sort (Comparator<D> comparator) {

    if (!dataList.isEmpty()) {
      Collections.sort(dataList, comparator);
      fireIntervalChanged(0, dataList.size() - 1);
    }
  }

  public synchronized void fireItemAdded (int index) {

    Iterator<CatalogDataListener> listenerIter = listenerList.getListeners();
    CatalogDataEvent catalogDataEvent;

    catalogDataEvent = new CatalogDataEvent(this, index, index);
    while (listenerIter.hasNext()) {
      listenerIter.next().itemAdded(catalogDataEvent);
    }
  }

  public synchronized void fireIntervalRemoved (int startIndex, int endIndex) {

    Iterator<CatalogDataListener> listenerIter = listenerList.getListeners();
    CatalogDataEvent catalogDataEvent;

    catalogDataEvent = new CatalogDataEvent(this, startIndex, endIndex);
    while (listenerIter.hasNext()) {
      listenerIter.next().intervalRemoved(catalogDataEvent);
    }
  }

  public synchronized void fireIntervalChanged (int startIndex, int endIndex) {

    Iterator<CatalogDataListener> listenerIter = listenerList.getListeners();
    CatalogDataEvent catalogDataEvent;

    catalogDataEvent = new CatalogDataEvent(this, startIndex, endIndex);
    while (listenerIter.hasNext()) {
      listenerIter.next().intervalChanged(catalogDataEvent);
    }
  }

}
