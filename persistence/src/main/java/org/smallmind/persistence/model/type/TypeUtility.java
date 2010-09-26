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
package org.smallmind.persistence.model.type;

import java.util.Date;

public class TypeUtility {

   private static final Class[] PRIMITIVE_EQUIVALENTS = new Class[] {Date.class, String.class, Long.class, Boolean.class, Integer.class, Double.class, Float.class, Character.class, Short.class, Byte.class};
/*
  public static MetaType getMetaType (Type type) {

    System.out.println("..........3:" + field.getName() + ":" + field.getGenericType());
    if (type instanceof Class) {
      Class currentClass = (Class)field.getGenericType();

      if (currentClass.isPrimitive()) {
        if (currentClass.equals(long.class)) {
          return Long.class;
        }
        if (currentClass.equals(char.class)) {
          return Character.class;
        }
        if (currentClass.equals(int.class)) {
          return Integer.class;
        }
        if (currentClass.equals(byte.class)) {
          return Byte.class;
        }
        if (currentClass.equals(short.class)) {
          return Short.class;
        }
        if (currentClass.equals(float.class)) {
          return Float.class;
        }
        if (currentClass.equals(double.class)) {
          return Double.class;
        }
        if (currentClass.equals(boolean.class)) {
          return Boolean.class;
        }
      }
      else if (currentClass.isEnum()) {
        return Enum.class;
      }

      for (Class primitiveClass : PRIMITIVE_EQUIVALENTS) {
        if (currentClass.equals(primitiveClass)) {

          return primitiveClass;
        }
      }
    }

    if (field.getGenericType() instanceof TypeVariable) {
      for (Type ft : ((TypeVariable)field.getGenericType()).getBounds()) {
        System.out.println("!!!!!!!:" + ft.getClass() + ":" + ft);
      }
    }

    return null;
  }
  */
}
