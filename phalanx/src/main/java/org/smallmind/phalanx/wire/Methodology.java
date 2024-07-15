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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.phalanx.wire.transport.ArgumentInfo;
import org.smallmind.phalanx.wire.transport.SyntheticArgument;

public class Methodology {

  private final Method method;
  private final HashMap<String, ArgumentInfo> argumentInfoMap = new HashMap<>();

  public Methodology (Class<?> serviceInterface, Method method, SyntheticArgument... syntheticArguments)
    throws ServiceDefinitionException {

    int index = 0;

    this.method = method;

    if ((syntheticArguments != null) && (syntheticArguments.length > 0)) {
      for (SyntheticArgument syntheticArgument : syntheticArguments) {
        argumentInfoMap.put(syntheticArgument.getName(), new ArgumentInfo(index++, syntheticArgument.getParameterType()));
      }
    } else {
      for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
        for (Annotation annotation : parameterAnnotations) {
          if (annotation.annotationType().equals(Argument.class)) {
            argumentInfoMap.put(((Argument)annotation).value(), new ArgumentInfo(index, method.getParameterTypes()[index++]));
            break;
          }
        }
      }
    }

    if (index != method.getParameterTypes().length) {
      throw new ServiceDefinitionException("The method(%s) of service interface(%s) requires @Argument annotations", method.getName(), serviceInterface.getName());
    }
  }

  public Method getMethod () {

    return method;
  }

  public ArgumentInfo getArgumentInfo (String name) {

    return argumentInfoMap.get(name);
  }
}