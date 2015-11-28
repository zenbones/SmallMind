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
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import org.smallmind.web.websocket.WebSocket;
import org.smallmind.web.websocket.WebSocketException;

public class RemoteEndpointImpl implements RemoteEndpoint {

  private final WebSocket webSocket;
  private final HashMap<Class<?>, EncoderHandler<?>> encoderHandlerMap = new HashMap<>();

  public RemoteEndpointImpl (WebSocket webSocket, EndpointConfig endpointConfig)
    throws InstantiationException, IllegalAccessException {

    this.webSocket = webSocket;

    HashMap<Class<? extends Encoder>, Encoder> encoderInstanceMap = new HashMap<>();

    for (Class<? extends Encoder> encoderClass : endpointConfig.getEncoders()) {
      if (Encoder.Text.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
        }

        encoderHandlerMap.put(GenericParameterUtility.getTypeParameter(encoderClass, Encoder.Text.class), new EncoderTextHandler<>((Encoder.Text)encoder));
      }
      if (Encoder.TextStream.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
        }

        encoderHandlerMap.put(GenericParameterUtility.getTypeParameter(encoderClass, Encoder.TextStream.class), new EncoderTextStreamHandler<>((Encoder.TextStream)encoder));
      }
      if (Encoder.Binary.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
        }

        encoderHandlerMap.put(GenericParameterUtility.getTypeParameter(encoderClass, Encoder.Binary.class), new EncoderBinaryHandler<>((Encoder.Binary)encoder));
      }
      if (Encoder.BinaryStream.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
        }

        encoderHandlerMap.put(GenericParameterUtility.getTypeParameter(encoderClass, Encoder.BinaryStream.class), new EncoderBinaryStreamHandler<>((Encoder.BinaryStream)encoder));
      }
    }
  }

  @Override
  public boolean getBatchingAllowed () {

    return false;
  }

  @Override
  public void setBatchingAllowed (boolean allowed) {

  }

  @Override
  public void flushBatch () {

  }

  @Override
  public void sendPing (ByteBuffer applicationData)
    throws IOException {

    try {
      webSocket.ping(applicationData.array());
    } catch (WebSocketException webSocketException) {
      throw new IOException(webSocketException);
    }
  }

  @Override
  public void sendPong (ByteBuffer applicationData)
    throws IOException {

    throw new IOException("pongs are automatically sent in response to pings");
  }

  public class Basic extends RemoteEndpointImpl implements RemoteEndpoint.Basic {

    public Basic (WebSocket webSocket, EndpointConfig endpointConfig)
      throws InstantiationException, IllegalAccessException {

      super(webSocket, endpointConfig);
    }

    @Override
    public void sendText (String text)
      throws IOException {

      try {
        webSocket.text(text);
      } catch (WebSocketException webSocketException) {
        throw new IOException(webSocketException);
      }
    }

    @Override
    public void sendBinary (ByteBuffer data)
      throws IOException {

      try {
        webSocket.binary(data.array());
      } catch (WebSocketException webSocketException) {
        throw new IOException(webSocketException);
      }
    }

    @Override
    public void sendText (String partialMessage, boolean isLast)
      throws IOException {

    }

    @Override
    public void sendBinary (ByteBuffer partialByte, boolean isLast)
      throws IOException {

    }

    @Override
    public OutputStream getSendStream () throws IOException {

      return null;
    }

    @Override
    public Writer getSendWriter () throws IOException {

      return null;
    }

    @Override
    public void sendObject (Object data)
      throws IOException, EncodeException {

      EncoderHandler<?> encoderHandler;

      if ((encoderHandler = encoderHandlerMap.get(data.getClass())) != null) {
        sendBinary(ByteBuffer.wrap(encoderHandler.encode(data)));
      } else {
        sendText(data.toString());
      }
    }
  }

  public class Async extends RemoteEndpointImpl implements RemoteEndpoint.Async {

    private AtomicLong sendTimeout = new AtomicLong(0);

    public Async (WebSocket webSocket, EndpointConfig endpointConfig)
      throws InstantiationException, IllegalAccessException {

      super(webSocket, endpointConfig);
    }

    @Override
    public synchronized long getSendTimeout () {

      return sendTimeout.get();
    }

    @Override
    public synchronized void setSendTimeout (long timeoutmillis) {

      sendTimeout.set(timeoutmillis);
    }

    @Override
    public void sendText (String text, SendHandler handler) {

    }

    @Override
    public Future<Void> sendText (String text) {

      return null;
    }

    @Override
    public Future<Void> sendBinary (ByteBuffer data) {

      return null;
    }

    @Override
    public void sendBinary (ByteBuffer data, SendHandler handler) {

    }

    @Override
    public Future<Void> sendObject (Object data) {

      return null;
    }

    @Override
    public void sendObject (Object data, SendHandler handler) {

    }
  }
}
