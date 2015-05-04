/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.calendar;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.smallmind.nutsnbolts.calendar.CalendarUtilities;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DayInMonthComboBoxModel implements ComboBoxModel {

  private WeakEventListenerList<ListDataListener> listenerList;
  private int year;
  private int month;
  private int selectedDay;

  public DayInMonthComboBoxModel (int year, int month) {

    this.year = year;
    this.month = month;

    selectedDay = 1;

    listenerList = new WeakEventListenerList<ListDataListener>();
  }

  public synchronized void addListDataListener (ListDataListener listDataListener) {

    listenerList.addListener(listDataListener);
  }

  public synchronized void removeListDataListener (ListDataListener listDataListener) {

    listenerList.removeListener(listDataListener);
  }

  public int getYear () {

    return year;
  }

  public synchronized void setYear (int year) {

    this.year = year;

    if (selectedDay > getSize()) {
      selectedDay = getSize();
    }
    fireContentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
  }

  public int getMonth () {

    return month;
  }

  public synchronized void setMonth (int month) {

    this.month = month;

    if (selectedDay > getSize()) {
      selectedDay = getSize();
    }
    fireContentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()));
  }

  public synchronized int getSize () {

    return CalendarUtilities.getDaysInMonth(year, month);
  }

  public synchronized void setSelectedItem (Object anItem) {

    selectedDay = (Integer)anItem;
  }

  public synchronized Object getSelectedItem () {

    return selectedDay;
  }

  public synchronized Object getElementAt (int index) {

    return index + 1;
  }

  public synchronized void fireContentsChanged (ListDataEvent listDataEvent) {

    for (ListDataListener listDataListener : listenerList) {
      listDataListener.contentsChanged(listDataEvent);
    }
  }

}
