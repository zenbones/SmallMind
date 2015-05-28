package org.smallmind.throng.wire;

import java.lang.reflect.Proxy;

public class WireProxyFactory {

  public static Proxy generateProxy (RequestTransport transport, int version, String serviceName, Class<?> serviceInterface, InstanceIdExtractor instanceIdExtractor)
    throws Exception {

    return (Proxy)Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[]{serviceInterface}, new WireInvocationHandler(transport, version, serviceName, serviceInterface, instanceIdExtractor));
  }
}