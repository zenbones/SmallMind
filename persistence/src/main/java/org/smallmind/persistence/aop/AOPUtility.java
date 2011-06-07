/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.aop;

import org.smallmind.nutsnbolts.reflection.bean.BeanInvocationException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class AOPUtility {

   public static Object getParameterValue (JoinPoint joinPoint, String parameterName)
      throws BeanAccessException, BeanInvocationException {

      return getParameterValue(joinPoint, null, parameterName);
   }

   public static Object getParameterValue (JoinPoint joinPoint, Class expectedType, String parameterName)
      throws BeanAccessException, BeanInvocationException {

      Object argumentValue;
      MethodSignature methodSignature = ((MethodSignature)joinPoint.getSignature());
      String[] parameterNames;
      String baseParameter;
      String parameterGetter = null;
      int dotPos;

      if ((dotPos = parameterName.indexOf('.')) < 0) {
         baseParameter = parameterName;
      }
      else {
         baseParameter = parameterName.substring(0, dotPos);
         parameterGetter = parameterName.substring(dotPos + 1);
      }

      parameterNames = methodSignature.getParameterNames();
      for (int index = 0; index < parameterNames.length; index++) {
         if (parameterNames[index].equals(baseParameter)) {
            argumentValue = (parameterGetter == null) ? joinPoint.getArgs()[index] : BeanUtility.executeGet(joinPoint.getArgs()[index], parameterGetter);

            if (argumentValue == null) {
               if ((expectedType != null) && expectedType.isPrimitive()) {
                  throw new BeanAccessException("A 'null' parameter can't be assigned to the primitive expected type '%s'", expectedType);
               }

               return null;
            }
            else if ((expectedType != null) && (!TypeUtility.isEssentiallyTheSameAs(expectedType, argumentValue.getClass()))) {
               throw new BeanAccessException("The parameter(%s) must be of the expected type '%s'", baseParameter, expectedType);
            }

            return argumentValue;
         }
      }

      throw new BeanAccessException("The parameter(%s) was not found as part of the method(%s) signature", baseParameter, methodSignature.getName());
   }
}
