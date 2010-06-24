package org.smallmind.nutsnbolts.swing.calendar;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.nutsnbolts.util.calendar.CalendarUtilities;

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
