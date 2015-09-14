package org.smallmind.phalanx.wire;

import java.lang.reflect.Proxy;

public class WireProxyFactory {

  public static Proxy generateProxy (RequestTransport transport, String serviceGroup, int version, String serviceName, Class<?> serviceInterface)
    throws Exception {

    return (Proxy)Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[]{serviceInterface}, new WireInvocationHandler(transport, serviceGroup, version, serviceName, serviceInterface));
  }
}