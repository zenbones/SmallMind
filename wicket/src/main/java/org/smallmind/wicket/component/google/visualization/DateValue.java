/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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
import org.joda.time.DateTime;

public class DateValue extends TimeBasedValue {

  private static DateValue NULL_VALUE = new DateValue(null);

  public static DateValue asNull () {

    return NULL_VALUE;
  }

  public static DateValue create (long milliseconds) {

    return new DateValue(new DateTime(milliseconds));
  }

  public static DateValue create (Long milliseconds) {

    return (milliseconds == null) ? NULL_VALUE : new DateValue(new DateTime(milliseconds));
  }

  public static DateValue create (Date date) {

    return (date == null) ? NULL_VALUE : new DateValue(new DateTime(date));
  }

  private DateValue (DateTime instant) {

    super(instant);
  }

  @Override
  public ValueType getType () {

    return ValueType.DATE;
  }

  public int getYear () {

    return getInstant().getYear();
  }

  public int getMonth () {

    return getInstant().getMonthOfYear() - 1;
  }

  public int getDay () {

    return getInstant().getDayOfMonth();
  }

  public String forScript () {

    if (getInstant() == null) {

      return "null";
    }

    StringBuilder dateBuilder = new StringBuilder("new Date(");

    dateBuilder.append(getYear()).append(',').append(getMonth()).append(',').append(getDay()).append(",0,0,0,0)");

    return dateBuilder.toString();
  }
}


