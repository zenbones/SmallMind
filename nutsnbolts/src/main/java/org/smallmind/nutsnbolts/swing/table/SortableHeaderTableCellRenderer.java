package org.smallmind.nutsnbolts.swing.table;

import java.awt.Component;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.smallmind.nutsnbolts.swing.LayoutManagerConstructionException;
import org.smallmind.nutsnbolts.swing.label.PlainLabel;

public class SortableHeaderTableCellRenderer<E extends Enum> implements TableCellRenderer {

   private HashMap<Object, SortableHeaderPanel> renderMap;
   private boolean returnToNeutral;
   private boolean showOrder;

   public SortableHeaderTableCellRenderer (boolean returnToNeutral, boolean showOrder) {

      this.returnToNeutral = returnToNeutral;
      this.showOrder = showOrder;

      renderMap = new HashMap<Object, SortableHeaderPanel>();
   }

   public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

      SortableHeaderPanel headerPanel;

      if ((headerPanel = renderMap.get(value)) == null) {
         try {
            headerPanel = new SortableHeaderPanel<E>((SortableTable<E>)table, (E)value, new PlainLabel((String)table.getColumnModel().getColumn(column).getIdentifier(), JLabel.CENTER), returnToNeutral, showOrder);
         }
         catch (LayoutManagerConstructionException l) {
            throw new IllegalStateException(l);
         }

         renderMap.put(value, headerPanel);
      }

      return headerPanel;
   }

}
