package org.smallmind.nutsnbolts.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.SystemColor;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

public class Separator extends JComponent {

   public static int HORIZONTAL = SwingConstants.HORIZONTAL;
   public static int VERTICAL = SwingConstants.VERTICAL;

   private int orientation;
   private int length;

   public Separator () {

      this(0, HORIZONTAL);
   }

   public Separator (int length, int orientation) {

      this.length = length;
      this.orientation = orientation;

      setFocusable(false);

      setForeground(SystemColor.controlShadow);
      setBackground(SystemColor.controlLtHighlight);
   }

   public int getOrientation () {

      return orientation;
   }

   public void setOrientation (int orientation) {

      if (this.orientation != orientation) {
         this.orientation = orientation;
         revalidate();
         repaint();
      }
   }

   public Dimension getPreferredSize () {

      if (orientation == VERTICAL) {
         return new Dimension(2, length);
      }
      else {
         return new Dimension(length, 2);
      }
   }

   public void paint (Graphics graphics) {

      Dimension currentSize = getSize();

      if (orientation == VERTICAL) {
         graphics.setColor(getForeground());
         graphics.drawLine(0, 0, 0, currentSize.height);

         graphics.setColor(getBackground());
         graphics.drawLine(1, 0, 1, currentSize.height);
      }
      else {
         graphics.setColor(getForeground());
         graphics.drawLine(0, 0, currentSize.width, 0);

         graphics.setColor(getBackground());
         graphics.drawLine(0, 1, currentSize.width, 1);
      }
   }

}
