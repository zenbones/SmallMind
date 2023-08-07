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

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getProtocol();
  }

  @Override
  public String getURL () {

    HttpServletRequest request;
    String query;

    return ((request = asyncWindow.getRequest()) == null) ? null : ((query = request.getQueryString()) == null) ? request.getRequestURL().toString() : request.getRequestURL().append("?").append(query).toString();
  }

  @Override
  public List<Locale> getLocales () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? List.of(Locale.getDefault()) : Collections.list(request.getLocales());
  }

  @Override
  public boolean isSecure () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) != null) && request.isSecure();
  }

  @Override
  public Principal getUserPrincipal () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getUserPrincipal();
  }

  @Override
  public boolean isUserInRole (String role) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) != null) && request.isUserInRole(role);
  }

  @Override
  public InetSocketAddress getRemoteAddress () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : new InetSocketAddress(request.getRemoteHost(), request.getRemotePort());
  }

  @Override
  public InetSocketAddress getLocalAddress () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : new InetSocketAddress(request.getLocalName(), request.getLocalPort());
  }

  @Override
  public String getContextPath () {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getContextPath();
  }

  @Override
  public String getContextInitParameter (String name) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getServletContext().getInitParameter(name);
  }

  @Override
  public Object getContextAttribute (String name) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getServletContext().getAttribute(name);
  }

  @Override
  public String getHeader (String name) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getHeader(name);
  }

  @Override
  public List<String> getHeaderValues (String name) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? Collections.emptyList() : Collections.list(request.getHeaders(name));
  }

  @Override
  public String getParameter (String name) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getParameter(name);
  }

  @Override
  public List<String> getParameterValues (String name) {

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? Collections.emptyList() : Arrays.asList(request.getParameterValues(name));
  }

  @Override
  public String getCookie (String name) {

    HttpServletRequest request;

    if ((request = asyncWindow.getRequest()) != null) {

      Cookie[] cookies = request.getCookies();

      if (cookies != null) {
        for (Cookie cookie : cookies) {
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

    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : request.getAttribute(name);
  }

  @Override
  public String getHttpSessionId () {

    HttpSession httpSession;
    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : ((httpSession = request.getSession(false)) == null) ? null : httpSession.getId();
  }

  @Override
  public Object getHttpSessionAttribute (String name) {

    HttpSession httpSession;
    HttpServletRequest request;

    return ((request = asyncWindow.getRequest()) == null) ? null : ((httpSession = request.getSession(false)) == null) ? null : httpSession.getAttribute(name);
  }

  @Override
  public void setHttpSessionAttribute (String name, Object value) {

    HttpServletRequest request;

    if ((request = asyncWindow.getRequest()) == null) {
      throw new IllegalStateException("No http session was created");
    } else {

      HttpSession httpSession;

      if ((httpSession = request.getSession(false)) == null) {
        throw new IllegalStateException("No http session was created");
      } else {
        httpSession.setAttribute(name, value);
      }
    }
  }

  @Override
  public void invalidateHttpSession () {

    HttpServletRequest request;

    if ((request = asyncWindow.getRequest()) != null) {

      HttpSession httpSession;

      if ((httpSession = request.getSession(false)) != null) {
        httpSession.invalidate();
      }
    }
  }
}
