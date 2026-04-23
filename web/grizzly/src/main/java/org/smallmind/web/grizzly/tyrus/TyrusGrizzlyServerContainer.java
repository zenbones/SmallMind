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
package org.smallmind.web.grizzly.tyrus;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.tyrus.container.grizzly.server.WebSocketAddOn;
import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.cluster.ClusterContext;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;

/**
 * Grizzly-specific {@link TyrusServerContainer} that attaches Tyrus WebSocket support to a configured
 * {@link NetworkListener} and optionally exposes a WSADL discovery handler.
 */
public class TyrusGrizzlyServerContainer extends TyrusServerContainer {

  private final TyrusWebSocketEngine engine;
  private final WebSocketExtensionInstaller[] webSocketExtensionInstallers;
  private final NetworkListener networkListener;
  private final Map<String, Object> properties;
  private final String contextPath;

  /**
   * Constructs the container, builds the Tyrus engine from the provided properties, registers a
   * {@link WebSocketAddOn} on the listener, and optionally adds a WSADL handler to the server.
   *
   * @param httpServer                   Grizzly server that will host the WebSocket add-on and optional WSADL handler
   * @param networkListener              listener to register the WebSocket add-on on
   * @param webappContext                servlet context used for context path and attribute storage
   * @param properties                   Tyrus engine properties, or {@code null} for defaults
   * @param includeWsadlSupport          {@code true} to register the WSADL discovery handler
   * @param staticHttpHandler            fallback handler for WSADL requests that are not for the descriptor document
   * @param webSocketExtensionInstallers extension installers whose extensions are merged into matching endpoint configs
   */
  public TyrusGrizzlyServerContainer (HttpServer httpServer, NetworkListener networkListener, WebappContext webappContext, Map<String, Object> properties, boolean includeWsadlSupport, HttpHandler staticHttpHandler, WebSocketExtensionInstaller... webSocketExtensionInstallers) {

    super((Set<Class<?>>)null);

    final Map<String, Object> localProperties;

    this.networkListener = networkListener;
    this.properties = properties;
    this.webSocketExtensionInstallers = webSocketExtensionInstallers;

    // defensive copy
    if (properties == null) {
      localProperties = Collections.emptyMap();
    } else {
      localProperties = new HashMap<>(properties);
    }

    final Integer incomingBufferSize = Utils.getProperty(localProperties, TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, Integer.class);
    final ClusterContext clusterContext = Utils.getProperty(localProperties, ClusterContext.CLUSTER_CONTEXT, ClusterContext.class);
    final Integer maxSessionsPerApp = Utils.getProperty(localProperties, TyrusWebSocketEngine.MAX_SESSIONS_PER_APP, Integer.class);
    final Integer maxSessionsPerRemoteAddr = Utils.getProperty(localProperties, TyrusWebSocketEngine.MAX_SESSIONS_PER_REMOTE_ADDR, Integer.class);
    final Boolean parallelBroadcastEnabled = Utils.getProperty(localProperties, TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, Boolean.class);
    final DebugContext.TracingType tracingType = Utils.getProperty(localProperties, TyrusWebSocketEngine.TRACING_TYPE, DebugContext.TracingType.class, DebugContext.TracingType.OFF);
    final DebugContext.TracingThreshold tracingThreshold = Utils.getProperty(localProperties, TyrusWebSocketEngine.TRACING_THRESHOLD, DebugContext.TracingThreshold.class, DebugContext.TracingThreshold.TRACE);

    final ApplicationEventListener applicationEventListener = Utils.getProperty(localProperties, ApplicationEventListener.APPLICATION_EVENT_LISTENER, ApplicationEventListener.class);

    engine = TyrusWebSocketEngine.builder(this)
               .incomingBufferSize(incomingBufferSize)
               .clusterContext(clusterContext)
               .applicationEventListener(applicationEventListener)
               .maxSessionsPerApp(maxSessionsPerApp)
               .maxSessionsPerRemoteAddr(maxSessionsPerRemoteAddr)
               .parallelBroadcastEnabled(parallelBroadcastEnabled)
               .tracingType(tracingType)
               .tracingThreshold(tracingThreshold)
               .build();

    // idle timeout set to indefinite.
    networkListener.getKeepAlive().setIdleTimeoutInSeconds(-1);
    networkListener.registerAddOn(new WebSocketAddOn(this, webappContext.getContextPath()));

    if (includeWsadlSupport) {
      httpServer.getServerConfiguration().addHttpHandler(new WsadlHttpHandler(engine, staticHttpHandler));
    }

    contextPath = webappContext.getContextPath();
    webappContext.setAttribute("jakarta.websocket.server.ServerContainer", this);
  }

