/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;

public class DefaultRollingDateChooserCellRenderer implements RollingDateChooserCellRenderer {

  private final RollingDateChooser rollingDateChooser;
  private final JLabel dayLabel;

  public DefaultRollingDateChooserCellRenderer (RollingDateChooser rollingDateChooser) {

    this.rollingDateChooser = rollingDateChooser;

    dayLabel = new JLabel("", JLabel.CENTER);
    dayLabel.setOpaque(true);
  }

  public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

    dayLabel.setText(String.valueOf(((CalendarDate)value).getDay()));

    if (((row == rollingDateChooser.getTodayRow()) && (column == rollingDateChooser.getTodayColumn())) || rollingDateChooser.isMarked((CalendarDate)value)) {
      dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
    } else {
      dayLabel.setFont(dayLabel.getFont().deriveFont(Font.PLAIN));
    }

    if ((column < 6) && (((CalendarDate)table.getValueAt(row, column + 1)).getDay() == 1)) {
      dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, SystemColor.controlShadow));
    } else if ((row == (table.getRowCount() - 1)) && (((CalendarDate)value).getDay() > ((CalendarDate)table.getValueAt(row, 6)).getDay())) {
      dayLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.controlShadow), BorderFactory.createMatteBorder(0, 0, 0, 1, SystemColor.control)));
    } else if ((row < (table.getRowCount() - 1)) && (((CalendarDate)value).getDay() > ((CalendarDate)table.getValueAt(row + 1, column)).getDay())) {
      dayLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.controlShadow), BorderFactory.createMatteBorder(0, 0, 0, 1, SystemColor.control)));
    } else {
      dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, SystemColor.control));
    }

    if (isSelected) {
      dayLabel.setBackground(SystemColor.textHighlight);
    } else if (rollingDateChooser.isMarked((CalendarDate)value)) {
      dayLabel.setBackground(SystemColor.text);
    } else if ((row == rollingDateChooser.getTodayRow()) && (column == rollingDateChooser.getTodayColumn())) {
      dayLabel.setBackground(SystemColor.textHighlight);
    } else {
      dayLabel.setBackground(SystemColor.text);
    }

    if (rollingDateChooser.isMarked((CalendarDate)value)) {
      dayLabel.setForeground(Color.RED);
    } else if ((row == rollingDateChooser.getTodayRow()) && (column == rollingDateChooser.getTodayColumn())) {
      dayLabel.setForeground((isSelected) ? SystemColor.textHighlightText : SystemColor.GREEN);
    } else if ((column == 0) || (column == 6)) {
      dayLabel.setForeground(SystemColor.textInactiveText);
    } else if (isSelected) {
      dayLabel.setForeground(SystemColor.textHighlightText);
    } else {
      dayLabel.setForeground(SystemColor.textText);
    }

    return dayLabel;
  }
}
