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

public class TextValue extends Value {

   private static TextValue NULL_VALUE = new TextValue(null);

   private String text;

   public static TextValue asNull () {

      return NULL_VALUE;
   }

   public static TextValue create (String text) {

      return (text == null) ? NULL_VALUE : new TextValue(text);
   }

   private TextValue (String text) {

      this.text = text;
   }

   public String getText () {

      return text;
   }

   @Override
   public ValueType getType () {

      return ValueType.TEXT;
   }

   @Override
   public boolean isNull () {

      return (text == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!ValueType.TEXT.equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return text.compareTo((((TextValue)value).getText()));
      }
   }

   public String forScript () {

      return (text == null) ? "null" : '\'' + text + '\'';
   }

   public String toString () {

      return (text == null) ? "null" : text;
   }
}


