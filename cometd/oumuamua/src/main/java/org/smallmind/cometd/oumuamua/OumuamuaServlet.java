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
package org.smallmind.cometd.oumuamua;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cometd.bayeux.server.BayeuxServer;
import org.smallmind.cometd.oumuamua.transport.LongPollingTransport;

public class OumuamuaServlet extends HttpServlet {

  private OumuamuaServer oumuamuaServer;

  @Override
  public String getServletInfo () {

    return super.getServletInfo();
  }

  @Override
  public void init (ServletConfig servletConfig)
    throws ServletException {

    super.init(servletConfig);

    if ((oumuamuaServer = (OumuamuaServer)servletConfig.getServletContext().getAttribute(BayeuxServer.ATTRIBUTE)) == null) {
      throw new ServletException("Missing " + OumuamuaServer.class.getSimpleName() + " in the servlet context - was the " + OumuamuaServletContextListener.class.getSimpleName() + " installed?");
    } else {
      oumuamuaServer.start(servletConfig);
    }
  }

  @Override
  protected void doPost (HttpServletRequest request, HttpServletResponse response)
    throws IOException {

    LongPollingTransport longPollingTransport;

    if ((longPollingTransport = (LongPollingTransport)oumuamuaServer.getTransport("long-polling")) == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No long polling transport has been configured");
    } else {

      String contentLength;

      if (((contentLength = request.getHeader("Content-Length")) == null) || contentLength.isEmpty()) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing content length");
      } else {

        byte[] contentBuffer;
        int contentBufferSize = 0;

        try {
          contentBufferSize = Integer.parseInt(contentLength);
        } catch (NumberFormatException numberFormatException) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid content length");
        }

        if ((contentBufferSize <= 0) || (request.getInputStream().read(contentBuffer = new byte[contentBufferSize]) < contentBufferSize)) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read full content");
        } else {

          AsyncContext asyncContext = request.startAsync();

          asyncContext.setTimeout(0);
          longPollingTransport.createCarrier(oumuamuaServer, asyncContext).onMessage(new String(contentBuffer));
        }
      }
    }
  }

  @Override
  public void destroy () {

    oumuamuaServer.stop();

    super.destroy();
  }
}
