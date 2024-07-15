/*
 * Copyright (c) 2007 through 2024 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.web.json.scaffold.fault.Fault;

public class SignatureUtility {

  private static final ConcurrentHashMap<String, Class<?>> SIGNATURE_MAP = new ConcurrentHashMap<>();

  public static String neutralEncode (Class<?> clazz) {

    if ((clazz == null) || void.class.equals(clazz) || Void.class.equals(clazz)) {

      return BuiltInType.VOID.getCode();
    } else {

      StringBuilder codeBuilder = new StringBuilder();

      if (clazz.isArray()) {

        int dimensions = 0;

        codeBuilder.append('[');
        while (clazz.isArray()) {
          dimensions++;
          clazz = clazz.getComponentType();
        }

        codeBuilder.append(",".repeat(Math.max(0, (dimensions - 1))));
        codeBuilder.append(']');
      }

      if (clazz.isPrimitive()) {
        if (clazz.equals(boolean.class)) {
          codeBuilder.insert(0, BuiltInType.BOOLEAN.getCode());
        } else if (clazz.equals(byte.class)) {
          codeBuilder.insert(0, BuiltInType.BYTE.getCode());
        } else if (clazz.equals(short.class)) {
          codeBuilder.insert(0, BuiltInType.SHORT.getCode());
        } else if (clazz.equals(int.class)) {
          codeBuilder.insert(0, BuiltInType.INTEGER.getCode());
        } else if (clazz.equals(long.class)) {
          codeBuilder.insert(0, BuiltInType.LONG.getCode());
        } else if (clazz.equals(float.class)) {
          codeBuilder.insert(0, BuiltInType.FLOAT.getCode());
        } else if (clazz.equals(double.class)) {
          codeBuilder.insert(0, BuiltInType.DOUBLE.getCode());
        } else if (clazz.equals(char.class)) {
          codeBuilder.insert(0, BuiltInType.CHARACTER.getCode());
        }
      } else if (clazz.equals(String.class)) {
        codeBuilder.insert(0, BuiltInType.STRING.getCode());
      } else if (clazz.equals(Date.class)) {
        codeBuilder.insert(0, BuiltInType.DATE.getCode());
      } else if (clazz.equals(Fault.class)) {
        codeBuilder.insert(0, BuiltInType.FAULT.getCode());
      } else if (clazz.equals(Object.class)) {
        codeBuilder.insert(0, BuiltInType.OBJECT.getCode());
      } else {
        codeBuilder.insert(0, clazz.getSimpleName()).insert(0, '!');
      }

      return codeBuilder.toString();
    }
  }

  public static String nativeEncode (Class<?> clazz) {

    if ((clazz == null) || void.class.equals(clazz) || Void.class.equals(clazz)) {

      return "V";
    } else {

      StringBuilder codeBuilder = new StringBuilder();

      while (clazz.isArray()) {
        codeBuilder.append("[");
        clazz = clazz.getComponentType();
      }

      if (clazz.isPrimitive()) {
        if (clazz.equals(boolean.class)) {
          codeBuilder.append("Z");
        } else if (clazz.equals(byte.class)) {
          codeBuilder.append("B");
        } else if (clazz.equals(short.class)) {
          codeBuilder.append("S");
        } else if (clazz.equals(int.class)) {
          codeBuilder.append("I");
        } else if (clazz.equals(long.class)) {
          codeBuilder.append("J");
        } else if (clazz.equals(float.class)) {
          codeBuilder.append("F");
        } else if (clazz.equals(double.class)) {
          codeBuilder.append("D");
        } else if (clazz.equals(char.class)) {
          codeBuilder.append("C");
        }
      } else {
        codeBuilder.append('L').append(clazz.getName().replace('.', '/')).append(';');
      }

      return codeBuilder.toString();
    }
  }

  public static Class<?> nativeDecode (String type)
    throws ClassNotFoundException {

    switch (type.charAt(0)) {
      case 'V':
        return Void.class;
      case 'Z':
        return boolean.class;
      case 'B':
        return byte.class;
      case 'C':
        return char.class;
      case 'S':
        return short.class;
      case 'I':
        return int.class;
      case 'J':
        return long.class;
      case 'F':
        return float.class;
      case 'D':
        return (double.class);
      case 'L':
        return getObjectType(type.substring(1, type.length() - 1).replace('/', '.'));
      case '[':
        return getObjectType(type.replace('/', '.'));
      default:
        throw new ClassNotFoundException("Unknown format for parameter signature(" + type + ")");
    }
  }

  private static Class<?> getObjectType (String type)
    throws ClassNotFoundException {

    Class<?> objectType;

    if ((objectType = SIGNATURE_MAP.get(type)) == null) {
      synchronized (SIGNATURE_MAP) {
        if ((objectType = SIGNATURE_MAP.get(type)) == null) {
          SIGNATURE_MAP.put(type, objectType = Class.forName(type));
        }
      }
    }

    return objectType;
  }
}