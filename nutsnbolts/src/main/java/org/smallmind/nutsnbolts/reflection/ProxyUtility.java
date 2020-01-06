/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;

public class ProxyUtility {

  private static final HashMap<String, Method> METHOD_MAP = new HashMap<String, Method>();
  private static final HashMap<String, Class> SIGNATURE_MAP = new HashMap<String, Class>();

  public static Object invoke (Object proxy, InvocationHandler invocationHandler, boolean isSubclass, String methodCode, String methodName, String resultSignature, String[] signatures, Object... args)
    throws Throwable {

    Method proxyMethod;

    if ((proxyMethod = METHOD_MAP.get(methodCode)) == null) {
      synchronized (METHOD_MAP) {
        if ((proxyMethod = METHOD_MAP.get(methodCode)) == null) {

          Class methodContainer = (isSubclass) ? proxy.getClass().getSuperclass() : proxy.getClass();

          METHOD_MAP.put(methodCode, proxyMethod = methodContainer.getMethod(methodName, assembleSignature(signatures)));
        }
      }
    }

    if (invocationHandler == null) {
      switch (resultSignature.charAt(0)) {
        case 'V':
          return null;
        case 'Z':
          return false;
        case 'B':
          return 0;
        case 'C':
          return (char)0;
        case 'S':
          return 0;
        case 'I':
          return 0;
        case 'J':
          return 0L;
        case 'F':
          return 0.0F;
        case 'D':
          return 0.0D;
        case 'L':
          return null;
        case '[':
          return null;
        default:
          throw new ByteCodeManipulationException("Unknown format for result signature(%s)", resultSignature);
      }
    }

    return invocationHandler.invoke(proxy, proxyMethod, args);
  }

  private static Class[] assembleSignature (String[] signatures) {

    Class[] parsedSignature;
    LinkedList<Class> parsedList;

    parsedList = new LinkedList<>();
    for (String signature : signatures) {
      switch (signature.charAt(0)) {
        case 'Z':
          parsedList.add(boolean.class);
          break;
        case 'B':
          parsedList.add(byte.class);
          break;
        case 'C':
          parsedList.add(char.class);
          break;
        case 'S':
          parsedList.add(short.class);
          break;
        case 'I':
          parsedList.add(int.class);
          break;
        case 'J':
          parsedList.add(long.class);
          break;
        case 'F':
          parsedList.add(float.class);
          break;
        case 'D':
          parsedList.add(double.class);
          break;
        case 'L':
          parsedList.add(getObjectType(signature.substring(1, signature.length() - 1).replace('/', '.')));
          break;
        case '[':
          parsedList.add(getObjectType(signature.replace('/', '.')));
          break;
        default:
          throw new ByteCodeManipulationException("Unknown format for parameter signature(%s)", signature);
      }
    }

    parsedSignature = new Class[parsedList.size()];
    parsedList.toArray(parsedSignature);

    return parsedSignature;
  }

  private static Class getObjectType (String type) {

    Class objectType;

    if ((objectType = SIGNATURE_MAP.get(type)) == null) {
      synchronized (SIGNATURE_MAP) {
        if ((objectType = SIGNATURE_MAP.get(type)) == null) {
          try {
            SIGNATURE_MAP.put(type, objectType = Class.forName(type));
          } catch (ClassNotFoundException classNotFoundException) {
            throw new ByteCodeManipulationException(classNotFoundException);
          }
        }
      }
    }

    return objectType;
  }
}
