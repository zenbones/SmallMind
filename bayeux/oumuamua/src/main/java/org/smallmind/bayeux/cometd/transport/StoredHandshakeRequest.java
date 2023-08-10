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
package org.smallmind.bayeux.cometd.transport;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class StoredHandshakeRequest {

  private final Map<String, List<String>> headerMap;
  private final Map<String, List<String>> parameterMap;
  private final ServerEndpointConfig serverEndpointConfig;
  private final ServletContext servletContext;
  private final HttpSession httpSession;
  private final Principal userPrincipal;
  private final URI requestURI;
  private final String queryString;

  public StoredHandshakeRequest (ServerEndpointConfig serverEndpointConfig, ServletContext servletContext, HandshakeRequest handshakeRequest) {

    this.serverEndpointConfig = serverEndpointConfig;
    this.servletContext = servletContext;

    requestURI = handshakeRequest.getRequestURI();
    queryString = handshakeRequest.getQueryString();
    headerMap = handshakeRequest.getHeaders();
    parameterMap = handshakeRequest.getParameterMap();
    userPrincipal = handshakeRequest.getUserPrincipal();
    httpSession = (HttpSession)handshakeRequest.getHttpSession();
  }

  public ServerEndpointConfig getServerEndpointConfig () {

    return serverEndpointConfig;
  }

  public Principal getUserPrincipal () {

    return userPrincipal;
  }

  public URI getRequestURI () {

    return requestURI;
  }

  public String getQueryString () {

    return queryString;
  }

  public ServletContext getServletContext () {

    return servletContext;
  }

  public Map<String, List<String>> getHeaderMap () {

    return headerMap;
  }

  public Map<String, List<String>> getParameterMap () {

    return parameterMap;
  }

  public HttpSession getHttpSession () {

    return httpSession;
  }
}
