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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
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
 * Async servlet endpoint that receives Bayeux messages over long-polling HTTP.
 *
 * @param <V> value representation
 */
public class AsyncOumuamuaServlet<V extends Value<V>> extends HttpServlet {

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private OumuamuaServer<V> server;
  private LongPollingConnection<V> connection;

  /**
   * @return servlet information string from the parent implementation
   */
  @Override
  public String getServletInfo () {

    return super.getServletInfo();
  }

  /**
   * Initializes the servlet, wiring the Bayeux server, protocol, and transport.
   *
   * @param servletConfig servlet configuration
   * @throws ServletException if the server, protocol, or transport is missing
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
   * Handles inbound Bayeux envelopes posted by clients using async IO.
   *
   * @param request  http request carrying the payload
   * @param response http response for error handling
   * @throws IOException if the payload cannot be read
   */
  @Override
  protected void doPost (HttpServletRequest request, HttpServletResponse response)
    throws IOException {

    String contentLength;

    if (((contentLength = request.getHeader("Content-Length")) == null) || contentLength.isEmpty()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing content length");
    } else {

      int contentBufferSize = 0;

      try {
        contentBufferSize = Integer.parseInt(contentLength);
      } catch (NumberFormatException numberFormatException) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid content length");
      }

      if (contentBufferSize <= 0) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read the full content");
      } else {

        AsyncContext asyncContext = request.startAsync();
        ServletInputStream inputStream = request.getInputStream();

        asyncContext.setTimeout(0);
        inputStream.setReadListener(new OumuamuaReadListener<>(executorService, server, connection, asyncContext, inputStream, contentBufferSize));
      }
    }
  }

  /**
   * Shuts down the server and executor when the servlet is destroyed.
   */
  @Override
  public void destroy () {

    server.stop();
    executorService.shutdown();

    super.destroy();
  }

  /**
   * Asynchronous read listener that consumes the request body and forwards messages. Callbacks are serialized, and the
   * decoded messages are handed to the connection exactly once, because a container may report all data read before
   * the listener has consumed an already buffered body and then report it again (Grizzly does both).
   *
   * @param <V> value representation
   */
  private static class OumuamuaReadListener<V extends Value<V>> implements ReadListener {

    private final ExecutorService executorService;
    private final OumuamuaServer<V> server;
    private final LongPollingConnection<V> connection;
    private final AsyncContext asyncContext;
    private final ServletInputStream inputStream;
    private final byte[] contentBuffer;
    private int index = 0;
    private boolean delivered = false;

    /**
     * Creates a read listener to accumulate the incoming payload.
     *
     * @param executorService   executor used to process messages
     * @param server            owning server
     * @param connection        long-poll connection used to deliver responses
     * @param asyncContext      async context for this request
     * @param inputStream       input stream for the request body
     * @param contentBufferSize declared content length
     */
    public OumuamuaReadListener (ExecutorService executorService, OumuamuaServer<V> server, LongPollingConnection<V> connection, AsyncContext asyncContext, ServletInputStream inputStream, int contentBufferSize) {

      this.executorService = executorService;
      this.server = server;
      this.connection = connection;
      this.asyncContext = asyncContext;
      this.inputStream = inputStream;

      contentBuffer = new byte[contentBufferSize];
    }

    /**
     * Reads available data into the content buffer as it arrives.
     *
     * @throws IOException if more data is received than expected
     */
    @Override
    public synchronized void onDataAvailable ()
      throws IOException {

      readAvailableContent();
    }

    /**
     * Triggered once the entire payload has arrived; drains any content not yet read, then deserializes and processes
     * the messages. Repeated invocations after the messages have been handed off are ignored.
     *
     * @throws IOException if the content is shorter than declared or cannot be parsed
     */
    @Override
    public synchronized void onAllDataRead ()
      throws IOException {

      if (!delivered) {
        readAvailableContent();

        if (index == contentBuffer.length) {

          Message<V>[] messages;

          LoggerManager.getLogger(OumuamuaServlet.class).log(server.getMessageLogLevel(), () -> "<=" + new String(contentBuffer));

          messages = server.getCodec().from(contentBuffer);

          ((ServletProtocol<V>)connection.getTransport().getProtocol()).onReceipt(messages);

          delivered = true;
          executorService.submit(() -> connection.onMessages(asyncContext, messages));
        } else if (inputStream.isFinished()) {
          throw new IOException("Received fewer bytes(" + index + ") than the declared content length(" + contentBuffer.length + ")");
        }
      }
    }

    /**
     * Reads whatever content the stream can supply without blocking, up to the declared content length.
     *
     * @throws IOException if more data is received than expected
     */
    private void readAvailableContent ()
      throws IOException {

      int bytesRead;

      while ((index < contentBuffer.length) && inputStream.isReady() && ((bytesRead = inputStream.read(contentBuffer, index, contentBuffer.length - index)) >= 0)) {
        index += bytesRead;
      }

      if ((index == contentBuffer.length) && inputStream.isReady() && (inputStream.read() >= 0)) {
        throw new IOException("Available data exceeds the declared content length");
      }
    }

    /**
     * Handles read errors by logging the failure and, unless the messages were already handed to the connection (which
     * then owns completion), completing the async context.
     *
     * @param throwable encountered error
     */
    @Override
    public synchronized void onError (Throwable throwable) {

      if (!delivered) {
        asyncContext.complete();
      }

      LoggerManager.getLogger(OumuamuaReadListener.class).error(throwable);
    }
  }
}
