package org.smallmind.wicket.component.google.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.smallmind.nutsnbolts.util.ArrayIterator;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class DataTable extends TableElement {

   private ColumnDescription[] columnDescriptions;
   private ArrayList<TableRow> rows;

   public DataTable (List<ColumnDescription> columnDescriptionList) {

      this(columnDescriptionList.toArray(new ColumnDescription[columnDescriptionList.size()]));
   }

   public DataTable (ColumnDescription[] columnDescriptions) {

      this.columnDescriptions = columnDescriptions;

      rows = new ArrayList<TableRow>();
   }

   public int getColumnCount () {

      return columnDescriptions.length;
   }

   public ColumnDescription getColumnDescription (int index) {

      return columnDescriptions[index];
   }

   public Iterable<ColumnDescription> getColumnDescriptions () {

      return new ArrayIterator<ColumnDescription>(columnDescriptions);
   }

   public synchronized TableRow createTableRow () {

      TableRow tableRow = new TableRow(this, new TableCell[columnDescriptions.length]);

      rows.add(tableRow);

      return tableRow;
   }

   public synchronized Iterable<TableRow> getRows () {

      return new IterableIterator<TableRow>(Collections.unmodifiableList(rows).iterator());
   }

   public synchronized TableRow getRow (int index) {

      return rows.get(index);
   }

   public synchronized TableCell getCell (int rowIndex, int cellIndex) {

      return rows.get(rowIndex).getCell(cellIndex);
   }

   public synchronized Value getValue (int rowIndex, int cellIndex) {

      return rows.get(rowIndex).getCell(cellIndex).getValue();
   }
}
