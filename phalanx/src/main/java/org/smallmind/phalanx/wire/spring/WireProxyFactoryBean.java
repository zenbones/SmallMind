/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.phalanx.wire.spring;

import java.lang.reflect.Proxy;
import org.smallmind.phalanx.wire.ParameterExtractor;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that creates a wire client proxy for a service interface.
 */
public class WireProxyFactoryBean implements InitializingBean, FactoryBean<Proxy> {

  private Proxy serviceProxy;
  private RequestTransport requestTransport;
  private ParameterExtractor<String> serviceGroupExtractor;
  private ParameterExtractor<String> instanceIdExtractor;
  private ParameterExtractor<Long> timeoutExtractor;
  private Class<?> serviceInterface;
  private String serviceName;
  private int version;

  /**
   * @param serviceInterface interface the proxy should implement.
   */
  public void setServiceInterface (Class<?> serviceInterface) {

    this.serviceInterface = serviceInterface;
  }

  /**
   * @param requestTransport transport used for outbound calls.
   */
  public void setRequestTransport (RequestTransport requestTransport) {

    this.requestTransport = requestTransport;
  }

  /**
   * @param serviceName logical service name.
   */
  public void setServiceName (String serviceName) {

    this.serviceName = serviceName;
  }

  /**
   * @param version service version.
   */
  public void setVersion (int version) {

    this.version = version;
  }

  /**
   * @param serviceGroupExtractor extractor resolving service group per invocation.
   */
  public void setServiceGroupExtractor (ParameterExtractor<String> serviceGroupExtractor) {

    this.serviceGroupExtractor = serviceGroupExtractor;
  }

  /**
   * @param instanceIdExtractor extractor resolving instance id for whispers.
   */
  public void setInstanceIdExtractor (ParameterExtractor<String> instanceIdExtractor) {

    this.instanceIdExtractor = instanceIdExtractor;
  }

  /**
   * @param timeoutExtractor extractor resolving per-call timeout.
   */
  public void setTimeoutExtractor (ParameterExtractor<Long> timeoutExtractor) {

    this.timeoutExtractor = timeoutExtractor;
  }

  /**
   * Builds the proxy once all properties are set.
   *
   * @throws Exception if proxy creation fails.
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    serviceProxy = WireProxyFactory.generateProxy(requestTransport, version, serviceName, serviceInterface, serviceGroupExtractor, instanceIdExtractor, timeoutExtractor);
  }

  /**
   * @return the created proxy.
   */
  @Override
  public Proxy getObject () {

    return serviceProxy;
  }

  /**
   * @return the service interface type.
   */
  @Override
  public Class<?> getObjectType () {

    return serviceInterface;
  }

  /**
   * @return always true; proxy is a singleton.
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
