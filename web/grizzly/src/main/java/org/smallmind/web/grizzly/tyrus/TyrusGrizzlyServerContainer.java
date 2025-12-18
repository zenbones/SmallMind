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
import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.cluster.ClusterContext;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;

/**
 * Grizzly-backed {@link TyrusServerContainer} that wires WebSocket support into the configured {@link NetworkListener}
 * and optionally serves WSADL for discovery.
 */
public class TyrusGrizzlyServerContainer extends TyrusServerContainer {

  private final TyrusWebSocketEngine engine;
  private final WebSocketExtensionInstaller[] webSocketExtensionInstallers;
  private final NetworkListener networkListener;
  private final Map<String, Object> properties;
  private final String contextPath;

  /**
   * Creates and configures the Tyrus WebSocket engine and attaches a {@link TyrusWebSocketAddOn} to the provided
   * listener.
   *
   * @param httpServer                  Grizzly HTTP server hosting the application
   * @param networkListener             listener used to serve WebSocket connections
   * @param webappContext               web application context for attribute registration
   * @param properties                  optional container properties passed to Tyrus
   * @param includeWsadlSupport         whether to expose WSADL discovery
   * @param staticHttpHandler           optional static handler used to serve WSADL when enabled
   * @param webSocketExtensionInstallers extension installers applied when merging endpoint configs
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
    networkListener.registerAddOn(new TyrusWebSocketAddOn(this, webappContext.getContextPath(), new HashMap<>()));

    if (includeWsadlSupport) {
      httpServer.getServerConfiguration().addHttpHandler(new WsadlHttpHandler(engine, staticHttpHandler));
    }

    contextPath = webappContext.getContextPath();
    webappContext.setAttribute("jakarta.websocket.server.ServerContainer", this);
  }

  /**
   * Starts the container at the configured context and port.
   *
   * @throws IOException          if underlying transport cannot start
   * @throws DeploymentException  if WebSocket deployment fails
   */
  public void start ()
    throws IOException, DeploymentException {

    super.start(contextPath, getPort());
  }

  /**
   * @return immutable map of container properties supplied at construction time
   */
  @Override
  public Map<String, Object> getProperties () {

    return properties;
  }

  /**
   * @return active port of the attached {@link NetworkListener}, or -1 if unavailable
   */
  @Override
  public int getPort () {

    return ((networkListener != null) && (networkListener.getPort() > 0)) ? networkListener.getPort() : -1;
  }

  /**
   * @return the underlying Tyrus {@link WebSocketEngine}
   */
  @Override
  public WebSocketEngine getWebSocketEngine () {

    return engine;
  }

  /**
   * Registers an annotated endpoint class against the current context path.
   *
   * @param endpointClass endpoint to deploy
   * @throws DeploymentException if deployment fails
   */
  @Override
  public void register (Class<?> endpointClass)
    throws DeploymentException {

    engine.register(endpointClass, contextPath);
  }

  /**
   * Registers a {@link ServerEndpointConfig}, merging any configured {@link Extension extensions} from installers that
   * target the same endpoint class and path.
   *
   * @param serverEndpointConfig endpoint configuration to deploy
   * @throws DeploymentException if deployment fails
   */
  @Override
  public void register (ServerEndpointConfig serverEndpointConfig)
    throws DeploymentException {

    engine.register(mergeExtensions(serverEndpointConfig), contextPath);
  }

  /**
   * Merges installer-provided extensions with the supplied endpoint configuration, avoiding duplicates.
   *
   * @param serverEndpointConfig original configuration
   * @return configuration including additional extensions when applicable
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
