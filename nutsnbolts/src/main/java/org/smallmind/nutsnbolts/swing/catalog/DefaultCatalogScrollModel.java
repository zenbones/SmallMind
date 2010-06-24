package org.smallmind.nutsnbolts.swing.catalog;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JViewport;

public class DefaultCatalogScrollModel implements CatalogScrollModel {

   private Catalog catalog;

   public DefaultCatalogScrollModel (Catalog catalog) {

      this.catalog = catalog;
   }

   public int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

      Rectangle viewRectangle;
      int index;
      int jiggleJump = 0;

      viewRectangle = ((JViewport)catalog.getParent()).getViewRect();

      if (direction < 0) {
         if (viewRectangle.getY() > 0) {
            index = catalog.getIndexAtPoint(new Point(0, (int)viewRectangle.getY()));
            jiggleJump = (int)(viewRectangle.getY() - catalog.getSquashedRectangleAtIndex(index).getY());
            if (jiggleJump == 0) {
               jiggleJump += catalog.getSquashedRectangleAtIndex(index - 1).getHeight();
            }
         }
      }
      else if (direction > 0) {
         if ((viewRectangle.getY() + viewRectangle.getHeight()) < catalog.getPreferredSize().getHeight()) {
            index = catalog.getIndexAtPoint(new Point(0, (int)(viewRectangle.getY() + viewRectangle.getHeight())));
            jiggleJump = (int)((catalog.getSquashedRectangleAtIndex(index).getY() + catalog.getSquashedRectangleAtIndex(index).getHeight()) - (viewRectangle.getY() + viewRectangle.getHeight()));
            if (jiggleJump == 0) {
               jiggleJump += catalog.getSquashedRectangleAtIndex(index + 1).getHeight();
            }
         }
      }

      return jiggleJump;
   }

   public int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

      Dimension preferredSize;
      Rectangle viewRectangle;

      viewRectangle = ((JViewport)catalog.getParent()).getViewRect();

      if (direction < 0) {
         return (int)viewRectangle.getY();
      }
      else if (direction > 0) {
         preferredSize = catalog.getPreferredSize();

         return (int)(preferredSize.getHeight() - viewRectangle.getY() + viewRectangle.getHeight());
      }

      return 0;
   }

}
