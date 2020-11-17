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
package org.smallmind.web.grizzly.tyrus;

import java.io.IOException;
import javax.websocket.DeploymentException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.util.ContentType;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.wsadl.model.Application;
import org.smallmind.web.grizzly.GrizzlyInitializationException;
import org.smallmind.web.grizzly.WebSocketExtensionInstaller;

public class TyrusWebSocketAddOn implements AddOn {

  private final WebSocketExtensionInstaller[] webSocketExtensionInstallers;
  private final ServerConfiguration serverConfiguration;
  private final WebappContext webappContext;
  private final HttpHandler staticHttpHandler;
  private final String contextPath;
  private final boolean includeWsadlSupport;
  private TyrusGrizzlyServerContainer serverContainer;

  public TyrusWebSocketAddOn (ServerConfiguration serverConfiguration, WebappContext webappContext, String contextPath, boolean includeWsadlSupport, HttpHandler staticHttpHandler, WebSocketExtensionInstaller... webSocketExtensionInstallers) {

    this.webSocketExtensionInstallers = webSocketExtensionInstallers;
    this.serverConfiguration = serverConfiguration;
    this.webappContext = webappContext;
    this.contextPath = contextPath;
    this.includeWsadlSupport = includeWsadlSupport;
    this.staticHttpHandler = staticHttpHandler;
  }

  public void start (int port)
    throws IOException, DeploymentException {

    serverContainer.start(contextPath, port);
  }

  public void doneDeployment () {

    serverContainer.doneDeployment();
  }

  public void stop () {

    serverContainer.stop();
  }

  @Override
  public void setup (NetworkListener networkListener, FilterChainBuilder builder) {

    int httpServerFilterIndex;

    serverContainer = new TyrusGrizzlyServerContainer(networkListener, contextPath, webSocketExtensionInstallers);

    if ((httpServerFilterIndex = builder.indexOfType(HttpServerFilter.class)) < 0) {
      throw new GrizzlyInitializationException("Missing http servlet filter in the available filter chain");
    } else {

      networkListener.getKeepAlive().setIdleTimeoutInSeconds(-1);

      if (includeWsadlSupport) {
        serverConfiguration.addHttpHandler(new WsadlHttpHandler((TyrusWebSocketEngine)serverContainer.getWebSocketEngine(), staticHttpHandler));
      }

      // Insert the WebSocketFilter right before HttpServerFilter
      builder.add(httpServerFilterIndex, new TyrusGrizzlyServerFilter(serverContainer.getWebSocketEngine(), contextPath));
      webappContext.setAttribute("javax.websocket.server.ServerContainer", serverContainer);
    }
  }

  private static class WsadlHttpHandler extends HttpHandler {

    private final TyrusWebSocketEngine tyrusWebSocketEngine;
    private final HttpHandler staticHttpHandler;

    private JAXBContext wsadlJaxbContext;

    private WsadlHttpHandler (TyrusWebSocketEngine tyrusWebSocketEngine, HttpHandler staticHttpHandler) {

      this.tyrusWebSocketEngine = tyrusWebSocketEngine;
      this.staticHttpHandler = staticHttpHandler;
    }

    private synchronized JAXBContext getWsadlJaxbContext ()
      throws JAXBException {

      if (wsadlJaxbContext == null) {
        wsadlJaxbContext = JAXBContext.newInstance(Application.class.getPackage().getName());
      }
      return wsadlJaxbContext;
    }

    @Override
    public void service (Request request, Response response)
      throws Exception {

      if (request.getMethod().equals(Method.GET) && request.getRequestURI().endsWith("application.wsadl")) {

        getWsadlJaxbContext().createMarshaller().marshal(tyrusWebSocketEngine.getWsadlApplication(), response.getWriter());
        response.setStatus(200);
        response.setContentType(ContentType.newContentType("application/wsadl+xml"));

        return;
      }

      if (staticHttpHandler != null) {
        staticHttpHandler.service(request, response);
      } else {
        response.sendError(404);
      }
    }
  }
}

