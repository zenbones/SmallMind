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
