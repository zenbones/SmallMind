package org.smallmind.nutsnbolts.swing.spinner;

import java.awt.Component;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class DefaultSpinnerRenderer implements SpinnerRenderer {

   private JLabel renderLabel;

   public DefaultSpinnerRenderer () {

      this(JLabel.LEFT);
   }

   public DefaultSpinnerRenderer (int alignment) {

      renderLabel = new JLabel();
      renderLabel.setHorizontalAlignment(alignment);
      renderLabel.setOpaque(true);
      renderLabel.setBackground(SystemColor.text);
      renderLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SystemColor.controlShadow), BorderFactory.createMatteBorder(2, 2, 2, 2, SystemColor.text)));
   }

   public Component getSpinnerRendererComponent (Spinner spinner, Object value) {

      renderLabel.setText(value.toString());

      return renderLabel;
   }

}
