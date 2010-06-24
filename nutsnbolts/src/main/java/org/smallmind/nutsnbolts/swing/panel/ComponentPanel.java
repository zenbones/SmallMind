package org.smallmind.nutsnbolts.swing.panel;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

public class ComponentPanel extends JPanel implements Scrollable {

   public ComponentPanel (LayoutManager layoutManager) {

      super(layoutManager);
   }

   public synchronized int getIndexAtPoint (Point point) {

      int height = 0;

      for (int count = 0; count < getComponentCount(); count++) {
         height += getComponent(count).getPreferredSize().getHeight();
         if (point.getY() <= height) {
            return count;
         }
      }

      return -1;
   }

   public synchronized Rectangle getSquashedRectangleAtIndex (int index) {

      int yPos = 0;

      for (int count = 0; count < index; count++) {
         yPos += getComponent(count).getPreferredSize().getHeight();
      }

      return new Rectangle(0, yPos, 0, (int)getComponent(index).getPreferredSize().getHeight());
   }

   public Dimension getPreferredScrollableViewportSize () {

      return getPreferredSize();
   }

   public boolean getScrollableTracksViewportWidth () {

      return true;
   }

   public boolean getScrollableTracksViewportHeight () {

      return false;
   }

   public int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

      Rectangle viewRectangle;
      int index;
      int jiggleJump = 0;

      viewRectangle = ((JViewport)getParent()).getViewRect();

      if (direction < 0) {
         if (viewRectangle.getY() > 0) {
            index = getIndexAtPoint(new Point(0, (int)viewRectangle.getY()));
            jiggleJump = (int)(viewRectangle.getY() - getSquashedRectangleAtIndex(index).getY());
            if (jiggleJump == 0) {
               jiggleJump += getSquashedRectangleAtIndex(index - 1).getHeight();
            }
         }
      }
      else if (direction > 0) {
         if ((viewRectangle.getY() + viewRectangle.getHeight()) < getPreferredSize().getHeight()) {
            index = getIndexAtPoint(new Point(0, (int)(viewRectangle.getY() + viewRectangle.getHeight())));
            jiggleJump = (int)((getSquashedRectangleAtIndex(index).getY() + getSquashedRectangleAtIndex(index).getHeight()) - (viewRectangle.getY() + viewRectangle.getHeight()));
            if (jiggleJump == 0) {
               if (index >= (getComponentCount() - 1)) {
                  jiggleJump = 0;
               }
               else {
                  jiggleJump += getSquashedRectangleAtIndex(index + 1).getHeight();
               }
            }
         }
      }

      return jiggleJump;
   }

   public int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

      Dimension preferredSize;
      Rectangle viewRectangle;

      viewRectangle = ((JViewport)getParent()).getViewRect();

      if (direction < 0) {
         return (int)viewRectangle.getY();
      }
      else if (direction > 0) {
         preferredSize = getPreferredSize();

         return (int)(preferredSize.getHeight() - viewRectangle.getY() + viewRectangle.getHeight());
      }

      return 0;
   }

}
