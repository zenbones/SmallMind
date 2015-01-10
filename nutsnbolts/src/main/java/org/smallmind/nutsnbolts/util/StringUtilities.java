/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.nutsnbolts.util;

public class StringUtilities {

  public static String toDisplayCase (String anyCase) {

    return toDisplayCase(anyCase, '\0');
  }

  public static String toDisplayCase (String anyCase, char wordMarker) {

    StringBuilder displayBuilder;
    boolean newWord = true;

    displayBuilder = new StringBuilder();

    for (int count = 0; count < anyCase.length(); count++) {
      if (anyCase.charAt(count) == wordMarker) {
        newWord = true;
        displayBuilder.append(" ");
      }
      else if (newWord) {
        displayBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
        newWord = false;
      }
      else {
        displayBuilder.append(Character.toLowerCase(anyCase.charAt(count)));
      }
    }

    return displayBuilder.toString();
  }

  public static String toCamelCase (String anyCase, char wordMarker) {

    return toCamelCase(anyCase, wordMarker, true);
  }

  public static String toCamelCase (String anyCase, char wordMarker, boolean startUpper) {

    StringBuilder camelBuilder;
    boolean upper = startUpper;

    camelBuilder = new StringBuilder();

    for (int count = 0; count < anyCase.length(); count++) {
      if (anyCase.charAt(count) == wordMarker) {
        upper = true;
      }
      else if (upper) {
        camelBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
        upper = false;
      }
      else {
        camelBuilder.append(Character.toLowerCase(anyCase.charAt(count)));
      }
    }

    return camelBuilder.toString();
  }

  public static String toStaticFieldName (String anyCase, char wordMarker) {

    StringBuilder fieldBuilder;

    fieldBuilder = new StringBuilder();

    for (int count = 0; count < anyCase.length(); count++) {
      if (anyCase.charAt(count) == wordMarker) {
        fieldBuilder.append('_');
      }
      else {
        fieldBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
      }
    }

    return fieldBuilder.toString();
  }

  public static boolean isJavaIdentifier (String anyName) {

    for (int count = 0; count < anyName.length(); count++) {
      if (count == 0) {
        if (!Character.isJavaIdentifierStart(anyName.charAt(count))) {
          return false;
        }
      }
      else if (!Character.isJavaIdentifierPart(anyName.charAt(count))) {
        return false;
      }
    }

    return true;
  }

}
