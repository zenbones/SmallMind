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

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import jakarta.websocket.CloseReason;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.Parameters;
import org.glassfish.grizzly.memory.ByteBufferArray;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyWriter;
import org.glassfish.tyrus.container.grizzly.client.TaskProcessor;
import org.glassfish.tyrus.core.CloseReasons;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.ReadHandler;
import org.glassfish.tyrus.spi.ServerContainer;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.UpgradeResponse;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * WebSocket {@link Filter} implementation, which supposed to be placed into a {@link FilterChain} right after HTTP
 * Filter: {@link HttpServerFilter}, {@link HttpClientFilter}; depending whether it's server or client side. The
 * <code>WebSocketFilter</code> handles websocket connection, handshake phases and, when receives a websocket frame -
 * redirects it to appropriate connection ({@link org.glassfish.tyrus.core.TyrusEndpointWrapper},
 * {@link org.glassfish.tyrus.core.TyrusWebSocket}) for processing.
 *
 * @author Alexey Stashok
 * @author Pavel Bucek
 */
public class TyrusGrizzlyServerFilter extends BaseFilter {

  private static final Attribute<org.glassfish.tyrus.spi.Connection> TYRUS_CONNECTION = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(TyrusGrizzlyServerFilter.class.getName() + ".Connection");
  private static final Attribute<TaskProcessor> TASK_PROCESSOR = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(TaskProcessor.class.getName() + ".TaskProcessor");

  private final ServerContainer serverContainer;
  private final Map<String, Object> tyrusUpgradeRequestProperties;
  private final String contextPath;

  // ------------------------------------------------------------ Constructors

  /**
   * Constructs a new {@link org.smallmind.web.grizzly.tyrus.TyrusGrizzlyServerFilter}.
   *
   * @param serverContainer server container.
   * @param contextPath     the context path of the deployed application. If the value is "" or "/", a request URI
   *                        "/a" will be divided into context path "" and url-pattern "/a".
   */
  public TyrusGrizzlyServerFilter (ServerContainer serverContainer, String contextPath, Map<String, Object> tyrusUpgradeRequestProperties) {

    this.serverContainer = serverContainer;
    this.contextPath = contextPath.endsWith("/") ? contextPath : contextPath + "/";
    this.tyrusUpgradeRequestProperties = tyrusUpgradeRequestProperties;
  }

  // ----------------------------------------------------- Methods from Filter

  private static UpgradeRequest createWebSocketRequest (final HttpContent requestContent, Map<String, Object> propertyMap) {

    final HttpRequestPacket requestPacket = (HttpRequestPacket)requestContent.getHttpHeader();

    Parameters parameters = new Parameters();

    parameters.setQuery(requestPacket.getQueryStringDC());
    parameters.setQueryStringEncoding(Charsets.UTF8_CHARSET);

    Map<String, String[]> parameterMap = new HashMap<>();

    for (String paramName : parameters.getParameterNames()) {
      parameterMap.put(paramName, parameters.getParameterValues(paramName));
    }

    final RequestContext requestContext =
      RequestContext.Builder.create()
        .requestURI(URI.create(requestPacket.getRequestURI()))
        .queryString(requestPacket.getQueryString())
        .parameterMap(parameterMap)
        .secure(requestPacket.isSecure())
        .remoteAddr(requestPacket.getRemoteAddress())
        .serverAddr(requestPacket.getLocalHost() == null ? requestPacket.getLocalAddress() : requestPacket.getLocalHost())
        .serverPort(requestPacket.getLocalPort())
        .tyrusProperties(propertyMap)
        .build();

    for (String name : requestPacket.getHeaders().names()) {
      for (String headerValue : requestPacket.getHeaders().values(name)) {

        final List<String> values = requestContext.getHeaders().get(name);
        if (values == null) {
          requestContext.getHeaders().put(name, Utils.parseHeaderValue(headerValue.strip()));
        } else {
          values.addAll(Utils.parseHeaderValue(headerValue.strip()));
        }
      }
    }

    return requestContext;
  }

