/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.LongPollingConnection;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.LongPollingTransport;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.ServletProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * HTTP servlet that accepts Bayeux long-polling requests, reads the full request body, hands the
 * decoded messages to the server, and completes the response asynchronously.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class OumuamuaServlet<V extends Value<V>> extends HttpServlet {

  private LongPollingConnection<V> connection;
  private OumuamuaServer<V> server;

  /**
   * Returns a human-readable description of the servlet, delegating to the base implementation.
   *
   * @return servlet info string from {@link HttpServlet#getServletInfo()}
   */
  @Override
  public String getServletInfo () {

    return super.getServletInfo();
  }

  /**
   * Initializes the servlet by locating the {@link OumuamuaServer} from the servlet context,
   * resolving the servlet protocol and long-polling transport, and starting the server.
   *
   * @param servletConfig the servlet configuration provided by the container
   * @throws ServletException if the server is absent from the context, the servlet protocol has
   *                          not been configured, or the long-polling transport is missing
   */
  @Override
  public void init (ServletConfig servletConfig)
    throws ServletException {

    super.init(servletConfig);

    if ((server = (OumuamuaServer<V>)servletConfig.getServletContext().getAttribute(Server.ATTRIBUTE)) == null) {
      throw new ServletException("Missing " + OumuamuaServer.class.getSimpleName() + " in the servlet context - was the " + OumuamuaServletContextListener.class.getSimpleName() + " installed?");
    } else {

      Protocol<V> servletProtocol;

      if ((servletProtocol = server.getProtocol(Protocols.SERVLET.getName())) == null) {
        throw new ServletException("No http protocol support has been configured");
      } else {

        LongPollingTransport<V> transport;

        if ((transport = (LongPollingTransport<V>)servletProtocol.getTransport(Transports.LONG_POLLING.getName())) == null) {
          throw new ServletException("No long polling transport support has been configured");
        } else {
          connection = new LongPollingConnection<>(transport, server);
          server.start(servletConfig);
        }
      }
    }
  }

  /**
   * Handles a Bayeux POST: validates the {@code Content-Length} header, reads the full body into
   * a buffer, decodes the messages with the server codec, and submits asynchronous processing.
   *
   * @param request  the inbound HTTP request carrying the serialized Bayeux message array
   * @param response used to send HTTP error codes when the request is malformed or unreadable
   * @throws IOException if writing an HTTP error response fails
   */
  @Override
  protected void doPost (HttpServletRequest request, HttpServletResponse response)
    throws IOException {

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
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read the full content");
      } else {

        LoggerManager.getLogger(LongPollingConnection.class).log(server.getMessageLogLevel(), () -> "<=" + new String(contentBuffer));

        try {

          Message<V>[] messages = server.getCodec().from(contentBuffer);

          ((ServletProtocol<V>)connection.getTransport().getProtocol()).onReceipt(messages);
          AsyncContext asyncContext = request.startAsync();
          asyncContext.setTimeout(0);

          server.getExecutorService().submit(() -> connection.onMessages(asyncContext, messages));
        } catch (IOException ioException) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, ioException.getMessage());
        }
      }
    }
  }

  /**
   * Reads bytes from the stream until the buffer is completely filled or EOF is reached.
   *
   * @param inputStream   the stream to read from; expected to contain exactly
   *                      {@code contentBuffer.length} bytes
   * @param contentBuffer pre-allocated destination whose length defines how many bytes to read
   * @return {@code true} if every byte was read successfully; {@code false} if EOF was encountered
   * before the buffer was full or an {@link IOException} occurred
   */
  private boolean readStream (InputStream inputStream, byte[] contentBuffer) {

    try {

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
    } catch (IOException ioException) {
      LoggerManager.getLogger(OumuamuaServlet.class).error(ioException);

      return false;
    }
  }

  /**
   * Stops the server and releases all resources when the servlet container unloads the servlet.
   */
  @Override
  public void destroy () {

    server.stop();

    super.destroy();
  }
}
