package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;
import org.smallmind.nutsnbolts.util.ArrayIterator;

public class TableRow extends TableElement {

   private DataTable dataTable;
   private TableCell[] cells;
   private int index = 0;

   protected TableRow (DataTable dataTable, TableCell[] cells) {

      this.dataTable = dataTable;
      this.cells = cells;
   }

   public int getColumnCount () {

      return dataTable.getColumnCount();
   }

   public synchronized Iterable<TableCell> getCells () {

      return new ArrayIterator<TableCell>(cells);
   }

   public synchronized TableCell getCell (int index) {

      return cells[index];
   }

   public synchronized Value getValue (int index) {

      return getCell(index).getValue();
   }

   public synchronized void addCell (TableCell tableCell) {

      if (!dataTable.getColumnDescription(index).getType().equals(tableCell.getType())) {
         throw new TypeMismatchException("%s != %s", dataTable.getColumnDescription(index).getType(), tableCell.getType());
      }

      cells[index++] = tableCell;
   }
}
