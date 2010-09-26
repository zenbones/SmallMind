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
package org.smallmind.swing.calendar;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.swing.ComponentUtilities;
import org.smallmind.swing.LayoutManagerConstructionException;
import org.smallmind.swing.LayoutManagerFactory;
import org.smallmind.swing.event.DateSelectionEvent;
import org.smallmind.swing.event.DateSelectionListener;
import org.smallmind.swing.spinner.DefaultSpinnerRenderer;
import org.smallmind.swing.spinner.IntegerSpinnerModel;
import org.smallmind.swing.spinner.Spinner;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.nutsnbolts.calendar.Month;

public class DateField extends JPanel implements ChangeListener, ItemListener {

   private WeakEventListenerList<DateSelectionListener> listenerList;
   private Spinner yearSpinner;
   private Spinner monthSpinner;
   private JComboBox dayInMonthComboBox;
   private DayInMonthComboBoxModel dayInMonthComboBoxModel;

   public DateField ()
      throws LayoutManagerConstructionException {

      this(new CalendarDate(new Date()));
   }

   public DateField (CalendarDate initialDate)
      throws LayoutManagerConstructionException {

      super(LayoutManagerFactory.getLayoutManager(GridBagLayout.class));

      GridBagConstraints constraint = new GridBagConstraints();

      yearSpinner = new Spinner(new IntegerSpinnerModel(initialDate.getYear(), 1, 1, null), 300);

      monthSpinner = new Spinner(new MonthSpinnerModel(initialDate.getMonth()), 500);
      monthSpinner.setSpinnerRenderer(new DefaultSpinnerRenderer(JLabel.CENTER));

      dayInMonthComboBoxModel = new DayInMonthComboBoxModel(initialDate.getYear(), initialDate.getMonth());

      dayInMonthComboBox = new JComboBox(dayInMonthComboBoxModel);
      dayInMonthComboBox.setEditable(false);
      dayInMonthComboBox.setRenderer(new DayInMonthListCellRenderer(dayInMonthComboBoxModel));
      dayInMonthComboBox.setBackground(SystemColor.text);
      dayInMonthComboBox.setFocusable(false);
      dayInMonthComboBox.setFont(dayInMonthComboBox.getFont().deriveFont(Font.PLAIN));
      ComponentUtilities.setPreferredHeight(dayInMonthComboBox, ComponentUtilities.getPreferredHeight(monthSpinner));

      dayInMonthComboBox.setSelectedIndex(initialDate.getDay() - 1);

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(0, 0, 0, 1);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 0;
      add(monthSpinner, constraint);

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.NONE;
      constraint.insets = new Insets(0, 1, 0, 1);
      constraint.gridx = 1;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 0;
      add(dayInMonthComboBox, constraint);

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.NONE;
      constraint.insets = new Insets(0, 1, 0, 0);
      constraint.gridx = 2;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 0;
      add(yearSpinner, constraint);

      monthSpinner.addChangeListener(this);
      yearSpinner.addChangeListener(this);
      dayInMonthComboBox.addItemListener(this);

      listenerList = new WeakEventListenerList<DateSelectionListener>();
   }

   public synchronized void addDateSelectionListener (DateSelectionListener dateSelectionListener) {

      listenerList.addListener(dateSelectionListener);
   }

   public synchronized void removeDateSelectionListener (DateSelectionListener dateSelectionListener) {

      listenerList.removeListener(dateSelectionListener);
   }

   public synchronized CalendarDate getCalendarDate () {

      return new CalendarDate((Integer)yearSpinner.getValue(), ((Month)monthSpinner.getValue()).ordinal() + 1, (Integer)dayInMonthComboBoxModel.getSelectedItem());
   }

   public synchronized void setCalendarDate (CalendarDate calendarDate) {

      yearSpinner.setValue(calendarDate.getYear());
      monthSpinner.setValue(Month.values()[calendarDate.getMonth() - 1]);
      dayInMonthComboBox.setSelectedIndex(calendarDate.getDay() - 1);
   }

   public synchronized void fireDateSelected () {

      DateSelectionEvent dateSelectionEvent;

      dateSelectionEvent = new DateSelectionEvent(this, getCalendarDate());
      for (DateSelectionListener dateSelectionListener : listenerList) {
         dateSelectionListener.dateChosen(dateSelectionEvent);
      }
   }

   public synchronized void stateChanged (ChangeEvent changeEvent) {

      if (changeEvent.getSource() == monthSpinner) {
         dayInMonthComboBoxModel.setMonth(((Month)monthSpinner.getValue()).ordinal() + 1);
      }
      else if (changeEvent.getSource() == yearSpinner) {
         dayInMonthComboBoxModel.setYear((Integer)yearSpinner.getValue());
      }

      fireDateSelected();
   }

   public void itemStateChanged (ItemEvent itemEvent) {

      if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
         fireDateSelected();
      }
   }

}
