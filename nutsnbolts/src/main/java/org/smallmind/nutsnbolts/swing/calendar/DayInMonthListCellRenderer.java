package org.smallmind.nutsnbolts.swing.calendar;

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
import org.smallmind.nutsnbolts.swing.ComponentUtilities;
import org.smallmind.nutsnbolts.swing.LayoutManagerConstructionException;
import org.smallmind.nutsnbolts.swing.LayoutManagerFactory;
import org.smallmind.nutsnbolts.calendar.CalendarUtilities;

public class DayInMonthListCellRenderer implements ListCellRenderer {

   DayInMonthComboBoxModel model;
   private JPanel renderPanel;
   private JLabel dayOfWeekLabel;
   private JLabel dayLabel;

   public DayInMonthListCellRenderer (DayInMonthComboBoxModel model)
      throws LayoutManagerConstructionException {

      GridBagConstraints constraint;

      this.model = model;

      constraint = new GridBagConstraints();

      renderPanel = new JPanel(LayoutManagerFactory.getLayoutManager(GridBagLayout.class));
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
