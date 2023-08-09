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
package org.smallmind.bayeux.cometd.context;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.bayeux.cometd.transport.StoredHandshakeRequest;

public class OumuamuaWebsocketContext implements BayeuxContext {

  private final StoredHandshakeRequest storedHandshakeRequest;

  public OumuamuaWebsocketContext (StoredHandshakeRequest storedHandshakeRequest) {

    this.storedHandshakeRequest = storedHandshakeRequest;
  }

  @Override
  public String getProtocol () {

    return "HTTP/1.1";
  }

  @Override
  public String getURL () {

    return (storedHandshakeRequest.getQueryString() == null) ? storedHandshakeRequest.getRequestURI().toString() : storedHandshakeRequest.getRequestURI() + "?" + storedHandshakeRequest.getQueryString();
  }

  @Override
  public List<Locale> getLocales () {

    List<Locale> localeList = (List<Locale>)storedHandshakeRequest.getServerEndpointConfig().getUserProperties().get("javax.websocket.upgrade.locales");

    return ((localeList == null) || localeList.isEmpty()) ? List.of(Locale.getDefault()) : localeList;
  }

  @Override
  public boolean isSecure () {

    String scheme = storedHandshakeRequest.getRequestURI().getScheme();

    return "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme);
  }

  @Override
  public Principal getUserPrincipal () {

    return storedHandshakeRequest.getUserPrincipal();
  }

  @Override
  public boolean isUserInRole (String role) {

    return false;
  }

  @Override
  public InetSocketAddress getRemoteAddress () {

    return (InetSocketAddress)storedHandshakeRequest.getServerEndpointConfig().getUserProperties().get("javax.websocket.endpoint.remoteAddress");
  }

  @Override
  public InetSocketAddress getLocalAddress () {

    return (InetSocketAddress)storedHandshakeRequest.getServerEndpointConfig().getUserProperties().get("javax.websocket.endpoint.localAddress");
  }

  @Override
  public String getContextPath () {

    return storedHandshakeRequest.getServletContext().getContextPath();
  }

  @Override
  public String getContextInitParameter (String name) {

    return storedHandshakeRequest.getServletContext().getInitParameter(name);
  }

  @Override
  public Object getContextAttribute (String name) {

    return storedHandshakeRequest.getServletContext().getAttribute(name);
  }

  @Override
  public String getHeader (String name) {

    List<String> values;

    return (((values = storedHandshakeRequest.getHeaderMap().get(name)) == null) || values.isEmpty()) ? null : values.get(0);
  }

  @Override
  public List<String> getHeaderValues (String name) {

    return storedHandshakeRequest.getHeaderMap().get(name);
  }

  @Override
  public String getParameter (String name) {

    List<String> values;

    return (((values = storedHandshakeRequest.getParameterMap().get(name)) == null) || values.isEmpty()) ? null : values.get(0);
  }

  @Override
  public List<String> getParameterValues (String name) {

    return storedHandshakeRequest.getParameterMap().get(name);
  }

  @Override
  public String getCookie (String name) {

    List<String> cookieList;

    if (((cookieList = storedHandshakeRequest.getHeaderMap().get("Cookie")) != null) && (!cookieList.isEmpty())) {
      for (String cookieValue : cookieList) {
        for (HttpCookie cookie : HttpCookie.parse(cookieValue)) {
          if (cookie.getName().equals(name)) {
            return cookie.getValue();
          }
        }
      }
    }

    return null;
  }

  @Override
  public Object getRequestAttribute (String name) {

    return null;
  }

  @Override
  public String getHttpSessionId () {

    return (storedHandshakeRequest.getHttpSession() == null) ? null : storedHandshakeRequest.getHttpSession().getId();
  }

  @Override
  public Object getHttpSessionAttribute (String name) {

    return (storedHandshakeRequest.getHttpSession() == null) ? null : storedHandshakeRequest.getHttpSession().getAttribute(name);
  }

  @Override
  public void setHttpSessionAttribute (String name, Object value) {

    if (storedHandshakeRequest.getHttpSession() != null) {
      storedHandshakeRequest.getHttpSession().setAttribute(name, value);
    }
  }

  @Override
  public void invalidateHttpSession () {

    if (storedHandshakeRequest.getHttpSession() != null) {
      storedHandshakeRequest.getHttpSession().invalidate();
    }
  }
}
