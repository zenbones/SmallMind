package org.smallmind.swing.catalog;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class DefaultCatalogCellRenderer implements CatalogCellRenderer {

   public Component getCatalogCellRendererComponent (Catalog catalog, Object value, int index, boolean isSelected, boolean cellHasFocus) {

      JPanel renderPanel;
      JLabel renderLabel;

      renderPanel = new JPanel(new GridLayout(1, 0));
      renderLabel = new JLabel(value.toString());
      renderPanel.add(renderLabel);

      if (isSelected) {
         renderPanel.setBorder(BorderFactory.createLineBorder(SystemColor.textHighlight, 2));
      }
      else {
         renderPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      }

      return renderPanel;
   }

}
