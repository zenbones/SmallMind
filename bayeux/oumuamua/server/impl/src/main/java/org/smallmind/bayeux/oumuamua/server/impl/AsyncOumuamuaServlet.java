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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.LongPollingConnection;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.LongPollingTransport;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.scribe.pen.LoggerManager;

public class AsyncOumuamuaServlet<V extends Value<V>> extends HttpServlet {

  private final ExecutorService executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
  private OumuamuaServer<V> server;
  private LongPollingConnection<V> connection;

  @Override
  public String getServletInfo () {

    return super.getServletInfo();
  }

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

  @Override
  public void destroy () {

    server.stop();
    executorService.shutdown();

    super.destroy();
  }

  private static class OumuamuaReadListener<V extends Value<V>> implements ReadListener {

    private final ExecutorService executorService;
    private final Server<V> server;
    private final LongPollingConnection<V> connection;
    private final AsyncContext asyncContext;
    private final ServletInputStream inputStream;
    private final byte[] contentBuffer;
    private int index = 0;

    public OumuamuaReadListener (ExecutorService executorService, Server<V> server, LongPollingConnection<V> connection, AsyncContext asyncContext, ServletInputStream inputStream, int contentBufferSize) {

      this.executorService = executorService;
      this.server = server;
      this.connection = connection;
      this.asyncContext = asyncContext;
      this.inputStream = inputStream;

      contentBuffer = new byte[contentBufferSize];
    }

    @Override
    public void onDataAvailable ()
      throws IOException {

      if (index == contentBuffer.length) {
        throw new IOException("Available data exceeds the declared content length");
      } else {

        int bytesRead;

        while (inputStream.isReady() && ((bytesRead = inputStream.read(contentBuffer, index, contentBuffer.length - index)) >= 0)) {
          index += bytesRead;
        }
      }
    }

    @Override
    public void onAllDataRead ()
      throws IOException {

      LoggerManager.getLogger(OumuamuaServlet.class).debug(() -> "<=" + new String(contentBuffer));

      Message<V>[] messages = server.getCodec().from(contentBuffer);

      executorService.submit(() -> connection.onMessages(asyncContext, messages));
    }

    @Override
    public void onError (Throwable throwable) {

      asyncContext.complete();

      LoggerManager.getLogger(OumuamuaReadListener.class).error(throwable);
    }
  }
}
