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

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.cometd.bayeux.server.BayeuxContext;

public class OumuamuaLocalContext implements BayeuxContext {

  @Override
  public String getProtocol () {

    return "LOCAL";
  }

  @Override
  public String getURL () {

    return null;
  }

  @Override
  public List<Locale> getLocales () {

    return List.of(Locale.getDefault());
  }

  @Override
  public boolean isSecure () {

    return true;
  }

  @Override
  public Principal getUserPrincipal () {

    return null;
  }

  @Override
  public boolean isUserInRole (String role) {

    return false;
  }

  @Override
  public InetSocketAddress getRemoteAddress () {

    return null;
  }

  @Override
  public InetSocketAddress getLocalAddress () {

    return null;
  }

  @Override
  public String getContextPath () {

    return null;
  }

  @Override
  public String getContextInitParameter (String name) {

    return null;
  }

  @Override
  public Object getContextAttribute (String name) {

    return null;
  }

  @Override
  public String getHeader (String name) {

    return null;
  }

  @Override
  public List<String> getHeaderValues (String name) {

    return Collections.emptyList();
  }

  @Override
  public String getParameter (String name) {

    return null;
  }

  @Override
  public List<String> getParameterValues (String name) {

    return Collections.emptyList();
  }

  @Override
  public String getCookie (String name) {

    return null;
  }

  @Override
  public Object getRequestAttribute (String name) {

    return null;
  }

  @Override
  public String getHttpSessionId () {

    return null;
  }

  @Override
  public Object getHttpSessionAttribute (String name) {

    return null;
  }

  @Override
  public void setHttpSessionAttribute (String name, Object value) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void invalidateHttpSession () {

    throw new UnsupportedOperationException();
  }
}
