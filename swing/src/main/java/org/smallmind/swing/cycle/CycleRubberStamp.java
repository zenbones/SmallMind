package org.smallmind.swing.cycle;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JComponent;

public class CycleRubberStamp extends JComponent {

   private Cycle cycle;

   public CycleRubberStamp (Cycle cycle) {

      this.cycle = cycle;
   }

   public Component getRenderComponent () {

      return cycle.getRenderComponent();
   }

   public void paint (Graphics graphics) {

      Component renderComponent;

      renderComponent = getRenderComponent();
      renderComponent.setBounds(getBounds());

      renderComponent.paint(graphics);
   }

}
