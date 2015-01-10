/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.file;

import java.util.LinkedList;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class FilterComboBoxModel implements ComboBoxModel {

  private final WeakEventListenerList<ListDataListener> listenerList = new WeakEventListenerList<ListDataListener>();

  private LinkedList<FileFilter> filterList = new LinkedList<FileFilter>();
  private FileFilter selectedItem;

  public FilterComboBoxModel (FileFilter filter) {

    filterList.add(selectedItem = filter);
    if (filter != null) {
      filterList.add(null);
    }
  }

  public synchronized void setFilter (FileFilter filter) {

    if (filter != null) {
      if (filterList.size() == 1) {
        filterList.addFirst(selectedItem = filter);
      }
      else {
        filterList.set(0, selectedItem = filter);
      }

      fireContentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, 1));
    }
  }

  @Override
  public void setSelectedItem (Object selectedItem) {

    this.selectedItem = (FileFilter)selectedItem;
  }

  @Override
  public Object getSelectedItem () {

    return selectedItem;
  }

  @Override
  public int getSize () {

    return filterList.size();
  }

  @Override
  public Object getElementAt (int index) {

    return filterList.get(index);
  }

  private synchronized void fireContentsChanged (ListDataEvent listDataEvent) {

    for (ListDataListener listDataListener : listenerList) {
      listDataListener.contentsChanged(listDataEvent);
    }
  }

  @Override
  public synchronized void addListDataListener (ListDataListener listDataListener) {

    listenerList.addListener(listDataListener);
  }

  @Override
  public synchronized void removeListDataListener (ListDataListener listDataListener) {

    listenerList.removeListener(listDataListener);
  }
}