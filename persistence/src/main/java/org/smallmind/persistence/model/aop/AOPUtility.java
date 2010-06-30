package org.smallmind.persistence.model.aop;

import org.smallmind.persistence.model.bean.BeanAccessException;
import org.smallmind.persistence.model.bean.BeanInvocationException;
import org.smallmind.persistence.model.bean.BeanUtility;
import org.smallmind.persistence.model.reflect.ReflectionUtility;
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
            else if ((expectedType != null) && (!ReflectionUtility.isEssentiallyTheSameAs(expectedType, argumentValue.getClass()))) {
               throw new BeanAccessException("The parameter(%s) must be of the expected type '%s'", baseParameter, expectedType);
            }

            return argumentValue;
         }
      }

      throw new BeanAccessException("The parameter(%s) was not found as part of the method(%s) signature", baseParameter, methodSignature.getName());
   }
}
