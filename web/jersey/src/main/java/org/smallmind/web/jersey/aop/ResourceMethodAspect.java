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
package org.smallmind.web.jersey.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class ResourceMethodAspect {

  @Around(value = "execution(@org.smallmind.web.jersey.aop.ResourceMethod * * (..)) && @annotation(resourceMethod)", argNames = "thisJoinPoint, resourceMethod")
  public Object aroundEntityTypeMethod (ProceedingJoinPoint thisJoinPoint, ResourceMethod resourceMethod)
    throws Throwable {

    Object returnValue;

    if (resourceMethod.validate()) {
      EntityValidator.validateParameters(thisJoinPoint.getTarget(), ((MethodSignature)thisJoinPoint.getSignature()).getMethod(), thisJoinPoint.getArgs());
    }

    returnValue = thisJoinPoint.proceed();

    if (resourceMethod.validate()) {
      EntityValidator.validateReturnValue(thisJoinPoint.getTarget(), ((MethodSignature)thisJoinPoint.getSignature()).getMethod(), returnValue);
    }

    return returnValue;
  }
}
