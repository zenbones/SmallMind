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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import java.util.Arrays;
import java.util.List;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.nutsnbolts.servlet.FormattedServletException;

/**
 * {@link Transport} that registers a JSR-356 {@link jakarta.websocket.Endpoint} with the servlet
 * container on startup and exposes configuration parameters to endpoint instances via servlet
 * context attributes.
 *
 * @param <V> the concrete {@link Value} type carried by messages in this deployment
 */
public class WebSocketTransport<V extends Value<V>> extends AbstractAttributed implements Transport<V> {

  public static final String ATTRIBUTE = "org.smallmind.bayeux.oumuamua.transport.websocket";

  private final WebsocketProtocol<V> websocketProtocol;
  private final WebsocketConfiguration websocketConfiguration;

  /**
   * Constructs the transport bound to its owning protocol and the supplied configuration.
   *
   * @param websocketProtocol      the {@link WebsocketProtocol} that owns this transport
   * @param websocketConfiguration configuration parameters used at endpoint registration time and
   *                               exposed to connected sessions
   */
  public WebSocketTransport (WebsocketProtocol<V> websocketProtocol, WebsocketConfiguration websocketConfiguration) {

    this.websocketProtocol = websocketProtocol;
    this.websocketConfiguration = websocketConfiguration;
  }

  /**
   * Returns the {@link WebsocketProtocol} that owns this transport.
   *
   * @return owning protocol instance
   */
  @Override
  public Protocol<V> getProtocol () {

    return websocketProtocol;
  }

  /**
   * Returns the canonical transport name as defined by
   * {@link org.smallmind.bayeux.oumuamua.server.spi.Transports#WEBSOCKET}.
   *
   * @return transport name string
   */
  @Override
  public String getName () {

    return Transports.WEBSOCKET.getName();
  }

  /**
   * Indicates whether this transport is restricted to local (same-JVM) use, as determined by
   * {@link org.smallmind.bayeux.oumuamua.server.spi.Transports#WEBSOCKET}.
   *
   * @return {@code true} if the transport is local-only, {@code false} if it accepts remote clients
   */
  @Override
  public boolean isLocal () {

    return Transports.WEBSOCKET.isLocal();
  }

  /**
   * Returns the configured maximum idle session timeout, forwarded from
   * {@link WebsocketConfiguration#getMaxIdleTimeoutMilliseconds()}.
   *
   * @return idle timeout in milliseconds, or {@code -1} to use the container default
   */
  public long getMaxIdleTimeoutMilliseconds () {

    return websocketConfiguration.getMaxIdleTimeoutMilliseconds();
  }

  /**
   * Returns the configured asynchronous send timeout, forwarded from
   * {@link WebsocketConfiguration#getAsyncSendTimeoutMilliseconds()}.
   *
   * @return async send timeout in milliseconds; {@code 0} means no timeout
   */
  public long getAsyncSendTimeoutMilliseconds () {

    return websocketConfiguration.getAsyncSendTimeoutMilliseconds();
  }

  /**
   * Returns the configured maximum incoming text message buffer size, forwarded from
   * {@link WebsocketConfiguration#getMaximumTextMessageBufferSize()}.
   *
   * @return buffer size in characters, or {@code -1} to use the container default
   */
  public int getMaximumTextMessageBufferSize () {

    return websocketConfiguration.getMaximumTextMessageBufferSize();
  }

  /**
   * Registers the configured {@link jakarta.websocket.Endpoint} with the JSR-356
   * {@link ServerContainer} found in the servlet context, injecting the {@link Server} and this
   * transport as user properties so that endpoint instances can retrieve them.
   *
   * @param server        the Oumuamua {@link Server} instance to expose to endpoint sessions
   * @param servletConfig the servlet configuration providing access to the JSR-356 container
   * @throws ServletException wrapping any {@link DeploymentException} thrown during registration
   */
  @Override
  public void init (Server<?> server, ServletConfig servletConfig)
    throws ServletException {

    ServerContainer container = (ServerContainer)servletConfig.getServletContext().getAttribute(ServerContainer.class.getName());
    ServerEndpointConfig.Configurator configurator = new ServerEndpointConfig.Configurator();
    ServerEndpointConfig config = ServerEndpointConfig.Builder.create(websocketConfiguration.getEndpointClass(), normalizeURL(websocketConfiguration.getOumuamuaUrl()))
                                    .subprotocols((websocketConfiguration.getSubProtocol() == null) ? null : List.of(websocketConfiguration.getSubProtocol()))
                                    .configurator(new WebsocketConfigurator(configurator))
                                    .extensions((websocketConfiguration.getExtensions() == null) ? null : Arrays.asList(websocketConfiguration.getExtensions()))
                                    .build();

    config.getUserProperties().put(Server.ATTRIBUTE, server);
    config.getUserProperties().put(ATTRIBUTE, this);

    try {
      container.addEndpoint(config);
    } catch (DeploymentException deploymentException) {
      throw new FormattedServletException(deploymentException);
    }
  }

  /**
   * Prepends a leading {@code /} if absent and strips a trailing {@code /*} wildcard from the URL.
   *
   * @param url the raw URL path from configuration
   * @return the normalised path ready for JSR-356 endpoint registration
   */
  private String normalizeURL (String url) {

    return url.startsWith("/") ? stripWildcard(url) : "/" + stripWildcard(url);
  }

  /**
   * Removes a trailing {@code /*} wildcard segment from the URL if present.
   *
   * @param url the URL path to strip
   * @return the URL without its trailing {@code /*}, or the original string if no wildcard is present
   */
  private String stripWildcard (String url) {

    return url.endsWith("/*") ? url.substring(0, url.length() - 2) : url;
  }
}
