package org.smallmind.throng.wire;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;

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