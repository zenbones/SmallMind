package org.smallmind.wicket.component.google.visualization;

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class ColumnDescription extends TableElement {

   private String id;
   private ValueType type;
   private String label;

   public ColumnDescription (String id, ValueType type, String label) {

      super();

      this.id = id;
      this.type = type;
      this.label = label;
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

   public TableCell createTableCell (Value value) {

      if (!type.equals(value.getType())) {
         throw new TypeMismatchException("%s != %s", type, value.getType());
      }

      return new TableCell(value);
   }
}
