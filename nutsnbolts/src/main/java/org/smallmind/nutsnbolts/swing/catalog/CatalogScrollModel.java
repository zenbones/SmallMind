package org.smallmind.nutsnbolts.swing.catalog;

import java.awt.Rectangle;

public interface CatalogScrollModel {

   public abstract int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction);

   public abstract int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction);

}
