/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.nutsnbolts.servlet.FormattedServletException;

public class WebSocketTransport<V extends Value<V>> extends AbstractAttributed implements Transport<V> {

  public static final String ATTRIBUTE = "org.smallmind.bayeux.oumuamua.transport.websocket";

  private final WebsocketProtocol<V> websocketProtocol;
  private final WebsocketConfiguration websocketConfiguration;

  public WebSocketTransport (WebsocketProtocol<V> websocketProtocol, WebsocketConfiguration websocketConfiguration) {

    this.websocketProtocol = websocketProtocol;
    this.websocketConfiguration = websocketConfiguration;
  }

  @Override
  public Protocol<V> getProtocol () {

    return websocketProtocol;
  }

  @Override
  public String getName () {

    return Transports.WEBSOCKET.getName();
  }

  public long getMaxIdleTimeoutMilliseconds () {

    return websocketConfiguration.getMaxIdleTimeoutMilliseconds();
  }

  public long getAsyncSendTimeoutMilliseconds () {

    return websocketConfiguration.getAsyncSendTimeoutMilliseconds();
  }

  public int getMaximumTextMessageBufferSize () {

    return websocketConfiguration.getMaximumTextMessageBufferSize();
  }

  @Override
  public void init (Server<?> server, ServletConfig servletConfig)
    throws ServletException {

    ServerContainer container = (ServerContainer)servletConfig.getServletContext().getAttribute(ServerContainer.class.getName());
    ServerEndpointConfig.Configurator configurator = new ServerEndpointConfig.Configurator();
    ServerEndpointConfig config = ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, normalizeURL(websocketConfiguration.getOumuamuaUrl()))
                                    .subprotocols(websocketConfiguration.getSubProtocol() == null ? null : List.of(websocketConfiguration.getSubProtocol()))
                                    .configurator(new WebsocketConfigurator(configurator))
                                    .build();

    config.getUserProperties().put(Server.ATTRIBUTE, server);
    config.getUserProperties().put(ATTRIBUTE, this);

    try {
      container.addEndpoint(config);
    } catch (DeploymentException deploymentException) {
      throw new FormattedServletException(deploymentException);
    }
  }

  private String normalizeURL (String url) {

    return url.startsWith("/") ? stripWildcard(url) : "/" + stripWildcard(url);
  }

  private String stripWildcard (String url) {

    return url.endsWith("/*") ? url.substring(0, url.length() - 2) : url;
  }
}
