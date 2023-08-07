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
package org.smallmind.cometd.oumuamua.context;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.cometd.bayeux.server.BayeuxContext;
import org.smallmind.cometd.oumuamua.transport.AsyncWindow;

public class OumuamuaLongPollingContext implements BayeuxContext {

  private final AsyncWindow asyncWindow;

  public OumuamuaLongPollingContext (AsyncWindow asyncWindow) {

    this.asyncWindow = asyncWindow;
  }

  @Override
  public String getProtocol () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? protocol : (protocol = request.getProtocol());
  }

  @Override
  public String getURL () {

    String query;

    return ((query = request.getQueryString()) == null) ? request.getRequestURL().toString() : request.getRequestURL().append("?").append(query).toString();
  }

  @Override
  public List<Locale> getLocales () {

    return Collections.list(request.getLocales());
  }

  @Override
  public boolean isSecure () {

    return request.isSecure();
  }

  @Override
  public Principal getUserPrincipal () {

    return request.getUserPrincipal();
  }

  @Override
  public boolean isUserInRole (String role) {

    return request.isUserInRole(role);
  }

  @Override
  public InetSocketAddress getRemoteAddress () {

    return new InetSocketAddress(request.getRemoteHost(), request.getRemotePort());
  }

  @Override
  public InetSocketAddress getLocalAddress () {

    return new InetSocketAddress(request.getLocalName(), request.getLocalPort());
  }

  @Override
  public String getContextPath () {

    return request.getContextPath();
  }

  @Override
  public String getContextInitParameter (String name) {

    return request.getServletContext().getInitParameter(name);
  }

  @Override
  public Object getContextAttribute (String name) {

    return request.getServletContext().getAttribute(name);
  }

  @Override
  public String getHeader (String name) {

    return request.getHeader(name);
  }

  @Override
  public List<String> getHeaderValues (String name) {

    return Collections.list(request.getHeaders(name));
  }

  @Override
  public String getParameter (String name) {

    return request.getParameter(name);
  }

  @Override
  public List<String> getParameterValues (String name) {

    return Arrays.asList(request.getParameterValues(name));
  }

  @Override
  public String getCookie (String name) {

    Cookie[] cookies = request.getCookies();

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {

          return cookie.getValue();
        }
      }
    }

    return null;
  }

  @Override
  public Object getRequestAttribute (String name) {

    return request.getAttribute(name);
  }

  @Override
  public String getHttpSessionId () {

    HttpSession httpSession;

    return ((httpSession = request.getSession(false)) == null) ? null : httpSession.getId();
  }

  @Override
  public Object getHttpSessionAttribute (String name) {

    HttpSession httpSession;

    return ((httpSession = request.getSession(false)) == null) ? null : httpSession.getAttribute(name);
  }

  @Override
  public void setHttpSessionAttribute (String name, Object value) {

    HttpSession httpSession;

    if ((httpSession = request.getSession(false)) == null) {
      throw new IllegalStateException("No http session was created");
    } else {
      httpSession.setAttribute(name, value);
    }
  }

  @Override
  public void invalidateHttpSession () {

    HttpSession httpSession;

    if ((httpSession = request.getSession(false)) != null) {
      httpSession.invalidate();
    }
  }
}
