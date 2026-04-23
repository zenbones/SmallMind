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
 * Spring {@link FactoryBean} that builds and exposes a wire client proxy for a configured service interface
 * by delegating to {@link WireProxyFactory#generateProxy}.
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
   * Sets the service interface that the generated proxy must implement.
   *
   * @param serviceInterface interface class for the target service
   */
  public void setServiceInterface (Class<?> serviceInterface) {

    this.serviceInterface = serviceInterface;
  }

  /**
   * Sets the request transport used to submit outbound invocations.
   *
   * @param requestTransport transport for outbound calls
   */
  public void setRequestTransport (RequestTransport requestTransport) {

    this.requestTransport = requestTransport;
  }

  /**
   * Sets the logical service name embedded in each outbound request.
   *
   * @param serviceName logical name of the remote service
   */
  public void setServiceName (String serviceName) {

    this.serviceName = serviceName;
  }

  /**
   * Sets the service version embedded in each outbound request.
   *
   * @param version service version number
   */
  public void setVersion (int version) {

    this.version = version;
  }

  /**
   * Sets the extractor that resolves the service group at invocation time.
   *
   * @param serviceGroupExtractor extractor for the service group, or {@code null} for the default group
   */
  public void setServiceGroupExtractor (ParameterExtractor<String> serviceGroupExtractor) {

    this.serviceGroupExtractor = serviceGroupExtractor;
  }

  /**
   * Sets the extractor that resolves the target instance id for whisper (point-to-point) calls.
   *
   * @param instanceIdExtractor extractor for the instance id, or {@code null} for broadcast calls
   */
  public void setInstanceIdExtractor (ParameterExtractor<String> instanceIdExtractor) {

    this.instanceIdExtractor = instanceIdExtractor;
  }

  /**
   * Sets the extractor that resolves a per-call timeout in milliseconds.
   *
   * @param timeoutExtractor extractor for the call timeout, or {@code null} to use the transport default
   */
  public void setTimeoutExtractor (ParameterExtractor<Long> timeoutExtractor) {

    this.timeoutExtractor = timeoutExtractor;
  }

  /**
   * Generates the wire proxy after all required properties have been set.
   *
   * @throws Exception if proxy creation fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    serviceProxy = WireProxyFactory.generateProxy(requestTransport, version, serviceName, serviceInterface, serviceGroupExtractor, instanceIdExtractor, timeoutExtractor);
  }

  /**
   * Returns the generated wire proxy instance.
   *
   * @return proxy implementing the configured service interface
   */
  @Override
  public Proxy getObject () {

    return serviceProxy;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return the configured service interface class
   */
  @Override
  public Class<?> getObjectType () {

    return serviceInterface;
  }

  /**
   * Indicates that this factory always returns the same proxy instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