  /**
   * Method handles Grizzly {@link Connection} close phase. Check if the {@link Connection} is a {@link
   * org.glassfish.tyrus.core.TyrusWebSocket}, if yes - tries to close the websocket gracefully (sending close frame)
   * and calls {@link org.glassfish.tyrus.core.TyrusWebSocket#onClose(org.glassfish.tyrus.core.frame.CloseFrame)}. If
   * the Grizzly {@link Connection} is not websocket - passes processing to the next filter in the chain.
   *
   * @param ctx {@link FilterChainContext}
   * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
   */
  @Override
  public NextAction handleClose (FilterChainContext ctx) {

    final org.glassfish.tyrus.spi.Connection connection = getConnection(ctx);
    if (connection != null) {
      TaskProcessor taskProcessor = getTaskProcessor(ctx);
      taskProcessor.processTask(
        new CloseTask(connection, CloseReasons.CLOSED_ABNORMALLY.getCloseReason(), ctx.getConnection()));
    }
    return ctx.getStopAction();
  }

  /**
   * Handle Grizzly {@link Connection} read phase. If the {@link Connection} has associated {@link
   * org.glassfish.tyrus.core.TyrusWebSocket} object (websocket connection), we check if websocket handshake has been
   * completed for this connection, if not - initiate/validate handshake. If handshake has been completed - parse
   * websocket {@link org.glassfish.tyrus.core.frame.Frame}s one by one and pass processing to appropriate {@link
   * org.glassfish.tyrus.core.TyrusWebSocket}: {@link org.glassfish.tyrus.core.TyrusEndpointWrapper} for server- and
   * client- side connections.
   *
   * @param ctx {@link FilterChainContext}
   * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
   * @throws IOException TODO
   */
  @Override
  @SuppressWarnings("unchecked")
  public NextAction handleRead (FilterChainContext ctx)
    throws IOException {
    // Get the parsed HttpContent (we assume prev. filter was HTTP)
    final HttpContent message = ctx.getMessage();

    final org.glassfish.tyrus.spi.Connection tyrusConnection = getConnection(ctx);

    LoggerManager.getLogger(TyrusGrizzlyServerFilter.class).trace("handleRead websocket: %s content-size=%s headers=\n%s", tyrusConnection, message.getContent().remaining(), message.getHttpHeader());

    if (tyrusConnection == null) {
      // Get the HTTP header
      final HttpHeader header = message.getHttpHeader();

      // If websocket is null - it means either non-websocket Connection
      if (!UpgradeRequest.WEBSOCKET.equalsIgnoreCase(header.getUpgrade())
            && message.getHttpHeader().isRequest()) {
        // if it's not a websocket connection - pass the processing to the next filter
        return ctx.getInvokeAction();
      }

      // https://github.com/eclipse-ee4j/tyrus/issues/737: each GrizzlyServerFilet & each path have different ATTR_NAME
      final String ATTR_NAME = "org.glassfish.tyrus.container.grizzly.WebSocketFilter.HANDSHAKE_PROCESSED." + contextPath;

      final AttributeHolder attributeHolder = ctx.getAttributes();
      if (attributeHolder != null) {
        final Object attribute = attributeHolder.getAttribute(ATTR_NAME);
        if (attribute != null) {
          // handshake was already performed on this context.
          return ctx.getInvokeAction();
        } else {
          attributeHolder.setAttribute(ATTR_NAME, true);
        }
      }
      // Handle handshake
      return handleHandshake(ctx, message);
    }

    // tyrusConnection is not null
    // this is websocket with the completed handshake
    if (message.getContent().hasRemaining()) {
      // get the frame(s) content

      Buffer buffer = message.getContent();
      message.recycle();
      final ReadHandler readHandler = tyrusConnection.getReadHandler();
      TaskProcessor taskProcessor = getTaskProcessor(ctx);
      if (!buffer.isComposite()) {
        taskProcessor.processTask(new ProcessTask(buffer.toByteBuffer(), readHandler));
      } else {
        final ByteBufferArray byteBufferArray = buffer.toByteBufferArray();
        final ByteBuffer[] array = byteBufferArray.getArray();

        for (int i = 0; i < byteBufferArray.size(); i++) {
          taskProcessor.processTask(new ProcessTask(array[i], readHandler));
        }

        byteBufferArray.recycle();
      }
    }
    return ctx.getStopAction();
  }

  private org.glassfish.tyrus.spi.Connection getConnection (FilterChainContext ctx) {

    return TYRUS_CONNECTION.get(ctx.getConnection());
  }

  // --------------------------------------------------------- Private Methods

  private TaskProcessor getTaskProcessor (FilterChainContext ctx) {

    return TASK_PROCESSOR.get(ctx.getConnection());
  }