  /**
   * Starts the WebSocket container at the configured context path and listener port.
   *
   * @throws IOException         if the transport cannot be started
   * @throws DeploymentException if endpoint deployment fails
   */
  public void start ()
    throws IOException, DeploymentException {

    super.start(contextPath, getPort());
  }

  /**
   * Returns the container properties supplied at construction time.
   *
   * @return immutable view of the properties map, or the original map reference
   */
  @Override
  public Map<String, Object> getProperties () {

    return properties;
  }

  /**
   * Returns the port of the attached network listener.
   *
   * @return listener port, or {@code -1} if the listener is unavailable or not yet bound
   */
  @Override
  public int getPort () {

    return ((networkListener != null) && (networkListener.getPort() > 0)) ? networkListener.getPort() : -1;
  }

  /**
   * Returns the underlying Tyrus WebSocket engine.
   *
   * @return the {@link WebSocketEngine} instance
   */
  @Override
  public WebSocketEngine getWebSocketEngine () {

    return engine;
  }

  /**
   * Registers an annotated WebSocket endpoint class with the engine.
   *
   * @param endpointClass annotated endpoint class to deploy
   * @throws DeploymentException if the endpoint cannot be registered
   */
  @Override
  public void register (Class<?> endpointClass)
    throws DeploymentException {

    engine.register(endpointClass, contextPath);
  }

  /**
   * Registers a programmatic WebSocket endpoint configuration, first merging any additional extensions provided by
   * matching installers.
   *
   * @param serverEndpointConfig endpoint configuration to deploy
   * @throws DeploymentException if the endpoint cannot be registered
   */
  @Override
  public void register (ServerEndpointConfig serverEndpointConfig)
    throws DeploymentException {

    engine.register(mergeExtensions(serverEndpointConfig), contextPath);
  }

  /**
   * Returns a potentially new {@link ServerEndpointConfig} with installer-provided extensions added, skipping any that
   * duplicate existing extensions by class or name.
   *
   * @param serverEndpointConfig original endpoint configuration
   * @return updated configuration including additional extensions, or the original if none were added
   */
  private ServerEndpointConfig mergeExtensions (ServerEndpointConfig serverEndpointConfig) {

    if (webSocketExtensionInstallers != null) {
      for (WebSocketExtensionInstaller webSocketExtensionInstaller : webSocketExtensionInstallers) {
        if (webSocketExtensionInstaller.getEndpointClass().equals(serverEndpointConfig.getEndpointClass()) && webSocketExtensionInstaller.getPath().equals(serverEndpointConfig.getPath())) {

          LinkedList<Extension> addedExtensionList = new LinkedList<>(Arrays.asList(webSocketExtensionInstaller.getExtensions()));

          for (Extension extension : serverEndpointConfig.getExtensions()) {
            addedExtensionList.removeIf((addedExtension) -> addedExtension.getClass().equals(extension.getClass()) || ((addedExtension.getName() != null) && addedExtension.getName().equals(extension.getName())));
            if (addedExtensionList.isEmpty()) {
              break;
            }
          }

          if (!addedExtensionList.isEmpty()) {

            addedExtensionList.addAll(serverEndpointConfig.getExtensions());

            return ServerEndpointConfig.Builder.create(serverEndpointConfig.getEndpointClass(), serverEndpointConfig.getPath())
                     .configurator(serverEndpointConfig.getConfigurator())
                     .decoders(serverEndpointConfig.getDecoders())
                     .encoders(serverEndpointConfig.getEncoders())
                     .extensions(addedExtensionList)
                     .subprotocols(serverEndpointConfig.getSubprotocols()).build();
          }
        }
      }
    }

    return serverEndpointConfig;
  }
}
