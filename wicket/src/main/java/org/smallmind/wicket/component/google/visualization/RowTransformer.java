package org.smallmind.wicket.component.google.visualization;

import java.util.Date;
import java.util.List;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableCell;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.BooleanValue;
import com.google.visualization.datasource.datatable.value.DateTimeValue;
import com.google.visualization.datasource.datatable.value.DateValue;
import com.google.visualization.datasource.datatable.value.NumberValue;
import com.google.visualization.datasource.datatable.value.TextValue;
import com.google.visualization.datasource.datatable.value.TimeOfDayValue;
import com.google.visualization.datasource.datatable.value.Value;
import com.google.visualization.datasource.datatable.value.ValueType;
import org.joda.time.DateTime;
import org.smallmind.wicket.FormattedWicketRuntimeException;

public class RowTransformer {

   private ValueType[] types;

   public RowTransformer (DataTable dataTable) {

      List<ColumnDescription> columnDescriptionList = dataTable.getColumnDescriptions();
      int index = 0;

      types = new ValueType[columnDescriptionList.size()];
      for (ColumnDescription columnDescription : columnDescriptionList) {
         types[index++] = columnDescription.getType();
      }
   }

   public RowTransformer (ValueType[] types) {

      this.types = types;
   }

   public TableRow transform (Object[] data) {

      if (types.length != data.length) {
         throw new FormattedWicketRuntimeException("The data length(%d) does not match the expected length(%d)", data.length, types.length);
      }

      TableRow tableRow;

      tableRow = new TableRow();
      for (int count = 0; count < data.length; count++) {
         tableRow.addCell(new TableCell(convertValue(types[count], data[count])));
      }

      return tableRow;
   }

   private Value convertValue (ValueType type, Object datum) {

      Class datumClass = datum.getClass();

      if (double.class.equals(datumClass) || (Double.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return new NumberValue((Double)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (float.class.equals(datumClass) || (Float.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return new NumberValue((Float)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (long.class.equals(datumClass) || (Long.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return new NumberValue((Long)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (int.class.equals(datumClass) || (Integer.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return new NumberValue((Integer)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (short.class.equals(datumClass) || (Short.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return new NumberValue((Short)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (byte.class.equals(datumClass) || (Byte.class.equals(datumClass))) {
         switch (type) {
            case NUMBER:
               return new NumberValue((Byte)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (boolean.class.equals(datumClass) || (Boolean.class.equals(datumClass))) {
         switch (type) {
            case BOOLEAN:
               return BooleanValue.getInstance((Boolean)datum);
            case TEXT:
               return new TextValue(String.valueOf(datum));
         }
      }
      if (char.class.equals(datumClass) || (Character.class.equals(datumClass))) {
         if (type.equals(ValueType.TEXT)) {
            return new TextValue(String.valueOf(datum));
         }
      }
      if (datum instanceof String) {
         if (type.equals(ValueType.TEXT)) {
            return new TextValue((String)datum);
         }
      }
      if (datum instanceof Date) {
         switch (type) {
            case NUMBER:
               return new NumberValue(((Date)datum).getTime());
            case DATETIME:

               DateTime dateTime = new DateTime(((Date)datum).getTime());

               return new DateTimeValue(dateTime.getYear(), dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth(), dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute(), dateTime.getMillisOfSecond());
            case DATE:

               DateTime date = new DateTime(((Date)datum).getTime());

               return new DateValue(date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth());
            case TIMEOFDAY:

               DateTime timeOfDay = new DateTime(((Date)datum).getTime());

               return new TimeOfDayValue(timeOfDay.getHourOfDay(), timeOfDay.getMinuteOfHour(), timeOfDay.getSecondOfMinute(), timeOfDay.getMillisOfSecond());
         }
      }
      if (datumClass.isEnum()) {
         if (type.equals(ValueType.TEXT)) {
            return new TextValue(datum.toString());
         }
      }

      throw new FormattedWicketRuntimeException("The data(%s) of class(%s) is not convertible to a value of type(%s)", datum.toString(), datumClass.getName(), type.name());
   }
}
