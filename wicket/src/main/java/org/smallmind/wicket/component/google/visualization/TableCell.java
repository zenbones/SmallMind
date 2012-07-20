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

import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public class TableCell extends TableElement {

  private Value value;
  private String formattedValue;

  protected TableCell (Value value) {

    this(value, null);
  }

  protected TableCell (Value value, String formattedValue) {

    if (value == null) {
      throw new IllegalArgumentException("The value must not be null");
    }

    this.value = value;
    this.formattedValue = formattedValue;
  }

  public synchronized ValueType getType () {

    return value.getType();
  }

  public synchronized void setValue (Value value) {

    setValue(value, null);
  }

  public synchronized void setValue (Value value, String formattedValue) {

    if (!this.value.getType().equals(value.getType())) {
      throw new TypeMismatchException("%s != %s", this.value.getType(), value.getType());
    }

    this.value = value;
    this.formattedValue = formattedValue;
  }

  public synchronized Value getValue () {

    return value;
  }

  public synchronized String getFormattedValue () {

    return formattedValue;
  }
}
