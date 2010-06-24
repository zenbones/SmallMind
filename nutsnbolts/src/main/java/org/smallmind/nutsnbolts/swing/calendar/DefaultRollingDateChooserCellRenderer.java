package org.smallmind.nutsnbolts.swing.calendar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;

public class DefaultRollingDateChooserCellRenderer implements RollingDateChooserCellRenderer {

   private RollingDateChooser rollingDateChooser;
   private JLabel dayLabel;

   public DefaultRollingDateChooserCellRenderer (RollingDateChooser rollingDateChooser) {

      this.rollingDateChooser = rollingDateChooser;

      dayLabel = new JLabel("", JLabel.CENTER);
      dayLabel.setOpaque(true);
   }

   public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

      dayLabel.setText(String.valueOf(((CalendarDate)value).getDay()));

      if (((row == rollingDateChooser.getTodayRow()) && (column == rollingDateChooser.getTodayColumn())) || rollingDateChooser.isMarked((CalendarDate)value)) {
         dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
      }
      else {
         dayLabel.setFont(dayLabel.getFont().deriveFont(Font.PLAIN));
      }

      if ((column < 6) && (((CalendarDate)table.getValueAt(row, column + 1)).getDay() == 1)) {
         dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, SystemColor.controlShadow));
      }
      else if ((row == (table.getRowCount() - 1)) && (((CalendarDate)value).getDay() > ((CalendarDate)table.getValueAt(row, 6)).getDay())) {
         dayLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.controlShadow), BorderFactory.createMatteBorder(0, 0, 0, 1, SystemColor.control)));
      }
      else if ((row < (table.getRowCount() - 1)) && (((CalendarDate)value).getDay() > ((CalendarDate)table.getValueAt(row + 1, column)).getDay())) {
         dayLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.controlShadow), BorderFactory.createMatteBorder(0, 0, 0, 1, SystemColor.control)));
      }
      else {
         dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, SystemColor.control));
      }

      if (isSelected) {
         dayLabel.setBackground(SystemColor.textHighlight);
      }
      else if (rollingDateChooser.isMarked((CalendarDate)value)) {
         dayLabel.setBackground(SystemColor.text);
      }
      else if ((row == rollingDateChooser.getTodayRow()) && (column == rollingDateChooser.getTodayColumn())) {
         dayLabel.setBackground(SystemColor.textHighlight);
      }
      else {
         dayLabel.setBackground(SystemColor.text);
      }

      if (rollingDateChooser.isMarked((CalendarDate)value)) {
         dayLabel.setForeground(Color.RED);
      }
      else if ((row == rollingDateChooser.getTodayRow()) && (column == rollingDateChooser.getTodayColumn())) {
         dayLabel.setForeground((isSelected) ? SystemColor.textHighlightText : SystemColor.GREEN);
      }
      else if ((column == 0) || (column == 6)) {
         dayLabel.setForeground(SystemColor.textInactiveText);
      }
      else if (isSelected) {
         dayLabel.setForeground(SystemColor.textHighlightText);
      }
      else {
         dayLabel.setForeground(SystemColor.textText);
      }

      return dayLabel;
   }

}
