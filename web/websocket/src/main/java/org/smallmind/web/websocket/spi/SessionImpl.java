/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.websocket.CloseReason;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.smallmind.nutsnbolts.http.HTTPCodec;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.web.websocket.CloseCode;
import org.smallmind.web.websocket.ConnectionState;
import org.smallmind.web.websocket.WebSocket;

public class SessionImpl implements Session {

  private final WebSocket webSocket;
  private final WebSocketContainer container;
  private final EndpointConfig endpointConfig;
  private final Map<String, Object> userPropertyMap = new HashMap<>();
  private final String id = SnowflakeId.newInstance().generateHexEncoding();
  private MessageHandler textMessageHandler;
  private MessageHandler binaryMessageHandler;
  private MessageHandler pongMessageHandler;

  public SessionImpl (WebSocket webSocket, WebSocketContainer container, EndpointConfig endpointConfig) {

    this.webSocket = webSocket;
    this.container = container;
    this.endpointConfig = endpointConfig;
  }

  @Override
  public WebSocketContainer getContainer () {

    return container;
  }

  @Override
  public void addMessageHandler (MessageHandler handler)
    throws IllegalStateException {

    if (handler instanceof MessageHandler.Whole) {
      addMessageHandler(GenericParameterUtility.getTypeParameter(handler.getClass(), MessageHandler.Whole.class), (MessageHandler.Whole)handler);
    }
    if (handler instanceof MessageHandler.Partial) {
      addMessageHandler(GenericParameterUtility.getTypeParameter(handler.getClass(), MessageHandler.Partial.class), (MessageHandler.Partial)handler);
    }
  }

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

