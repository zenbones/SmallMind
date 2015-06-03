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
package org.smallmind.swing.calendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.smallmind.nutsnbolts.calendar.CalendarUtility;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DateRangeTableModel implements TableModel {

  private static final String[] DAY_CHARS = {"S", "M", "T", "W", "T", "F", "S"};

  private WeakEventListenerList<TableModelListener> eventList;
  private boolean underunFlag = false;
  private boolean overunFlag = false;
  private int todayRow;
  private int todayColumn;
  private int anchorYear;
  private int anchorMonth;
  private int anchorDay;
  private int totalDays;

  public DateRangeTableModel (int year, int month, int day, int extentDays) {

    int underrunDays;

    if (year < 1) {
      throw new IllegalArgumentException("Parameter 'year' must be greater than 0");
    }
    if ((month < 1) || (month > 12)) {
      throw new IllegalArgumentException("Parameter 'month' must be between 1 and 12");
    }
    if ((day < 1) || (day > CalendarUtility.getDaysInMonth(year, month))) {
      throw new IllegalArgumentException("Parameter 'day' must be between 1 and " + CalendarUtility.getDaysInMonth(year, month));
    }

    eventList = new WeakEventListenerList<TableModelListener>();

    anchorYear = year;
    anchorMonth = month;
    anchorDay = 1;

    if ((underrunDays = CalendarUtility.getDayOfWeek(year, month, 1) - 1) > 0) {
      underunFlag = true;
      anchorDay -= underrunDays;
      if (anchorDay < 1) {
        anchorMonth--;
        if (anchorMonth < 1) {
          anchorMonth = 12;
          anchorYear--;
        }

        anchorDay += CalendarUtility.getDaysInMonth(anchorYear, anchorMonth);
      }
    }

    totalDays = setTotalDays(underrunDays + extentDays + day - 1);
  }

  private int setTotalDays (int partialDays) {

    Calendar today = GregorianCalendar.getInstance();
    int currentYear = anchorYear;
    int currentMonth = anchorMonth;
    int currentDay = anchorDay;
    int accumulatedDays = 0;

    while (accumulatedDays < partialDays) {
      if ((currentYear == today.get(Calendar.YEAR)) && (currentMonth == today.get(Calendar.MONTH) + 1)) {
        todayRow = (accumulatedDays + today.get(Calendar.DAY_OF_MONTH)) / 7;
        todayColumn = ((accumulatedDays + today.get(Calendar.DAY_OF_MONTH)) % 7) - 1;
      }

      accumulatedDays += CalendarUtility.getDaysInMonth(currentYear, currentMonth) - currentDay + 1;
      currentDay = 1;
      currentMonth++;
      if (currentMonth > 12) {
        currentMonth = 1;
        currentYear++;
      }
    }

    if ((accumulatedDays % 7) != 0) {
      overunFlag = true;
      accumulatedDays += 7 - (accumulatedDays % 7);
    }

    return accumulatedDays;
  }

  public void addTableModelListener (TableModelListener tableModelListener) {

    eventList.addListener(tableModelListener);
  }

  public void removeTableModelListener (TableModelListener tableModelListener) {

    eventList.removeListener(tableModelListener);
  }

  public boolean hasUnderun () {

    return underunFlag;
  }

  public boolean hasOverun () {

    return overunFlag;
  }

  public int getTodayRow () {

    return todayRow;
  }

  public int getTodayColumn () {

    return todayColumn;
  }

  public int getStartingMonth () {

    return anchorMonth;
  }

  public int getRow (CalendarDate calendarDate) {

    return getIndexDays(calendarDate) / 7;
  }

  public int getColumn (CalendarDate calendarDate) {

    int indexDays = getIndexDays(calendarDate);

    return indexDays - ((indexDays / 7) * 7);
  }

  private int getIndexDays (CalendarDate calendarDate) {

    int indexDays = 0;

    if (calendarDate.getYear() < anchorYear) {
      throw new IndexOutOfBoundsException("Date is before the start of calendar(" + calendarDate + ")");
    }
    else if (calendarDate.getYear() > anchorYear) {
      indexDays += CalendarUtility.getDaysInMonth(anchorYear, anchorMonth) - anchorDay;
      for (int count = anchorMonth + 1; count <= 12; count++) {
        indexDays += CalendarUtility.getDaysInMonth(anchorYear, count);
      }
    }
    else if (calendarDate.getMonth() < anchorMonth) {
      throw new IndexOutOfBoundsException("Date is before the start of calendar(" + calendarDate + ")");
    }
    else if (calendarDate.getMonth() > anchorMonth) {
      indexDays += CalendarUtility.getDaysInMonth(anchorYear, anchorMonth) - anchorDay;
    }
    else if (calendarDate.getDay() < anchorDay) {
      throw new IndexOutOfBoundsException("Date is before the start of calendar(" + calendarDate + ")");
    }

    for (int count = (calendarDate.getYear() == anchorYear) ? (anchorMonth + 1) : 1; count < calendarDate.getMonth(); count++) {
      indexDays += CalendarUtility.getDaysInMonth(calendarDate.getYear(), count);
    }

    indexDays += (calendarDate.getDay() - (((calendarDate.getYear() == anchorYear) && (calendarDate.getMonth() == anchorMonth)) ? anchorDay : 0));
    if (indexDays >= totalDays) {
      throw new IndexOutOfBoundsException("Date is after the end of calendar(" + calendarDate + ")");
    }

    return indexDays;
  }

  public Class<?> getColumnClass (int columnIndex) {

    return CalendarDate.class;
  }

  public int getColumnCount () {

    return 7;
  }

  public String getColumnName (int columnIndex) {

    return DAY_CHARS[columnIndex];
  }

  public int getRowCount () {

    return totalDays / 7;
  }

  public Object getValueAt (int rowIndex, int columnIndex) {

    int currentYear = anchorYear;
    int currentMonth = anchorMonth;
    int currentDay = anchorDay;
    int indexDays = (rowIndex * 7) + columnIndex;

    while ((currentDay + indexDays) > CalendarUtility.getDaysInMonth(currentYear, currentMonth)) {
      indexDays -= CalendarUtility.getDaysInMonth(currentYear, currentMonth) - currentDay;
      currentDay = 0;
      currentMonth++;
      if (currentMonth > 12) {
        currentMonth = 1;
        currentYear++;
      }
    }

    return new CalendarDate(currentYear, currentMonth, currentDay + indexDays);
  }

  public boolean isCellEditable (int rowIndex, int columnIndex) {

    return false;
  }

  public void setValueAt (Object aValue, int rowIndex, int columnIndex) {

  }

}
