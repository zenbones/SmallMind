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
package org.smallmind.cloud.transport;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

public class FauxMethod implements Serializable {

   private Class[] signature;
   private Class returnType;
   private String name;

   public FauxMethod (Method method) {

      name = method.getName();
      returnType = method.getReturnType();
      signature = method.getParameterTypes();
   }

   public String getName () {
      return name;
   }

   public Class getReturnType () {
      return returnType;
   }

   public Class[] getSignature () {
      return signature;
   }

   public int hashCode () {

      int hashCode;

      hashCode = name.hashCode();
      hashCode = hashCode ^ returnType.getName().hashCode();

      for (Class parameterType : signature) {
         hashCode = hashCode ^ parameterType.getName().hashCode();
      }

      return hashCode;
   }

   public boolean equals (Object obj) {

      return (obj instanceof FauxMethod) && name.equals(((FauxMethod)obj).getName()) && returnType.equals(((FauxMethod)obj).getReturnType()) && Arrays.equals(signature, ((FauxMethod)obj).getSignature());
   }
}
