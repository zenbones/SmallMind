package org.smallmind.swing.spinner;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

public class SpinnerRubberStamp extends JComponent {

   private Spinner spinner;

   public SpinnerRubberStamp (Spinner spinner) {

      this.spinner = spinner;
   }

   public Dimension getPreferredSize () {

      return spinner.getRenderComponent().getPreferredSize();
   }

   public Dimension getMaximumSize () {

      return spinner.getRenderComponent().getMaximumSize();
   }

   public Dimension getMinimumSize () {

      return spinner.getRenderComponent().getMinimumSize();
   }

   public void paint (Graphics graphics) {

      Component renderComponent;

      renderComponent = spinner.getRenderComponent();
      renderComponent.setBounds(getBounds());

      renderComponent.paint(graphics);
   }

}
