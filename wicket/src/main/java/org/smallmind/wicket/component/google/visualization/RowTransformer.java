package org.smallmind.wicket.component.google.visualization;

import java.util.Date;
import org.smallmind.wicket.FormattedWicketRuntimeException;

public class RowTransformer {

   private DataTable dataTable;

   public RowTransformer (DataTable dataTable) {

      this.dataTable = dataTable;
   }

   public void transform (TableRow tableRow, Object[] data) {

      if (dataTable.getColumnCount() != data.length) {
         throw new FormattedWicketRuntimeException("The data length(%d) does not match the expected length(%d)", data.length, dataTable.getColumnCount());
      }

      for (int count = 0; count < data.length; count++) {
         tableRow.addCell(dataTable.getColumnDescription(count).createTableCell(convertValue(dataTable.getColumnDescription(count).getType(), data[count])));
      }
   }

   private Value convertValue (ValueType type, Object datum) {

      if (datum == null) {

         return type.asNull();
      }

      Class datumClass = datum.getClass();

      if (double.class.equals(datumClass) || (Double.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return NumberValue.create((Double)datum);
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (float.class.equals(datumClass) || (Float.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return NumberValue.create(((Float)datum).doubleValue());
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (long.class.equals(datumClass) || (Long.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return NumberValue.create(((Long)datum).doubleValue());
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (int.class.equals(datumClass) || (Integer.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return NumberValue.create(((Integer)datum).doubleValue());
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (short.class.equals(datumClass) || (Short.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return NumberValue.create(((Short)datum).doubleValue());
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (byte.class.equals(datumClass) || (Byte.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return NumberValue.create(((Byte)datum).doubleValue());
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (boolean.class.equals(datumClass) || (Boolean.class.equals(datumClass))) {
         switch (type) {
            case BOOLEAN:
               return BooleanValue.create((Boolean)datum);
            case TEXT:
               return TextValue.create(String.valueOf(datum));
         }
      }
      if (char.class.equals(datumClass) || (Character.class.equals(datumClass))) {
         if (type.equals(ValueType.TEXT)) {
            return TextValue.create(String.valueOf(datum));
         }
      }
      if (datum instanceof String) {
         if (type.equals(ValueType.TEXT)) {
            return TextValue.create((String)datum);
         }
      }
      if (datum instanceof Date) {
         switch (type) {
            case NUMBER:
               return NumberValue.create((double)((Date)datum).getTime());
            case DATETIME:
               return DateTimeValue.create((Date)datum);
            case DATE:
               return DateValue.create((Date)datum);
            case TIMEOFDAY:
               return TimeOfDayValue.create((Date)datum);
         }
      }
      if (datumClass.isEnum()) {
         if (type.equals(ValueType.TEXT)) {
            return TextValue.create(datum.toString());
         }
      }

      throw new FormattedWicketRuntimeException("The data(%s) of class(%s) is not convertible to a value of type(%s)", datum.toString(), datumClass.getName(), type.name());
   }
}
