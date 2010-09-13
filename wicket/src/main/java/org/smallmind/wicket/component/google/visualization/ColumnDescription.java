package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class ColumnDescription extends TableElement {

   private CellFormatter cellFormatter;
   private ValueType type;
   private String id;
   private String label;

   public ColumnDescription (String id, ValueType type, String label) {

      this(id, type, label, null);
   }

   public ColumnDescription (String id, ValueType type, String label, CellFormatter cellFormatter) {

      super();

      this.id = id;
      this.type = type;
      this.label = label;
      this.cellFormatter = cellFormatter;
   }

   public String getId () {

      return id;
   }

   public ValueType getType () {

      return type;
   }

   public String getLabel () {

      return label;
   }

   public CellFormatter getCellFormatter () {

      return cellFormatter;
   }

   public TableCell createTableCell (Value value) {

      return createTableCell(value, null);
   }

   public TableCell createTableCell (Value value, String formattedValue) {

      if (!type.equals(value.getType())) {
         throw new TypeMismatchException("%s != %s", type, value.getType());
      }

      return new TableCell(value, ((formattedValue == null) && (cellFormatter != null)) ? cellFormatter.format(value) : formattedValue);
   }
}
