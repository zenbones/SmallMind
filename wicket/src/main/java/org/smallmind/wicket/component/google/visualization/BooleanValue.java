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

public class BooleanValue extends Value {

   private static BooleanValue NULL_VALUE = new BooleanValue(null);

   private Boolean logic;

   public static BooleanValue asNull () {

      return NULL_VALUE;
   }

   public static BooleanValue create (boolean logic) {

      return new BooleanValue(logic);
   }

   public static BooleanValue create (Boolean logic) {

      return (logic == null) ? NULL_VALUE : new BooleanValue(logic);
   }

   private BooleanValue (Boolean logic) {

      this.logic = logic;
   }

   public Boolean getBoolean () {

      return logic;
   }

   @Override
   public ValueType getType () {

      return ValueType.BOOLEAN;
   }

   @Override
   public boolean isNull () {

      return (logic == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!ValueType.BOOLEAN.equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return logic.compareTo((((BooleanValue)value).getBoolean()));
      }
   }

   @Override
   public String forScript () {

      return toString();
   }

   public String toString () {

      return (logic == null) ? "null" : logic.toString();
   }
}


