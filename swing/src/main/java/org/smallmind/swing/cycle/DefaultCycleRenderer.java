package org.smallmind.swing.cycle;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class DefaultCycleRenderer implements CycleRenderer {

   private JLabel renderLabel;

   public DefaultCycleRenderer () {

      renderLabel = new JLabel();
      renderLabel.setOpaque(true);
   }

   public Component getCycleRendererComponent (Cycle cycle, Object value, int index, boolean selected) {

      renderLabel.setText(value.toString());
      renderLabel.setBackground((selected) ? cycle.getSelectedBackgroundColor() : cycle.getUnselectedBackgroundColor());
      renderLabel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 0, (selected) ? cycle.getSelectedBackgroundColor() : cycle.getUnselectedBackgroundColor()));
      renderLabel.setForeground((selected) ? cycle.getSelectedForegroundColor() : cycle.getUnselectedForegroundColor());

      return renderLabel;
   }

}
