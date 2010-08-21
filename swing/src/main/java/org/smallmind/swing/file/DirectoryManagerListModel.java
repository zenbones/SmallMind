package org.smallmind.swing.file;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DirectoryManagerListModel implements ListModel {

   private WeakEventListenerList<ListDataListener> listenerList;
   private List<File> directoryList;

   public DirectoryManagerListModel (List<File> directoryList) {

      this.directoryList = directoryList;

      listenerList = new WeakEventListenerList<ListDataListener>();
   }

   public void addListDataListener (ListDataListener listDataListener) {

      listenerList.addListener(listDataListener);
   }

   public void removeListDataListener (ListDataListener listDataListener) {

      listenerList.removeListener(listDataListener);
   }

   public synchronized Object getElementAt (int index) {

      return directoryList.get(index);
   }

   public synchronized int getSize () {

      return directoryList.size();
   }

   public synchronized void addDirectory (File directory) {

      int index;

      index = directoryList.size();
      directoryList.add(directory);
      fireIntervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index + 1));
   }

   public synchronized void removeDirectory (int index) {

      directoryList.remove(index);
      fireIntervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
   }

   private synchronized void fireIntervalAdded (ListDataEvent listDataEvent) {

      Iterator<ListDataListener> listenerIter = listenerList.getListeners();

      while (listenerIter.hasNext()) {
         listenerIter.next().intervalAdded(listDataEvent);
      }
   }

   private synchronized void fireIntervalRemoved (ListDataEvent listDataEvent) {

      Iterator<ListDataListener> listenerIter = listenerList.getListeners();

      while (listenerIter.hasNext()) {
         listenerIter.next().intervalRemoved(listDataEvent);
      }
   }

}