/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.ContentType;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.wsadl.model.Application;

public class WsadlHttpHandler extends HttpHandler {

  private final TyrusWebSocketEngine engine;
  private final HttpHandler staticHttpHandler;

  private JAXBContext wsadlJaxbContext;

  public WsadlHttpHandler (TyrusWebSocketEngine engine, HttpHandler staticHttpHandler) {

    this.engine = engine;
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

      getWsadlJaxbContext().createMarshaller().marshal(engine.getWsadlApplication(), response.getWriter());
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
