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
package org.smallmind.web.websocket.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Decoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.PongMessage;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.smallmind.nutsnbolts.http.HTTPCodec;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.web.websocket.CloseCode;
import org.smallmind.web.websocket.CloseListener;
import org.smallmind.web.websocket.ConnectionState;
import org.smallmind.web.websocket.WebSocket;
import org.smallmind.web.websocket.WebSocketException;

/**
 * Client-side {@link Session} backed by {@link WebSocket}, handling message dispatch, decoders, and endpoint callbacks.
 */
public class SessionImpl implements Session, CloseListener {

  private final WebSocket webSocket;
  private final WebSocketContainer container;
  private final Endpoint endpoint;
  private final ClientEndpointConfig endpointConfig;
  private final AtomicReference<MessageHandler> textMessageHandlerRef = new AtomicReference<>();
  private final AtomicReference<MessageHandler> binaryMessageHandlerRef = new AtomicReference<>();
  private final AtomicReference<MessageHandler> pongMessageHandlerRef = new AtomicReference<>();
  private final HashMap<Class<? extends Decoder>, Decoder> decoderInstanceMap = new HashMap<>();
  private final String id = SnowflakeId.newInstance().generateHexEncoding();

  /**
   * Establishes a client session for the given endpoint and configuration.
   *
   * @param container      the owning container
   * @param uri            the websocket URI
   * @param endpoint       the endpoint instance to receive callbacks
   * @param endpointConfig the endpoint configuration
   * @throws IOException              if the socket cannot be opened
   * @throws NoSuchAlgorithmException if handshake hashing fails
   * @throws WebSocketException       if the handshake or URI is invalid
   */
  public SessionImpl (WebSocketContainer container, URI uri, final Endpoint endpoint, ClientEndpointConfig endpointConfig)
    throws IOException, NoSuchAlgorithmException, WebSocketException {

    String[] preferredSubProtocols = null;

    this.container = container;
    this.endpoint = endpoint;
    this.endpointConfig = endpointConfig;

    if ((endpointConfig.getPreferredSubprotocols() != null) && (!endpointConfig.getPreferredSubprotocols().isEmpty())) {
      preferredSubProtocols = new String[endpointConfig.getPreferredSubprotocols().size()];
      endpointConfig.getPreferredSubprotocols().toArray(preferredSubProtocols);
    }

    webSocket = new WebSocket(uri, new ConfiguratorHandshakeListener(endpointConfig.getConfigurator()), endpointConfig.getExtensions().toArray(new Extension[0]), preferredSubProtocols) {

      @Override
      public void onError (Exception exception) {

        endpoint.onError(SessionImpl.this, exception);
      }

      @Override
      public void onPong (byte[] message) {

        MessageHandler pongMessageHandler;

        if ((pongMessageHandler = pongMessageHandlerRef.get()) != null) {
          if (pongMessageHandler instanceof MessageHandler.Whole) {
            ((MessageHandler.Whole)pongMessageHandler).onMessage(message);
          } else {
            ((MessageHandler.Partial)pongMessageHandler).onMessage(message, true);
          }
        }
      }

      @Override
      public void onText (String message) {

        MessageHandler textMessageHandler;

        if ((textMessageHandler = textMessageHandlerRef.get()) != null) {
          if (textMessageHandler instanceof MessageHandler.Whole) {
            ((MessageHandler.Whole)textMessageHandler).onMessage(message);
          } else {
            ((MessageHandler.Partial)textMessageHandler).onMessage(message, true);
          }
        }
      }

      @Override
      public void onBinary (byte[] message) {

        MessageHandler binaryMessageHandler;

        if ((binaryMessageHandler = binaryMessageHandlerRef.get()) != null) {
          if (binaryMessageHandler instanceof MessageHandler.Whole) {
            ((MessageHandler.Whole)binaryMessageHandler).onMessage(message);
          } else {
            ((MessageHandler.Partial)binaryMessageHandler).onMessage(message, true);
          }
        }
      }
    };

    webSocket.setMaxIdleTimeoutMilliseconds(container.getDefaultMaxSessionIdleTimeout());
    webSocket.setMaxTextBufferSize(container.getDefaultMaxTextMessageBufferSize());
    webSocket.setMaxBinaryBufferSize(container.getDefaultMaxBinaryMessageBufferSize());

    webSocket.addCloseListener(this);
    endpoint.onOpen(this, endpointConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebSocketContainer getContainer () {

    return container;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addMessageHandler (MessageHandler handler)
    throws IllegalStateException {

    if (handler instanceof MessageHandler.Whole) {
      addMessageHandler(getTypeParameter(handler.getClass(), MessageHandler.Whole.class), (MessageHandler.Whole)handler);
    }
    if (handler instanceof MessageHandler.Partial) {
      addMessageHandler(getTypeParameter(handler.getClass(), MessageHandler.Partial.class), (MessageHandler.Partial)handler);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized <T> void addMessageHandler (Class<T> clazz, MessageHandler.Whole<T> messageHandler) {

    boolean assigned = false;

    if (String.class.isAssignableFrom(clazz)) {
      assignTextMessageHandler(messageHandler);
      assigned = true;
    }
    if (Reader.class.isAssignableFrom(clazz)) {
      assignTextMessageHandler(messageHandler);
      assigned = true;
    }
    if (ByteBuffer.class.isAssignableFrom(clazz)) {
      assignBinaryMessageHandler(messageHandler);
      assigned = true;
    }
    if (InputStream.class.isAssignableFrom(clazz)) {
      assignBinaryMessageHandler(messageHandler);
      assigned = true;
    }
    if (PongMessage.class.isAssignableFrom(clazz)) {
      assignPongMessageHandler(messageHandler);
      assigned = true;
    }

    if (!assigned) {
      try {
        assigned = assignDecoder(clazz, messageHandler);
      } catch (IllegalAccessException | InstantiationException exception) {
        throw new MalformedMessageHandlerException(exception);
      }
    }

    if (!assigned) {
      throw new MalformedMessageHandlerException("Illegal parametrized type(%s) for whole message handler(%s)", clazz.getName(), messageHandler.getClass().getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized <T> void addMessageHandler (Class<T> clazz, MessageHandler.Partial<T> messageHandler) {

    boolean assigned = false;

    if (String.class.isAssignableFrom(clazz)) {
      assignTextMessageHandler(messageHandler);
      assigned = true;
    }
    if (ByteBuffer.class.isAssignableFrom(clazz)) {
      assignBinaryMessageHandler(messageHandler);
      assigned = true;
    }
    if (byte[].class.isAssignableFrom(clazz)) {
      assignBinaryMessageHandler(messageHandler);
      assigned = true;
    }

    if (!assigned) {
      throw new MalformedMessageHandlerException("Illegal parametrized type(%s) for partial message handler(%s)", clazz.getName(), messageHandler.getClass().getName());
    }
  }

  /**
   * Attempts to locate and register a decoder capable of handling the given message type.
   *
   * @param clazz          the desired decoded type
   * @param messageHandler the handler to receive decoded instances
   * @param <T>            the decoded type
   * @return {@code true} if a decoder was registered; otherwise {@code false}
   * @throws IllegalAccessException if a decoder cannot be instantiated
   * @throws InstantiationException if a decoder cannot be instantiated
   */
  private <T> boolean assignDecoder (Class<T> clazz, MessageHandler.Whole<T> messageHandler)
    throws IllegalAccessException, InstantiationException {

    boolean assigned = false;

    for (Class<? extends Decoder> decoderClass : endpointConfig.getDecoders()) {
      if ((Decoder.Text.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(getTypeParameter(decoderClass, Decoder.Text.class))) {

        Decoder decoder;

        if ((decoder = decoderInstanceMap.get(decoderClass)) == null) {
          decoderInstanceMap.put(decoderClass, decoder = decoderClass.newInstance());
        }

        assignTextMessageHandler(new DecodedStringHandler(this, endpoint, (Decoder.Text<?>)decoder, messageHandler));
        assigned = true;
      }
      if ((Decoder.TextStream.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(getTypeParameter(decoderClass, Decoder.TextStream.class))) {

        Decoder decoder;

        if ((decoder = decoderInstanceMap.get(decoderClass)) == null) {
          decoderInstanceMap.put(decoderClass, decoder = decoderClass.newInstance());
        }

        assignTextMessageHandler(new DecodedReaderHandler(this, endpoint, (Decoder.TextStream<?>)decoder, messageHandler));
        assigned = true;
      }
      if ((Decoder.Binary.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(getTypeParameter(decoderClass, Decoder.Binary.class))) {

        Decoder decoder;

        if ((decoder = decoderInstanceMap.get(decoderClass)) == null) {
          decoderInstanceMap.put(decoderClass, decoder = decoderClass.newInstance());
        }

        assignBinaryMessageHandler(new DecodedByteBufferHandler(this, endpoint, (Decoder.Binary<?>)decoder, messageHandler));
        assigned = true;
      }
      if ((Decoder.BinaryStream.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(getTypeParameter(decoderClass, Decoder.BinaryStream.class))) {

        Decoder decoder;

        if ((decoder = decoderInstanceMap.get(decoderClass)) == null) {
          decoderInstanceMap.put(decoderClass, decoder = decoderClass.newInstance());
        }

        assignBinaryMessageHandler(new DecodedInputStreamHandler(this, endpoint, (Decoder.BinaryStream<?>)decoder, messageHandler));
        assigned = true;
      }
    }

    return assigned;
  }

  /**
   * Determines the parameter type declared on a handler/decoder implementation.
   *
   * @param objectClass     the implementation class
   * @param targetInterface the generic interface implemented
   * @return the single generic type argument
   */
  private Class<?> getTypeParameter (Class<?> objectClass, Class<?> targetInterface) {

    List<Class<?>> parameterList;

    if ((parameterList = GenericUtility.getTypeArgumentsOfImplementation(objectClass, targetInterface)).size() != 1) {
      throw new MalformedMessageHandlerException("Unable to determine the parameterized type of %s(%s)", targetInterface.getName(), objectClass.getName());
    }

    return parameterList.get(0);
  }

  /**
   * Assigns a text message handler, enforcing single registration.
   *
   * @param messageHandler the handler to register
   */
  private void assignTextMessageHandler (MessageHandler messageHandler) {

    if (!textMessageHandlerRef.compareAndSet(null, messageHandler)) {
      throw new IllegalStateException("Session is already assigned a text message handler");
    }
  }

  /**
   * Assigns a binary message handler, enforcing single registration.
   *
   * @param messageHandler the handler to register
   */
  private void assignBinaryMessageHandler (MessageHandler messageHandler) {

    if (!binaryMessageHandlerRef.compareAndSet(null, messageHandler)) {
      throw new IllegalStateException("Session is already assigned a binary message handler");
    }
  }

  /**
   * Assigns a pong message handler, enforcing single registration.
   *
   * @param messageHandler the handler to register
   */
  private void assignPongMessageHandler (MessageHandler messageHandler) {

    if (!pongMessageHandlerRef.compareAndSet(null, messageHandler)) {
      throw new IllegalStateException("Session is already assigned a pong message handler");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Set<MessageHandler> getMessageHandlers () {

    Set<MessageHandler> handlerSet = new HashSet<>();
    MessageHandler textMessageHandler;
    MessageHandler binaryMessageHandler;
    MessageHandler pongMessageHandler;

    if ((textMessageHandler = textMessageHandlerRef.get()) != null) {
      handlerSet.add(textMessageHandler);
    }
    if ((binaryMessageHandler = binaryMessageHandlerRef.get()) != null) {
      handlerSet.add(binaryMessageHandler);
    }
    if ((pongMessageHandler = pongMessageHandlerRef.get()) != null) {
      handlerSet.add(pongMessageHandler);
    }

    return Collections.unmodifiableSet(handlerSet);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void removeMessageHandler (MessageHandler handler) {

    if (handler != null) {
      textMessageHandlerRef.compareAndSet(handler, null);
      binaryMessageHandlerRef.compareAndSet(handler, null);
      pongMessageHandlerRef.compareAndSet(handler, null);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProtocolVersion () {

    return String.valueOf(webSocket.getProtocolVersion());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getNegotiatedSubprotocol () {

    return webSocket.getNegotiatedProtocol();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Extension> getNegotiatedExtensions () {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSecure () {

    return webSocket.isSecure();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOpen () {

    return webSocket.getConnectionState().equals(ConnectionState.OPEN);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getMaxIdleTimeout () {

    return webSocket.getMaxIdleTimeoutMilliseconds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaxIdleTimeout (long milliseconds) {

    webSocket.setMaxIdleTimeoutMilliseconds(milliseconds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxBinaryMessageBufferSize () {

    return webSocket.getMaxBinaryBufferSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaxBinaryMessageBufferSize (int length) {

    webSocket.setMaxBinaryBufferSize(length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxTextMessageBufferSize () {

    return webSocket.getMaxTextBufferSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMaxTextMessageBufferSize (int length) {

    webSocket.setMaxTextBufferSize(length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onClose (int code, String reason) {

    endpoint.onClose(this, new CloseReason(jakarta.websocket.CloseReason.CloseCodes.getCloseCode(code), reason));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close () {

    try {
      webSocket.close(CloseCode.NORMAL, CloseCode.NORMAL.name());
    } catch (Exception exception) {
      endpoint.onError(this, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close (CloseReason closeReason) {

    try {
      webSocket.close(CloseCode.fromCode(closeReason.getCloseCode().getCode()), closeReason.getReasonPhrase());
    } catch (Exception exception) {
      endpoint.onError(this, exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI getRequestURI () {

    return webSocket.getUri();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getQueryString () {

    return webSocket.getUri().getQuery();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, List<String>> getRequestParameterMap () {

    try {
      return HTTPCodec.urlDecode(webSocket.getUri().getQuery()).asMap();
    } catch (UnsupportedEncodingException unsupportedEncodingException) {
      throw new SessionRuntimeException(unsupportedEncodingException);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getPathParameters () {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getUserProperties () {

    return endpointConfig.getUserProperties();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Principal getUserPrincipal () {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Session> getOpenSessions () {

    HashSet<Session> sessionSet = new HashSet<>();

    sessionSet.add(this);

    return sessionSet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId () {

    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RemoteEndpoint.Async getAsyncRemote () {

    return new RemoteEndpointImpl.Async(this, webSocket, endpoint, endpointConfig);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RemoteEndpoint.Basic getBasicRemote () {

    return new RemoteEndpointImpl.Basic(this, webSocket, endpoint, endpointConfig);
  }
}
