package org.smallmind.nutsnbolts.swing.catalog;

import java.awt.Rectangle;

public class SmoothCatalogScrollModel implements CatalogScrollModel {

   private int unit;
   private int block;

   public SmoothCatalogScrollModel (int unit, int block) {

      this.unit = unit;
      this.block = block;
   }

   public int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

      return unit;
   }

   public int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

      return block;
   }

}
