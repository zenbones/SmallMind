package org.smallmind.throng.wire;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class MethodInvoker {

  private static final Class[] EMPTY_SIGNATURE = new Class[0];
  private static final Class[] OBJECT_SIGNATURE = {Object.class};
  private final HashMap<Function, Methodology> methodMap;
  private final Class<?> serviceInterface;
  private final Object targetObject;

  public MethodInvoker (Object targetObject, Class<?> serviceInterface)
    throws NoSuchMethodException, ServiceDefinitionException {

    Class<?> endpointClass;
    Method toStringMethod;
    Method hashCodeMethod;
    Method equalsMethod;

    this.targetObject = targetObject;
    this.serviceInterface = serviceInterface;

    methodMap = new HashMap<>();
    for (Method method : serviceInterface.getMethods()) {
      methodMap.put(new Function(method), new Methodology(serviceInterface, method));
    }

    endpointClass = targetObject.getClass();

    toStringMethod = endpointClass.getMethod("toString", EMPTY_SIGNATURE);
    hashCodeMethod = endpointClass.getMethod("hashCode", EMPTY_SIGNATURE);
    equalsMethod = endpointClass.getMethod("equals", OBJECT_SIGNATURE);

    methodMap.put(new Function(toStringMethod), new Methodology(serviceInterface, toStringMethod));
    methodMap.put(new Function(hashCodeMethod), new Methodology(serviceInterface, hashCodeMethod));
    methodMap.put(new Function(equalsMethod), new Methodology(serviceInterface, equalsMethod, new SyntheticArgument("obj", Object.class)));
  }

  public Function match (Function partialFunction) {

    for (Function function : methodMap.keySet()) {
      if (function.getName().equals(partialFunction.getName())) {
        if (((partialFunction.getSignature() == null) || Arrays.equals(partialFunction.getSignature(), function.getSignature())) && ((partialFunction.getResultType() == null) || partialFunction.getResultType().equals(function.getResultType()))) {

          return function;
        }
      }
    }

    return null;
  }

  public Methodology getMethodology (Function function)
    throws MissingInvocationException {

    Methodology methodology;

    if ((methodology = methodMap.get(function)) == null) {
      throw new MissingInvocationException("No method(%s) available in service interface(%s)", function.getName(), serviceInterface.getName());
    }

    return methodology;
  }

  public Object remoteInvocation (WireContext[] contexts, Function function, Object... arguments)
    throws Exception {

    Methodology methodology;

    if ((methodology = methodMap.get(function)) == null) {
      throw new MissingInvocationException("No method(%s) available in service interface(%s)", function.getName(), serviceInterface.getName());
    }

    if ((contexts != null) && (contexts.length > 0)) {
      for (WireContext context : contexts) {
        if (context != null) {
          ContextFactory.pushContext(context);
        }
      }
    }

    try {
      return methodology.getMethod().invoke(targetObject, arguments);
    } catch (InvocationTargetException invocationTargetException) {
      if ((invocationTargetException.getCause() != null) && (invocationTargetException.getCause() instanceof Exception)) {
        throw (Exception)invocationTargetException.getCause();
      } else {
        throw invocationTargetException;
      }
    } finally {
      if ((contexts != null) && (contexts.length > 0)) {
        for (WireContext context : contexts) {
          if (context != null) {
            ContextFactory.popContext(context);
          }
        }
      }
    }
  }
}