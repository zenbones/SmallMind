package org.smallmind.nutsnbolts.swing.calendar;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.swing.spinner.EdgeAwareSpinnerModel;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.nutsnbolts.util.calendar.CalendarUtilities;
import org.smallmind.nutsnbolts.util.calendar.Month;

public class MonthSpinnerModel implements EdgeAwareSpinnerModel {

   private WeakEventListenerList<ChangeListener> listenerList;
   private Month month;

   public MonthSpinnerModel (int month) {

      this.month = CalendarUtilities.getMonth(month);

      listenerList = new WeakEventListenerList<ChangeListener>();
   }

   public void addChangeListener (ChangeListener changeListener) {

      listenerList.addListener(changeListener);
   }

   public void removeChangeListener (ChangeListener changeListener) {

      listenerList.removeListener(changeListener);
   }

   public Object getMinimumValue () {

      return CalendarUtilities.getMonth(1);
   }

   public Object getMaximumValue () {

      return CalendarUtilities.getMonth(12);
   }

   public Object getValue () {

      return month;
   }

   public void setValue (Object value) {

      ChangeEvent changeEvent;

      this.month = (Month)value;

      changeEvent = new ChangeEvent(this);
      for (ChangeListener changeListener : listenerList) {
         changeListener.stateChanged(changeEvent);
      }
   }

   public Object getNextValue () {

      return CalendarUtilities.getMonth((month.ordinal() < 11) ? month.ordinal() + 2 : month.ordinal() + 1);
   }

   public Object getPreviousValue () {

      return CalendarUtilities.getMonth((month.ordinal() > 0) ? month.ordinal() : month.ordinal() + 1);
   }

}
