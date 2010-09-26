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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Setter {

   private Class attributeClass;
   private Method method;
   private String attributeName;

   public Setter (Method method)
      throws ReflectionContractException {

      this.method = method;

      if (method.getReturnType() != Void.class) {
         throw new ReflectionContractException("Setter for attribute (%s) must return void", attributeName);
      }

      if (!(method.getName().startsWith("set") && (method.getName().length() > 3) && Character.isUpperCase(method.getName().charAt(3)))) {
         throw new ReflectionContractException("The declared name of a setter method must start with 'set' followed by a camel case attribute name");
      }
      attributeName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);

      if (method.getParameterTypes().length != 1) {
         throw new ReflectionContractException("Setter for attribute (%s) must declare a single parameter", attributeName);
      }
      attributeClass = method.getParameterTypes()[0];
   }

   public String getAttributeName () {

      return attributeName;
   }

   public Class getAttributeClass () {

      return attributeClass;
   }

   public Object invoke (Object target, Object value)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

      Object[] parameter = {value};

      return method.invoke(target, parameter);
   }

}
