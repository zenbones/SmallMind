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

import java.util.HashMap;

public class InstanceLocator {

   private static final HashMap<String, Object> INSTANCE_MAP = new HashMap<String, Object>();

   public static Object instantiate (String packageName, String interfaceName, String name, boolean required)
      throws DotNotationException {

      return instantiate(null, packageName, interfaceName, name, required);
   }

   public static Object instantiate (String className, boolean required)
      throws DotNotationException {

      return instantiate(null, className, required);
   }

   public static Object instantiate (ClassLoader classLoader, String packageName, String interfaceName, String name, boolean required)
      throws DotNotationException {

      String className;

      className = packageName + "." + name + interfaceName;
      return instantiate(classLoader, className, required);
   }

   public static Object instantiate (ClassLoader classLoader, String className, boolean required)
      throws DotNotationException {

      Object instance;

      synchronized (INSTANCE_MAP) {
         if (INSTANCE_MAP.containsKey(className)) {
            return INSTANCE_MAP.get(className);
         }
      }
      try {
         if (classLoader != null) {
            instance = Class.forName(className, true, classLoader).newInstance();
         }
         else {
            instance = Class.forName(className).newInstance();
         }
      }
      catch (ClassNotFoundException classNotFoundException) {
         if (required) {
            throw new DotNotationException(classNotFoundException);
         }
         else {
            instance = null;
         }
      }
      catch (Exception exception) {
         throw new DotNotationException(exception);
      }
      synchronized (INSTANCE_MAP) {
         INSTANCE_MAP.put(className, instance);
      }
      return instance;
   }

}
