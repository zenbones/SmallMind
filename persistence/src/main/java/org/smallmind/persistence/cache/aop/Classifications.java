package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Annotation;
import org.smallmind.persistence.model.aop.AOPUtility;
import org.aspectj.lang.JoinPoint;

public class Classifications {

   public static String get (Class<? extends Annotation> annotationType, JoinPoint joinPoint, Vector vector) {

      if (!vector.asParameter()) {

         return vector.classifier();
      }
      else {
         if (!annotationType.equals(CacheAs.class)) {
            throw new CacheAutomationError("Parameter based classifiers can only be used to annotate method executions (@CacheAs)");
         }

         try {
            return AOPUtility.getParameterValue(joinPoint, vector.classifier()).toString();
         }
         catch (Exception exception) {
            throw new CacheAutomationError(exception);
         }
      }
   }
}