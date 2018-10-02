/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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