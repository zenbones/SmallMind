/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Session;

public class AnnotatedEndpoint extends Endpoint {

  private final AnnotatedClientEndpointConfig endpointConfig;

  public AnnotatedEndpoint (Class<?> annotatedClass)
    throws DeploymentException {

    ClientEndpoint clientEndpointAnnotation;

    if ((clientEndpointAnnotation = annotatedClass.getAnnotation(ClientEndpoint.class)) == null) {
      throw new DeploymentException("Missing the required ClientEndpoint annotation");
    }

    try {
      endpointConfig = new AnnotatedClientEndpointConfig(clientEndpointAnnotation);
    } catch (InstantiationException | IllegalAccessException exception) {
      throw new DeploymentException("Unable to to instantiate the client endpoint configuration", exception);
    }
  }

  public AnnotatedClientEndpointConfig getEndpointConfig () {

    return endpointConfig;
  }

  @Override
  public void onOpen (Session session, EndpointConfig config) {

  }

  private class AnnotatedClientEndpointConfig implements ClientEndpointConfig {

    private final ClientEndpoint clientEndpoint;
    private final Configurator configurator;
    private final HashMap<String, Object> userProperties = new HashMap<>();

    public AnnotatedClientEndpointConfig (ClientEndpoint clientEndpoint)
      throws InstantiationException, IllegalAccessException {

      this.clientEndpoint = clientEndpoint;

      configurator = clientEndpoint.configurator().newInstance();
    }

    @Override
    public List<String> getPreferredSubprotocols () {

      return Arrays.asList(clientEndpoint.subprotocols());
    }

    @Override
    public List<Extension> getExtensions () {

      return null;
    }

    @Override
    public Configurator getConfigurator () {

      return configurator;
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders () {

      return Arrays.asList(clientEndpoint.encoders());
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders () {

      return Arrays.asList(clientEndpoint.decoders());
    }

    @Override
    public Map<String, Object> getUserProperties () {

      return userProperties;
    }
  }
}
