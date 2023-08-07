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
import java.io.InputStream;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.smallmind.cometd.oumuamua.logging.NodeRecord;
import org.smallmind.cometd.oumuamua.transport.CarrierType;
import org.smallmind.cometd.oumuamua.transport.LongPollingCarrier;
import org.smallmind.cometd.oumuamua.transport.LongPollingTransport;
import org.smallmind.cometd.oumuamua.transport.OumuamuaCarrier;
import org.smallmind.cometd.oumuamua.transport.WebSocketEndpoint;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;

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

        if ((contentBufferSize <= 0) || (!readStream(request.getInputStream(), contentBuffer = new byte[contentBufferSize]))) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read full content");
        } else {

          OumuamuaCarrier carrier;
          OumuamuaServerSession serverSession;
          JsonNode messageConglomerate = JsonCodec.readAsJsonNode(new String(contentBuffer));
          String clientId = null;

          for (JsonNode messageNode : messageConglomerate) {
            if (JsonNodeType.OBJECT.equals(messageNode.getNodeType()) && messageNode.has(Message.CLIENT_ID_FIELD)) {
              clientId = messageNode.get(Message.CLIENT_ID_FIELD).asText();
              break;
            }
          }

          if (clientId == null) {
            serverSession = new OumuamuaServerSession(oumuamuaServer, longPollingTransport, carrier = longPollingTransport.createCarrier(oumuamuaServer), false, null, oumuamuaServer.getConfiguration().getMaximumMessageQueueSize(), oumuamuaServer.getConfiguration().getMaximumUndeliveredLazyMessageCount());
            ((LongPollingCarrier)carrier).setServerSession(serverSession);
            oumuamuaServer.addSession(serverSession);

            respond(request, response, (LongPollingCarrier)carrier, messageConglomerate);
          } else if ((serverSession = (OumuamuaServerSession)oumuamuaServer.getSession(clientId)) == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown client id");
          } else if ((carrier = serverSession.getCarrier()) == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Session is invalid");
          } else if (!CarrierType.LONG_POLLING.equals(carrier.getType())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect transport for this session");
          } else {
            respond(request, response, (LongPollingCarrier)carrier, messageConglomerate);
          }
        }
      }
    }
  }

  private void respond (HttpServletRequest request, HttpServletResponse response, LongPollingCarrier carrier, JsonNode messageConglomerate) {

    System.out.println("<=" + messageConglomerate);
    LoggerManager.getLogger(WebSocketEndpoint.class).debug(new NodeRecord(messageConglomerate, true));

    AsyncContext asyncContext = request.startAsync();

    asyncContext.setTimeout(0);
    carrier.onMessage(asyncContext, messageConglomerate);
  }

  private boolean readStream (InputStream inputStream, byte[] contentBuffer)
    throws IOException {

    int totalBytesRead = 0;
    int bytesRead;

    while (totalBytesRead < contentBuffer.length) {
      if ((bytesRead = inputStream.read(contentBuffer, totalBytesRead, contentBuffer.length - totalBytesRead)) < 0) {
        return false;
      } else {
        totalBytesRead += bytesRead;
      }
    }

    return true;
  }

  @Override
  public void destroy () {

    oumuamuaServer.stop();

    super.destroy();
  }
}
