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
package org.smallmind.web.websocket.spi;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Decoder;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;

/**
 * Wraps an annotated {@link ClientEndpoint} class into an {@link Endpoint} with derived configuration.
 */
public class AnnotatedEndpoint extends Endpoint {

  private final AnnotatedClientEndpointConfig endpointConfig;

  /**
   * Creates an annotated endpoint from a {@link ClientEndpoint}-annotated class.
   *
   * @param annotatedClass the class containing websocket annotations
   * @throws DeploymentException if the class is not annotated or configuration cannot be built
   */
  public AnnotatedEndpoint (Class<?> annotatedClass)
    throws DeploymentException {

    ClientEndpoint clientEndpointAnnotation;

    if ((clientEndpointAnnotation = annotatedClass.getAnnotation(ClientEndpoint.class)) == null) {
      throw new DeploymentException("Missing the required ClientEndpoint annotation");
    }

    try {
      endpointConfig = new AnnotatedClientEndpointConfig(clientEndpointAnnotation);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
      throw new DeploymentException("Unable to to instantiate the client endpoint configuration", exception);
    }
  }

  /**
   * Provides the derived configuration for this annotated endpoint.
   *
   * @return the endpoint configuration
   */
  AnnotatedClientEndpointConfig getEndpointConfig () {

    return endpointConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onOpen (Session session, EndpointConfig config) {

  }

  /**
   * Client endpoint config derived from annotations.
   */
  private static class AnnotatedClientEndpointConfig implements ClientEndpointConfig {

    private final ClientEndpoint clientEndpoint;
    private final Configurator configurator;
    private final HashMap<String, Object> userProperties = new HashMap<>();

    /**
     * Builds configuration based on the {@link ClientEndpoint} annotation values.
     *
     * @param clientEndpoint the annotation instance
     * @throws InstantiationException if the configurator cannot be created
     * @throws IllegalAccessException if the configurator constructor is inaccessible
     * @throws NoSuchMethodException if a required constructor is missing
     * @throws InvocationTargetException if configurator construction fails
     */
    public AnnotatedClientEndpointConfig (ClientEndpoint clientEndpoint)
      throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

      this.clientEndpoint = clientEndpoint;

      configurator = clientEndpoint.configurator().getConstructor().newInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLContext getSSLContext () {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getPreferredSubprotocols () {

      return Arrays.asList(clientEndpoint.subprotocols());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Extension> getExtensions () {

      return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configurator getConfigurator () {

      return configurator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<? extends Encoder>> getEncoders () {

      return Arrays.asList(clientEndpoint.encoders());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<? extends Decoder>> getDecoders () {

      return Arrays.asList(clientEndpoint.decoders());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getUserProperties () {

      return userProperties;
    }
  }
}
