package org.smallmind.phalanx.wire.spring;

import java.lang.reflect.Proxy;
import org.smallmind.phalanx.wire.RequestTransport;
import org.smallmind.phalanx.wire.WireProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class WireProxyFactoryBean implements InitializingBean, FactoryBean<Proxy> {

  private Proxy serviceProxy;
  private RequestTransport requestTransport;
  private Class<?> serviceInterface;
  private String serviceName;
  private int version;

  public void setServiceInterface (Class<?> serviceInterface) {

    this.serviceInterface = serviceInterface;
  }

  public void setRequestTransport (RequestTransport requestTransport) {

    this.requestTransport = requestTransport;
  }

  public void setServiceName (String serviceName) {

    this.serviceName = serviceName;
  }

  public void setVersion (int version) {

    this.version = version;
  }

  @Override
  public void afterPropertiesSet ()
    throws Exception {

    serviceProxy = WireProxyFactory.generateProxy(requestTransport, version, serviceName, serviceInterface);
  }

  @Override
  public Proxy getObject () throws Exception {

    return serviceProxy;
  }

  @Override
  public Class<?> getObjectType () {

    return serviceInterface;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
