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
package org.smallmind.web.grizzly;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.ServletRequestAttributes;

// Handles some exception cases the Spring's version of the same class would otherwise spew into the log
public class GrizzlyRequestContextListener implements ServletRequestListener {

  private static final String REQUEST_ATTRIBUTES_ATTRIBUTE = RequestContextListener.class.getName() + ".REQUEST_ATTRIBUTES";

  @Override
  public void requestInitialized (ServletRequestEvent requestEvent) {

    if (!(requestEvent.getServletRequest() instanceof HttpServletRequest)) {
      throw new IllegalArgumentException(
        "Request is not an HttpServletRequest: " + requestEvent.getServletRequest());
    }

    HttpServletRequest request = (HttpServletRequest)requestEvent.getServletRequest();
    ServletRequestAttributes attributes = new ServletRequestAttributes(request);

    request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
    LocaleContextHolder.setLocale(request.getLocale());
    RequestContextHolder.setRequestAttributes(attributes);
  }

  @Override
  public void requestDestroyed (ServletRequestEvent requestEvent) {

    ServletRequestAttributes attributes = null;

    try {

      Object reqAttr = requestEvent.getServletRequest().getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE);

      if (reqAttr instanceof ServletRequestAttributes) {
        attributes = (ServletRequestAttributes)reqAttr;
      }
    } catch (Exception exception) {
      // Nothing to do here
    }

    RequestAttributes threadAttributes = RequestContextHolder.getRequestAttributes();

    if (threadAttributes != null) {
      // We're assumably within the original request thread...
      LocaleContextHolder.resetLocaleContext();
      RequestContextHolder.resetRequestAttributes();
      if (attributes == null && threadAttributes instanceof ServletRequestAttributes) {
        attributes = (ServletRequestAttributes)threadAttributes;
      }
    }
    if (attributes != null) {
      attributes.requestCompleted();
    }
  }
}
