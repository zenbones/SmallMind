package org.smallmind.nutsnbolts.swing.table;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

public class ListTable extends JTable {

   public ListTable (ListTableModel tableModel) {

      super(tableModel);

      setColumnSelectionAllowed(false);
      setRowSelectionAllowed(true);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setTableHeader(null);
   }

}
