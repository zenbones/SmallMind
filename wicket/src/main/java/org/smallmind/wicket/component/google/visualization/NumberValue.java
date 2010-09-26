/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

public class NumberValue extends Value {

   private static NumberValue NULL_VALUE = new NumberValue(null);

   private Double number;

   public static NumberValue asNull () {

      return NULL_VALUE;
   }

   public static NumberValue create (double number) {

      return new NumberValue(number);
   }

   public static NumberValue create (Double number) {

      return (number == null) ? NULL_VALUE : new NumberValue(number);
   }

   private NumberValue (Double number) {

      this.number = number;
   }

   public synchronized Double getNumber () {

      return number;
   }

   public synchronized void add (double number) {

      if (this.number == null) {
         throw new UnsupportedOperationException("Can't manipulate a null value");
      }

      this.number += number;
   }

   @Override
   public ValueType getType () {

      return ValueType.NUMBER;
   }

   @Override
   public boolean isNull () {

      return (number == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!ValueType.NUMBER.equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return number.compareTo((((NumberValue)value).getNumber()));
      }
   }

   @Override
   public String forScript () {

      return toString();
   }

   public String toString () {

      return (number == null) ? "null" : number.toString();
   }
}


