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
package org.smallmind.nutsnbolts.util;

import java.util.Comparator;

public class AlphaNumericComparator<T> implements Comparator<T> {

   private AlphaNumericConverter<T> alphaNumConverter;

   public AlphaNumericComparator () {

      this(new DefaultAlphaNumConverter<T>());
   }

   public AlphaNumericComparator (AlphaNumericConverter<T> alphaNumConverter) {

      this.alphaNumConverter = alphaNumConverter;
   }

   public int compare (T obj1, T obj2) {

      String first;
      String second;
      char firstChar;
      char secondChar;
      int minLength;
      int firstDigit;
      int secondDigit;
      int firstValue;
      int secondValue;
      int count;

      if ((obj1 == null) && (obj2 == null)) {
         return 0;
      }
      else if (obj1 == null) {
         return -1;
      }
      else if (obj2 == null) {
         return 1;
      }

      first = alphaNumConverter.toString(obj1);
      second = alphaNumConverter.toString(obj2);

      if ((first.length() == 0) && (second.length() == 0)) {
         return 0;
      }
      else if (first.length() == 0) {
         return -1;
      }
      else if (second.length() == 0) {
         return 1;
      }

      minLength = Math.min(first.length(), second.length());

      for (count = 0; count < minLength; count++) {
         firstChar = first.charAt(count);
         secondChar = second.charAt(count);
         if ((Character.isDigit(firstChar)) && (!Character.isDigit(secondChar))) {
            return -1;
         }
         else if ((!Character.isDigit(firstChar)) && (Character.isDigit(secondChar))) {
            return 1;
         }
         else if ((Character.isDigit(firstChar)) && (Character.isDigit(secondChar))) {
            firstDigit = Integer.parseInt(first.substring(count, count + 1));
            secondDigit = Integer.parseInt(second.substring(count, count + 1));
            if (firstDigit < secondDigit) {
               return -1;
            }
            else if (firstDigit > secondDigit) {
               return 1;
            }
         }
         else {
            firstValue = (int)Character.toLowerCase(firstChar);
            secondValue = (int)Character.toLowerCase(secondChar);
            if (firstValue < secondValue) {
               return -1;
            }
            else if (firstValue > secondValue) {
               return 1;
            }
         }
      }
      if (first.length() < second.length()) {
         return -1;
      }
      else if (first.length() > second.length()) {
         return 1;
      }
      return 0;
   }
}