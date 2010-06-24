package org.smallmind.nutsnbolts.swing.catalog;

import java.awt.Component;

public interface CatalogCellRenderer {

   public abstract Component getCatalogCellRendererComponent (Catalog catalog, Object value, int index, boolean isSelected, boolean cellHasFocus);

}