  /**
   * Handle websocket handshake
   *
   * @param ctx     {@link FilterChainContext}
   * @param content HTTP message
   * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
   */
  private NextAction handleHandshake (final FilterChainContext ctx, HttpContent content) {

    final UpgradeRequest upgradeRequest = createWebSocketRequest(content, tyrusUpgradeRequestProperties);

    if (!upgradeRequest.getRequestURI().getPath().startsWith(contextPath)) {
      // the request is not for the deployed application
      return ctx.getInvokeAction();
    }

    final UpgradeResponse upgradeResponse = new TyrusUpgradeResponse();
    final WebSocketEngine.UpgradeInfo upgradeInfo = serverContainer.getWebSocketEngine().upgrade(upgradeRequest, upgradeResponse);

    switch (upgradeInfo.getStatus()) {
      case SUCCESS:
        final Connection<?> grizzlyConnection = ctx.getConnection();
        write(ctx, upgradeResponse);

        final org.glassfish.tyrus.spi.Connection connection = upgradeInfo.createConnection(new GrizzlyWriter(ctx.getConnection()), reason -> grizzlyConnection.close());

        TYRUS_CONNECTION.set(grizzlyConnection, connection);
        TASK_PROCESSOR.set(grizzlyConnection, new TaskProcessor());

        grizzlyConnection.addCloseListener((CloseListener<?, ?>)(closeable, type) -> {
          // close detected on connection
          connection.close(CloseReasons.GOING_AWAY.getCloseReason());
          // might not be necessary, connection is going to be recycled/freed anyway
          TYRUS_CONNECTION.remove(grizzlyConnection);
          TASK_PROCESSOR.remove(grizzlyConnection);
        });

        return ctx.getStopAction();

      case HANDSHAKE_FAILED:
        write(ctx, upgradeResponse);
        content.recycle();
        return ctx.getStopAction();

      case NOT_APPLICABLE:
        writeTraceHeaders(ctx, upgradeResponse);
        return ctx.getInvokeAction();
    }

    return ctx.getStopAction();
  }

  private void write (FilterChainContext ctx, UpgradeResponse response) {

    final HttpResponsePacket responsePacket = ((HttpRequestPacket)((HttpContent)ctx.getMessage()).getHttpHeader()).getResponse();
    String reasonPhrase;

    responsePacket.setProtocol(Protocol.HTTP_1_1);
    responsePacket.setStatus(response.getStatus());

    if ((reasonPhrase = response.getReasonPhrase()) != null) {
      responsePacket.setReasonPhrase(reasonPhrase);
    }

    for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
      responsePacket.setHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
    }

    ctx.write(HttpContent.builder(responsePacket).build());
  }

  private void writeTraceHeaders (FilterChainContext ctx, UpgradeResponse upgradeResponse) {

    final HttpResponsePacket responsePacket = ((HttpRequestPacket)((HttpContent)ctx.getMessage()).getHttpHeader()).getResponse();

    for (Map.Entry<String, List<String>> entry : upgradeResponse.getHeaders().entrySet()) {
      if (entry.getKey().contains(UpgradeResponse.TRACING_HEADER_PREFIX)) {
        responsePacket.setHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
      }
    }
  }

  private static class ProcessTask extends TaskProcessor.Task {

    private final ByteBuffer buffer;
    private final ReadHandler readHandler;

    private ProcessTask (ByteBuffer buffer, ReadHandler readHandler) {

      this.buffer = buffer;
      this.readHandler = readHandler;
    }

    @Override
    public void execute () {

      try {
        readHandler.handle(buffer);
      } catch (RejectedExecutionException rejectedExecutionException) {
        LoggerManager.getLogger(TyrusGrizzlyServerFilter.class).warn(rejectedExecutionException.getMessage());
      }
    }
  }

  private static class CloseTask extends TaskProcessor.Task {

    private final org.glassfish.tyrus.spi.Connection connection;
    private final CloseReason closeReason;
    private final Connection<?> grizzlyConnection;

    private CloseTask (org.glassfish.tyrus.spi.Connection connection, CloseReason closeReason, Connection<?> grizzlyConnection) {

      this.connection = connection;
      this.closeReason = closeReason;
      this.grizzlyConnection = grizzlyConnection;
    }

    @Override
    public void execute () {

      connection.close(closeReason);
      TYRUS_CONNECTION.remove(grizzlyConnection);
      TASK_PROCESSOR.remove(grizzlyConnection);
    }
  }
}
