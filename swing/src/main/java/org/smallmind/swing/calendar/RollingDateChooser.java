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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellRenderer;
import org.smallmind.swing.ComponentUtilities;
import org.smallmind.swing.LayoutManagerConstructionException;
import org.smallmind.swing.LayoutManagerFactory;
import org.smallmind.swing.VerticalTextIcon;
import org.smallmind.swing.event.DateSelectionEvent;
import org.smallmind.swing.event.DateSelectionListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class RollingDateChooser extends JPanel implements ListSelectionListener, TableColumnModelListener {

   private static final Dimension PREFERRED_DIMENSION = new Dimension(200, 300);
   private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "Auhust", "September", "October", "November", "December"};

   private WeakEventListenerList<DateSelectionListener> listenerList;
   private HashSet<CalendarDate> markedSet;
   private JTable rollingMonthTable;
   private DateRangeTableModel model;
   private int selectedRow = -1;
   private int selectedColumn = -1;

   public RollingDateChooser (int year, int month, int day, int extentDays)
      throws LayoutManagerConstructionException {

      super(LayoutManagerFactory.getLayoutManager(GridLayout.class, new Class[] {int.class, int.class}, new Object[] {1, 0}));

      JScrollPane rollingMonthScrollPane;

      listenerList = new WeakEventListenerList<DateSelectionListener>();
      markedSet = new HashSet<CalendarDate>();
      model = new DateRangeTableModel(year, month, day, extentDays);

      rollingMonthTable = new JTable(model);
      rollingMonthTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      rollingMonthTable.setCellSelectionEnabled(true);
      rollingMonthTable.setDefaultRenderer(CalendarDate.class, new DefaultRollingDateChooserCellRenderer(this));
      rollingMonthTable.setDragEnabled(false);
      rollingMonthTable.setIntercellSpacing(new Dimension(0, 0));
      rollingMonthTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      rollingMonthTable.setShowGrid(false);
      rollingMonthTable.setRowHeight(20);

      rollingMonthTable.getTableHeader().setResizingAllowed(false);
      rollingMonthTable.getTableHeader().setReorderingAllowed(false);
      rollingMonthTable.getTableHeader().setDefaultRenderer(new DayHeaderTableCellRenderer());

      rollingMonthScrollPane = new JScrollPane(rollingMonthTable);
      rollingMonthScrollPane.setRowHeaderView(new MonthHeaderBar(model, rollingMonthTable.getRowHeight()));
      rollingMonthScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      rollingMonthScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      rollingMonthScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new CornerPanel());
      rollingMonthScrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new CornerPanel());
      rollingMonthScrollPane.getViewport().setBackground(SystemColor.desktop);

      add(rollingMonthScrollPane);

      rollingMonthTable.getSelectionModel().addListSelectionListener(this);
      rollingMonthTable.getColumnModel().addColumnModelListener(this);
   }

   public synchronized void addDateSelectionListener (DateSelectionListener dateSelectionListener) {

      listenerList.addListener(dateSelectionListener);
   }

   public synchronized void removeDateSelectionListener (DateSelectionListener dateSelectionListener) {

      listenerList.removeListener(dateSelectionListener);
   }

   public synchronized void setCellRenderer (RollingDateChooserCellRenderer cellRenderer) {

      rollingMonthTable.setDefaultRenderer(CalendarDate.class, cellRenderer);
   }

   public synchronized int getTodayRow () {

      return model.getTodayRow();
   }

   public synchronized int getTodayColumn () {

      return model.getTodayColumn();
   }

   public void requestFocus () {

      rollingMonthTable.requestFocusInWindow();
   }

   public synchronized boolean isMarked (CalendarDate calendarDate) {

      return markedSet.contains(calendarDate);
   }

   public synchronized void markDate (CalendarDate calendarDate, boolean set) {

      if (set) {
         markedSet.add(calendarDate);
      }
      else {
         markedSet.remove(calendarDate);
      }

      repaint();
   }

   public synchronized void clearSelection () {

      rollingMonthTable.clearSelection();
   }

   public synchronized void setSelectedDate (CalendarDate calendarDate) {

      int row = model.getRow(calendarDate);
      int column = model.getColumn(calendarDate);

      rollingMonthTable.setRowSelectionInterval(row, row);
      rollingMonthTable.setColumnSelectionInterval(column, column);
   }

   public synchronized void fireDateSelected () {

      DateSelectionEvent dateSelectionEvent;

      dateSelectionEvent = new DateSelectionEvent(this, (CalendarDate)model.getValueAt(selectedRow, selectedColumn));
      for (DateSelectionListener dateSelectionListener : listenerList) {
         dateSelectionListener.dateChosen(dateSelectionEvent);
      }
   }

   public Dimension getPreferredSize () {

      return PREFERRED_DIMENSION;
   }

   public Dimension getMinimumSize () {

      return PREFERRED_DIMENSION;
   }

   public synchronized void valueChanged (ListSelectionEvent listSelectionEvent) {

      if (!listSelectionEvent.getValueIsAdjusting()) {
         selectedRow = rollingMonthTable.getSelectedRow();
         selectedColumn = rollingMonthTable.getSelectedColumn();
         fireDateSelected();
      }
   }

   public void columnAdded (TableColumnModelEvent tableColumnModelEvent) {
   }

   public void columnMarginChanged (ChangeEvent tableColumnModelEvent) {
   }

   public void columnMoved (TableColumnModelEvent tableColumnModelEvent) {
   }

   public void columnRemoved (TableColumnModelEvent tableColumnModelEvent) {
   }

   public synchronized void columnSelectionChanged (ListSelectionEvent listSelectionEvent) {

      if ((!listSelectionEvent.getValueIsAdjusting()) && (selectedColumn != rollingMonthTable.getSelectedColumn())) {
         selectedColumn = rollingMonthTable.getSelectedColumn();
         selectedRow = rollingMonthTable.getSelectedRow();
         fireDateSelected();
      }
   }

   private class CornerPanel extends JPanel {

      public CornerPanel () {

         setBackground(SystemColor.textHighlight);
         setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, SystemColor.textHighlight.brighter()));
      }

   }

   private class MonthHeaderBar extends JPanel {

      private GridBagConstraints constraint;

      public MonthHeaderBar (DateRangeTableModel model, int rowHeight)
         throws LayoutManagerConstructionException {

         super(LayoutManagerFactory.getLayoutManager(GridBagLayout.class));

         int month;
         int currentDay = 0;
         int breakRow = 0;
         int index = 0;

         constraint = new GridBagConstraints();

         month = model.getStartingMonth();
         for (int count = 0; count < model.getRowCount(); count++) {
            if (((CalendarDate)model.getValueAt(count, 0)).getDay() < currentDay) {
               if ((count == 1) && model.hasUnderun()) {
                  setMonthLabel(0, index++, rowHeight);
               }
               else {
                  setMonthLabel(month, index++, (count - breakRow) * rowHeight);
               }

               breakRow = count;
               month++;
               if (month > 12) {
                  month = 1;
               }
            }

            currentDay = ((CalendarDate)model.getValueAt(count, 0)).getDay();
         }

         setMonthLabel(month, index, (model.getRowCount() - breakRow) * rowHeight);
      }

      private void setMonthLabel (int month, int index, int height) {

         JLabel monthLabel;
         Icon monthIcon;

         monthLabel = new JLabel();
         monthLabel.setOpaque(true);
         monthLabel.setBackground(SystemColor.textHighlight);
         monthLabel.setForeground(SystemColor.textHighlightText);
         monthLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.textHighlight.darker()), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder((month == 0) ? 0 : 1, 0, 0, 2, SystemColor.textHighlight.brighter()), BorderFactory.createMatteBorder(3, 1, 3, 3, SystemColor.textHighlight))));

         monthIcon = new VerticalTextIcon(monthLabel.getFontMetrics(monthLabel.getFont().deriveFont(Font.BOLD)), (month == 0) ? " " : MONTHS[month - 1], VerticalTextIcon.ROTATE_LEFT);
         monthLabel.setIcon(monthIcon);

         ComponentUtilities.setPreferredHeight(monthLabel, height);

         constraint.anchor = GridBagConstraints.NORTHWEST;
         constraint.fill = GridBagConstraints.NONE;
         constraint.gridx = 0;
         constraint.gridy = index;
         constraint.weightx = 0;
         constraint.weighty = 1;
         add(monthLabel, constraint);
      }

   }

   private class DayHeaderTableCellRenderer implements TableCellRenderer {

      private JLabel dayLabel;

      public DayHeaderTableCellRenderer () {

         dayLabel = new JLabel("", JLabel.CENTER);
         dayLabel.setOpaque(true);
         dayLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, SystemColor.textHighlight.brighter()), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
         dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
         dayLabel.setBackground(SystemColor.textHighlight);
         dayLabel.setForeground(SystemColor.textHighlightText);
      }

      public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

         dayLabel.setText((String)value);

         return dayLabel;
      }

   }

}
