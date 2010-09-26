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
package org.smallmind.swing.list;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class SmallMindListModel<T> implements ListModel, Serializable {

   private transient WeakEventListenerList<ListDataListener> listenerList;
   private List<T> dataList;

   public SmallMindListModel () {

      this(new ArrayList<T>());
   }

   public SmallMindListModel (List<T> dataList) {

      this.dataList = dataList;

      listenerList = new WeakEventListenerList<ListDataListener>();
   }

   private void readObject (ObjectInputStream in)
      throws IOException, ClassNotFoundException {

      in.defaultReadObject();
      listenerList = new WeakEventListenerList<ListDataListener>();
   }

   public synchronized void addListDataListener (ListDataListener listener) {

      listenerList.addListener(listener);
   }

   public synchronized void removeListDataListener (ListDataListener listener) {

      listenerList.removeListener(listener);
   }

   public synchronized int getSize () {

      return dataList.size();
   }

   public synchronized boolean isEmpty () {

      return dataList.isEmpty();
   }

   public synchronized boolean contains (T element) {

      return dataList.contains(element);
   }

   public synchronized int indexOf (T element) {

      return dataList.indexOf(element);
   }

   public synchronized void clear () {

      int size;

      if ((size = dataList.size()) > 0) {
         dataList.clear();
         fireIntervalRemoved(0, size - 1);
      }
   }

   public synchronized T getElementAt (int index) {

      return dataList.get(index);
   }

   public synchronized void setElement (int index, T element) {

      dataList.set(index, element);
      fireContentsChanged(index, index);
   }

   public synchronized void addElement (int index, T item) {

      dataList.add(index, item);
      fireIntervalAdded(index, index);
   }

   public synchronized int addElement (T element) {

      int index;

      index = dataList.size();
      dataList.add(element);
      fireIntervalAdded(index, index);

      return index;
   }

   public synchronized int addElement (T element, Comparator<T> comparator) {

      Iterator<T> dataIter;
      int index = 0;

      dataIter = dataList.iterator();
      while (dataIter.hasNext()) {
         if (comparator.compare(element, dataIter.next()) < 0) {
            break;
         }
         else {
            index++;
         }
      }

      addElement(index, element);

      return index;
   }

   public synchronized void removeElement (int index) {

      dataList.remove(index);
      fireIntervalRemoved(index, index);
   }

   public synchronized int removeElement (T element) {

      int index;

      if ((index = dataList.indexOf(element)) >= 0) {
         removeElement(index);
      }

      return index;
   }

   public synchronized void shiftElementUp (int index) {

      if (index > 0) {
         dataList.add(index - 1, dataList.remove(index));
         fireContentsChanged(index - 1, index);
      }
   }

   public synchronized void shiftElementDown (int index) {

      if (index < (dataList.size() - 1)) {
         dataList.add(index + 1, dataList.remove(index));
         fireContentsChanged(index, index + 1);
      }
   }

   public synchronized void fireContentsChanged (int index0, int index1) {

      Iterator<ListDataListener> listenerIter = listenerList.getListeners();
      ListDataEvent listDataEvent;

      listDataEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
      while (listenerIter.hasNext()) {
         listenerIter.next().contentsChanged(listDataEvent);
      }
   }

   public synchronized void fireIntervalAdded (int index0, int index1) {

      Iterator<ListDataListener> listenerIter = listenerList.getListeners();
      ListDataEvent listDataEvent;

      listDataEvent = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
      while (listenerIter.hasNext()) {
         listenerIter.next().intervalAdded(listDataEvent);
      }
   }

   public synchronized void fireIntervalRemoved (int index0, int index1) {

      Iterator<ListDataListener> listenerIter = listenerList.getListeners();
      ListDataEvent listDataEvent;

      listDataEvent = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
      while (listenerIter.hasNext()) {
         listenerIter.next().intervalRemoved(listDataEvent);
      }
   }

}
