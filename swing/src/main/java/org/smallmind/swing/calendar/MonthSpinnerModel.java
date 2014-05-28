/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.swing.calendar;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.calendar.CalendarUtilities;
import org.smallmind.nutsnbolts.calendar.Month;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.spinner.EdgeAwareSpinnerModel;

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
