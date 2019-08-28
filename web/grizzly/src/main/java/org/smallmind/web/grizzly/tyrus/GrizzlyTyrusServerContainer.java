/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import javax.websocket.DeploymentException;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.cluster.ClusterContext;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.smallmind.web.grizzly.WebSocketExtensionInstaller;

public class GrizzlyTyrusServerContainer extends TyrusServerContainer {

  private final WebSocketExtensionInstaller[] webSocketExtensionInstallers;
  private final WebSocketEngine webSocketEngine;
  private final NetworkListener networkListener;
  private final String contextPath;

  public GrizzlyTyrusServerContainer (NetworkListener networkListener, String contextPath, WebSocketExtensionInstaller... webSocketExtensionInstallers) {

    super((Set<Class<?>>)null);

    this.networkListener = networkListener;
    this.contextPath = contextPath;
    this.webSocketExtensionInstallers = webSocketExtensionInstallers;

    HashMap<String, Object> localProperties = new HashMap<>();

    final Integer incomingBufferSize = Utils.getProperty(localProperties, TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, Integer.class);
    final ClusterContext clusterContext = Utils.getProperty(localProperties, ClusterContext.CLUSTER_CONTEXT, ClusterContext.class);
    final ApplicationEventListener applicationEventListener = Utils.getProperty(localProperties, ApplicationEventListener.APPLICATION_EVENT_LISTENER, ApplicationEventListener.class);
    final Integer maxSessionsPerApp = Utils.getProperty(localProperties, TyrusWebSocketEngine.MAX_SESSIONS_PER_APP, Integer.class);
    final Integer maxSessionsPerRemoteAddr = Utils.getProperty(localProperties, TyrusWebSocketEngine.MAX_SESSIONS_PER_REMOTE_ADDR, Integer.class);
    final Boolean parallelBroadcastEnabled = Utils.getProperty(localProperties, TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, Boolean.class);
    final DebugContext.TracingType tracingType = Utils.getProperty(localProperties, TyrusWebSocketEngine.TRACING_TYPE, DebugContext.TracingType.class, DebugContext.TracingType.OFF);
    final DebugContext.TracingThreshold tracingThreshold = Utils.getProperty(localProperties, TyrusWebSocketEngine.TRACING_THRESHOLD, DebugContext.TracingThreshold.class, DebugContext.TracingThreshold.TRACE);

    webSocketEngine = TyrusWebSocketEngine.builder(this)
                        .incomingBufferSize(incomingBufferSize)
                        .clusterContext(clusterContext)
                        .applicationEventListener(applicationEventListener)
                        .maxSessionsPerApp(maxSessionsPerApp)
                        .maxSessionsPerRemoteAddr(maxSessionsPerRemoteAddr)
                        .parallelBroadcastEnabled(parallelBroadcastEnabled)
                        .tracingType(tracingType)
                        .tracingThreshold(tracingThreshold)
                        .build();
  }

  @Override
  public void register (Class<?> endpointClass)
    throws DeploymentException {

    webSocketEngine.register(endpointClass, contextPath);
  }

  @Override
  public void register (ServerEndpointConfig serverEndpointConfig)
    throws DeploymentException {

    webSocketEngine.register(mergeExtensions(serverEndpointConfig), contextPath);
  }

  private ServerEndpointConfig mergeExtensions (ServerEndpointConfig serverEndpointConfig) {

    if ((webSocketExtensionInstallers != null) && (webSocketExtensionInstallers.length > 0)) {
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

  @Override
  public WebSocketEngine getWebSocketEngine () {

    return webSocketEngine;
  }

  @Override
  public int getPort () {

    return ((networkListener != null) && (networkListener.getPort() > 0)) ? networkListener.getPort() : -1;
  }
}
