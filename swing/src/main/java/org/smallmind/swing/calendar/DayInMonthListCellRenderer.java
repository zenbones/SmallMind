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
package org.smallmind.swing.calendar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import org.smallmind.nutsnbolts.calendar.CalendarUtilities;
import org.smallmind.swing.ComponentUtilities;

public class DayInMonthListCellRenderer implements ListCellRenderer {

  DayInMonthComboBoxModel model;
  private JPanel renderPanel;
  private JLabel dayOfWeekLabel;
  private JLabel dayLabel;

  public DayInMonthListCellRenderer (DayInMonthComboBoxModel model) {

    GridBagConstraints constraint;

    this.model = model;

    constraint = new GridBagConstraints();

    renderPanel = new JPanel(new GridBagLayout());
    renderPanel.setOpaque(true);

    dayOfWeekLabel = new JLabel();
    dayOfWeekLabel.setFont(dayOfWeekLabel.getFont().deriveFont(Font.PLAIN));

    dayLabel = new JLabel();

    constraint.anchor = GridBagConstraints.WEST;
    constraint.fill = GridBagConstraints.NONE;
    constraint.insets = new Insets(0, 0, 0, 0);
    constraint.gridx = 0;
    constraint.gridy = 0;
    constraint.weightx = 0;
    constraint.weighty = 0;
    renderPanel.add(dayOfWeekLabel, constraint);

    constraint.anchor = GridBagConstraints.WEST;
    constraint.fill = GridBagConstraints.HORIZONTAL;
    constraint.insets = new Insets(0, 0, 0, 0);
    constraint.gridx = 1;
    constraint.gridy = 0;
    constraint.weightx = 1;
    constraint.weighty = 0;
    renderPanel.add(dayLabel, constraint);
  }

  public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

    boolean shady = false;
    int dayOfWeek;

    dayOfWeek = CalendarUtilities.getDayOfWeek(model.getYear(), model.getMonth(), (Integer)value);
    for (int day = 2; day <= (Integer)value; day++) {
      if (CalendarUtilities.getDayOfWeek(model.getYear(), model.getMonth(), day) == 1) {
        shady = !shady;
      }
    }

    dayOfWeekLabel.setText(CalendarUtilities.getDay(dayOfWeek).name().substring(0, 1));
    ComponentUtilities.setPreferredWidth(dayOfWeekLabel, 20);

    dayLabel.setText(value.toString());

    dayOfWeekLabel.setForeground(((dayOfWeek == 1) || (dayOfWeek == 7)) ? Color.RED : (isSelected) ? SystemColor.text : SystemColor.textText);
    dayLabel.setForeground((isSelected) ? SystemColor.text : SystemColor.textText);

    renderPanel.setBackground((isSelected) ? SystemColor.textHighlight : (shady && (index >= 0)) ? SystemColor.controlHighlight : SystemColor.text);
    renderPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, (isSelected) ? SystemColor.textHighlight : (shady && (index >= 0)) ? SystemColor.controlHighlight : SystemColor.text));

    return renderPanel;
  }

}