  @Override
  public synchronized <T> void addMessageHandler (Class<T> clazz, MessageHandler.Partial<T> messageHandler) {

    boolean assigned = false;

    if (String.class.isAssignableFrom(clazz)) {
      assignTextMessageHandler(messageHandler);
      assigned = true;
    }
    if (ByteBuffer.class.isAssignableFrom(clazz)) {
      assignBinaryMessageHandler(binaryMessageHandler);
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

  private <T> boolean assignDecoder (Class<T> clazz, MessageHandler.Whole<T> messageHandler)
    throws IllegalAccessException, InstantiationException {

    boolean assigned = false;

    for (Class<? extends Decoder> decoderClass : endpointConfig.getDecoders()) {
      if ((Decoder.Text.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(GenericParameterUtility.getTypeParameter(decoderClass, Decoder.Text.class))) {
        assignTextMessageHandler(new DecodedStringHandler<>((Decoder.Text)decoderClass.newInstance(), messageHandler));
        assigned = true;
      }
      if ((Decoder.TextStream.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(GenericParameterUtility.getTypeParameter(decoderClass, Decoder.TextStream.class))) {
        assignTextMessageHandler(new DecodedReaderHandler<>((Decoder.TextStream)decoderClass.newInstance(), messageHandler));
        assigned = true;
      }
      if ((Decoder.Binary.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(GenericParameterUtility.getTypeParameter(decoderClass, Decoder.Binary.class))) {
        assignBinaryMessageHandler(new DecodedByteBufferHandler<>((Decoder.Binary)decoderClass.newInstance(), messageHandler));
        assigned = true;
      }
      if ((Decoder.BinaryStream.class.isAssignableFrom(decoderClass)) && clazz.isAssignableFrom(GenericParameterUtility.getTypeParameter(decoderClass, Decoder.BinaryStream.class))) {
        assignBinaryMessageHandler(new DecodedInputStreamHandler<>((Decoder.BinaryStream)decoderClass.newInstance(), messageHandler));
        assigned = true;
      }
    }

    return assigned;
  }

  private void assignTextMessageHandler (MessageHandler messageHandler) {

    if (textMessageHandler != null) {
      throw new IllegalStateException("Session is already assigned a text message handler");
    }

    textMessageHandler = messageHandler;
  }

  private void assignBinaryMessageHandler (MessageHandler messageHandler) {

    if (binaryMessageHandler != null) {
      throw new IllegalStateException("Session is already assigned a binary message handler");
    }

    binaryMessageHandler = messageHandler;
  }

  private void assignPongMessageHandler (MessageHandler messageHandler) {

    if (pongMessageHandler != null) {
      throw new IllegalStateException("Session is already assigned a pong message handler");
    }

    pongMessageHandler = messageHandler;
  }

  @Override
  public synchronized Set<MessageHandler> getMessageHandlers () {

    Set<MessageHandler> handlerSet = new HashSet<MessageHandler>();

    if (textMessageHandler != null) {
      handlerSet.add(textMessageHandler);
    }
    if (binaryMessageHandler != null) {
      handlerSet.add(binaryMessageHandler);
    }
    if (pongMessageHandler != null) {
      handlerSet.add(pongMessageHandler);
    }

    return Collections.unmodifiableSet(handlerSet);
  }

  @Override
  public synchronized void removeMessageHandler (MessageHandler handler) {

    if (handler != null) {
      if (handler.equals(textMessageHandler)) {
        textMessageHandler = null;
      }
      if (handler.equals(binaryMessageHandler)) {
        binaryMessageHandler = null;
      }
      if (handler.equals(pongMessageHandler)) {
        pongMessageHandler = null;
      }
    }
  }

  @Override
  public String getProtocolVersion () {

    return String.valueOf(webSocket.getProtocolVersion());
  }

  @Override
  public String getNegotiatedSubprotocol () {

    return webSocket.getNegotiatedProtocol();
  }

  @Override
  public List<Extension> getNegotiatedExtensions () {

    return null;
  }

  @Override
  public boolean isSecure () {

    return webSocket.isSecure();
  }

  @Override
  public boolean isOpen () {

    return webSocket.getConnectionState().equals(ConnectionState.OPEN);
  }

  @Override
  public long getMaxIdleTimeout () {

    return webSocket.getMaxIdleTimeoutMilliseconds();
  }

  @Override
  public void setMaxIdleTimeout (long milliseconds) {

    webSocket.setMaxIdleTimeoutMilliseconds(milliseconds);
  }

  @Override
  public int getMaxBinaryMessageBufferSize () {

    return webSocket.getMaxBinaryBufferSize();
  }

  @Override
  public void setMaxBinaryMessageBufferSize (int length) {

    webSocket.setMaxBinaryBufferSize(length);
  }

  @Override
  public int getMaxTextMessageBufferSize () {

    return webSocket.getMaxTextBufferSize();
  }

  @Override
  public void setMaxTextMessageBufferSize (int length) {

    webSocket.setMaxTextBufferSize(length);
  }

  @Override
  public void close ()
    throws IOException {

    try {
      webSocket.close();
    } catch (Exception exception) {
      throw new IOException(exception);
    }
  }

  @Override
  public void close (CloseReason closeReason)
    throws IOException {

    try {
      webSocket.close(CloseCode.fromCode(closeReason.getCloseCode().getCode()), closeReason.getReasonPhrase());
    } catch (Exception exception) {
      throw new IOException(exception);
    }
  }

  @Override
  public URI getRequestURI () {

    return webSocket.getUri();
  }

  @Override
  public String getQueryString () {

    return webSocket.getUri().getQuery();
  }

  @Override
  public Map<String, List<String>> getRequestParameterMap () {

    try {
      return HTTPCodec.urlDecode(webSocket.getUri().getQuery()).asMap();
    } catch (UnsupportedEncodingException unsupportedEncodingException) {
      throw new SessionRuntimeException(unsupportedEncodingException);
    }
  }

  @Override
  public Map<String, String> getPathParameters () {

    return null;
  }

  @Override
  public Map<String, Object> getUserProperties () {

    return userPropertyMap;
  }

  @Override
  public Principal getUserPrincipal () {

    return null;
  }

  @Override
  public Set<Session> getOpenSessions () {

    HashSet<Session> sessionSet = new HashSet<>();

    sessionSet.add(this);

    return sessionSet;
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public RemoteEndpoint.Async getAsyncRemote () {

    return null;
  }

  @Override
  public RemoteEndpoint.Basic getBasicRemote () {

    return null;
  }
}
