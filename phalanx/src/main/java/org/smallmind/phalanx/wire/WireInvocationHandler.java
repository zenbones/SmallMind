package org.smallmind.phalanx.wire;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.context.Context;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class WireInvocationHandler implements InvocationHandler {

  private static final Class[] EMPTY_SIGNATURE = new Class[0];
  private static final Class[] OBJECT_SIGNATURE = {Object.class};
  private static final String[] NO_NAMES = new String[0];
  private static final String[] SINGLE_OBJECT_NAME = new String[]{"obj"};
  private final RequestTransport transport;
  private final ConcurrentHashMap<Class<? extends InstanceIdExtractor>, InstanceIdExtractor> instanceIdExtractorMap = new ConcurrentHashMap<>();
  private final HashMap<Method, String[]> methodMap = new HashMap<>();
  private final Class serviceInterface;
  private final String serviceGroup;
  private final String serviceName;
  private final int version;

  public WireInvocationHandler (RequestTransport transport, String serviceGroup, int version, String serviceName, Class<?> serviceInterface)
    throws Exception {

    this.transport = transport;
    this.version = version;
    this.serviceGroup = serviceGroup;
    this.serviceName = serviceName;
    this.serviceInterface = serviceInterface;

    for (Method method : serviceInterface.getMethods()) {

      String[] argumentNames = new String[method.getParameterTypes().length];
      int index = 0;

      for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
        for (Annotation annotation : parameterAnnotations) {
          if (annotation.annotationType().equals(Argument.class)) {
            argumentNames[index++] = ((Argument)annotation).value();
            break;
          }
        }
      }

      if (index != argumentNames.length) {
        throw new ServiceDefinitionException("The method(%s) of service interface(%s) requires @Argument annotations", method.getName(), serviceInterface.getName());
      }

      methodMap.put(method, argumentNames);
    }

    try {
      serviceInterface.getMethod("toString", EMPTY_SIGNATURE);
    } catch (NoSuchMethodException noSuchMethodException) {
      methodMap.put(Object.class.getMethod("toString", EMPTY_SIGNATURE), NO_NAMES);
    }
    try {
      serviceInterface.getMethod("hashCode", EMPTY_SIGNATURE);
    } catch (NoSuchMethodException noSuchMethodException) {
      methodMap.put(Object.class.getMethod("hashCode", EMPTY_SIGNATURE), NO_NAMES);
    }
    try {
      serviceInterface.getMethod("equals", OBJECT_SIGNATURE);
    } catch (NoSuchMethodException noSuchMethodException) {
      methodMap.put(Object.class.getMethod("equals", OBJECT_SIGNATURE), SINGLE_OBJECT_NAME);
    }
  }

  public Object invoke (Object proxy, final Method method, final Object[] args)
    throws Throwable {

    HashMap<String, Object> argumentMap = null;
    Context[] expectedContexts;
    WireContext[] wireContexts = null;
    Whisper whisper;
    String[] argumentNames;
    String instanceId;

    if ((argumentNames = methodMap.get(method)) == null) {
      throw new MissingInvocationException("No method(%s) available in the service interface(%s)", method.getName(), serviceInterface.getName());
    }
    if (argumentNames.length != ((args == null) ? 0 : args.length)) {
      throw new ServiceDefinitionException("The arguments for method(%s) in the service interface(%s) do not match those known from the service interface annotations", method.getName(), serviceInterface.getName());
    }

    if ((args != null) && (args.length > 0)) {
      argumentMap = new HashMap<>();
      for (int index = 0; index < args.length; index++) {
        if ((args[index] != null) && (!(args[index] instanceof Serializable))) {
          throw new TransportException("The argument(index=%d, name=%s, class=%s) is not Serializable", index, argumentNames[index], args[index].getClass().getName());
        }

        argumentMap.put(argumentNames[index], args[index]);
      }
    }

    if ((expectedContexts = ContextFactory.getContextsOn(method, WireContext.class)) != null) {

      int index = 0;

      wireContexts = new WireContext[expectedContexts.length];
      for (Context expectedContext : expectedContexts) {
        if (expectedContext instanceof WireContext) {
          wireContexts[index++] = (WireContext)expectedContext;
        }
      }
    }

    if ((whisper = method.getAnnotation(Whisper.class)) != null) {

      InstanceIdExtractor instanceIdExtractor;

      if ((instanceIdExtractor = instanceIdExtractorMap.get(whisper.value())) == null) {
        instanceIdExtractorMap.put(whisper.value(), instanceIdExtractor = whisper.value().newInstance());
      }
      if ((instanceId = instanceIdExtractor.getInstanceId(argumentMap, wireContexts)) == null) {
        throw new MissingInstanceIdException("Whisper invocations require an instance id(%s)", whisper.value().getName());
      }
    } else {
      instanceId = null;
    }

    if (method.getAnnotation(InOnly.class) != null) {

      if (!method.getReturnType().equals(void.class)) {
        throw new ServiceDefinitionException("The method(%s) in service interface(%s) is marked as @InOnly but does not return 'void'", method.getName(), serviceInterface.getName());
      }
      if (method.getExceptionTypes().length > 0) {
        throw new ServiceDefinitionException("The method(%s) in service interface(%s) is marked as @InOnly but declares an Exception list", method.getName(), serviceInterface.getName());
      }

      transport.transmitInOnly(serviceGroup, instanceId, new Address(version, serviceName, new Function(method)), argumentMap, wireContexts);

      return null;
    } else {

      return transport.transmitInOut(serviceGroup, instanceId, new Address(version, serviceName, new Function(method)), argumentMap, wireContexts);
    }
  }
}