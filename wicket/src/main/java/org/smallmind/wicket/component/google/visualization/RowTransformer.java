/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
