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
package org.smallmind.web.websocket.spi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.web.websocket.WebSocket;
import org.smallmind.web.websocket.WebSocketException;

public class RemoteEndpointImpl implements RemoteEndpoint {

  private final SessionImpl session;
  private final WebSocket webSocket;
  private final Endpoint endpoint;
  private final HashMap<Class<?>, EncoderHandler<?>> encoderHandlerMap = new HashMap<>();

  public RemoteEndpointImpl (SessionImpl session, WebSocket webSocket, Endpoint endpoint, EndpointConfig endpointConfig) {

    this.session = session;
    this.webSocket = webSocket;
    this.endpoint = endpoint;

    HashMap<Class<? extends Encoder>, Encoder> encoderInstanceMap = new HashMap<>();

    for (Class<? extends Encoder> encoderClass : endpointConfig.getEncoders()) {
      if (Encoder.Text.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          try {
            encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
          } catch (InstantiationException | IllegalAccessException exception) {
            endpoint.onError(session, exception);
          }
        }

        encoderHandlerMap.put(getTypeParameter(encoderClass, Encoder.Text.class), new EncoderTextHandler<>((Encoder.Text<?>)encoder));
      }
      if (Encoder.TextStream.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          try {
            encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
          } catch (InstantiationException | IllegalAccessException exception) {
            endpoint.onError(session, exception);
          }
        }

        encoderHandlerMap.put(getTypeParameter(encoderClass, Encoder.TextStream.class), new EncoderTextStreamHandler<>((Encoder.TextStream<?>)encoder));
      }
      if (Encoder.Binary.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          try {
            encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
          } catch (InstantiationException | IllegalAccessException exception) {
            endpoint.onError(session, exception);
          }
        }

        encoderHandlerMap.put(getTypeParameter(encoderClass, Encoder.Binary.class), new EncoderBinaryHandler<>((Encoder.Binary<?>)encoder));
      }
      if (Encoder.BinaryStream.class.isAssignableFrom(encoderClass)) {

        Encoder encoder;

        if ((encoder = encoderInstanceMap.get(encoderClass)) == null) {
          try {
            encoderInstanceMap.put(encoderClass, encoder = encoderClass.newInstance());
          } catch (InstantiationException | IllegalAccessException exception) {
            endpoint.onError(session, exception);
          }
        }

        encoderHandlerMap.put(getTypeParameter(encoderClass, Encoder.BinaryStream.class), new EncoderBinaryStreamHandler<>((Encoder.BinaryStream<?>)encoder));
      }
    }
  }

  private Class<?> getTypeParameter (Class<? extends Encoder> encoderClass, Class<?> encoderInterface) {

    List<Class<?>> parameterList;

    if ((parameterList = GenericUtility.getTypeArgumentsOfImplementation(encoderClass, encoderInterface)).size() != 1) {
      throw new MalformedMessageHandlerException("Unable to determine the parameterized type of %s(%s)", encoderInterface.getName(), encoderClass.getName());
    }

    return parameterList.get(0);
  }

  public SessionImpl getSession () {

    return session;
  }

  public WebSocket getWebSocket () {

    return webSocket;
  }

  public Endpoint getEndpoint () {

    return endpoint;
  }

  public HashMap<Class<?>, EncoderHandler<?>> getEncoderHandlerMap () {

    return encoderHandlerMap;
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
      endpoint.onError(session, webSocketException);
    }
  }

  @Override
  public void sendPong (ByteBuffer applicationData)
    throws IOException {

    throw new IOException("pongs are automatically sent in response to pings");
  }

  private static class SendFuture implements Future<Void> {

    private final SendRunnable sendRunnable;
    private final Thread sendThread;

    public SendFuture (SendRunnable sendRunnable) {

      this.sendRunnable = sendRunnable;

      (sendThread = new Thread(sendRunnable)).start();
    }

    @Override
    public boolean cancel (boolean mayInterruptIfRunning) {

      return false;
    }

    @Override
    public boolean isCancelled () {

      return false;
    }

    @Override
    public boolean isDone () {

      return !sendThread.isAlive();
    }

    @Override
    public Void get ()
      throws InterruptedException, ExecutionException {

      sendThread.join();
      if (sendRunnable.getThrowable() != null) {
        throw new ExecutionException(sendRunnable.getThrowable());
      }

      return null;
    }

    @Override
    public Void get (long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {

      sendThread.join(unit.toMillis(timeout));

      if (sendThread.isAlive()) {
        throw new TimeoutException();
      }
      if (sendRunnable.getThrowable() != null) {
        throw new ExecutionException(sendRunnable.getThrowable());
      }

      return null;
    }
  }

  private static class SendRunnable implements Runnable {

    private final SendExecutable executable;
    private Throwable throwable;

    public SendRunnable (SendExecutable executable) {

      this.executable = executable;
    }

    public Throwable getThrowable () {

      return throwable;
    }

    @Override
    public void run () {

      try {
        executable.execute();
      } catch (Throwable throwable) {
        this.throwable = throwable;
      }
    }
  }

  private static class SendStream extends OutputStream {

    private final RemoteEndpointImpl.Basic basicEndpoint;
    private final AtomicReference<ByteArrayOutputStream> partialStreamRef;

    public SendStream (Basic basicEndpoint, AtomicReference<ByteArrayOutputStream> partialStreamRef) {

      this.basicEndpoint = basicEndpoint;
      this.partialStreamRef = partialStreamRef;
    }

    @Override
    public void write (int b) {

      partialStreamRef.get().write(b);
    }

    @Override
    public void write (byte[] b, int off, int len) {

      partialStreamRef.get().write(b, off, len);
    }

    @Override
    public void write (byte[] b)
      throws IOException {

      partialStreamRef.get().write(b);
    }

    @Override
    public void close ()
      throws IOException {

      byte[] completeBuffer = partialStreamRef.get().toByteArray();

      partialStreamRef.set(null);
      basicEndpoint.sendBinary(ByteBuffer.wrap(completeBuffer));

      super.close();
    }
  }

  private static class SendWriter extends Writer {

    private final RemoteEndpointImpl.Basic basicEndpoint;
    private final AtomicReference<StringBuilder> partialBuilderRef;

    public SendWriter (Basic basicEndpoint, AtomicReference<StringBuilder> partialBuilderRef) {

      this.basicEndpoint = basicEndpoint;
      this.partialBuilderRef = partialBuilderRef;
    }

    @Override
    public void write (char[] cbuf, int off, int len) {

      partialBuilderRef.get().append(cbuf, off, len);
    }

    @Override
    public void flush () {

    }

    @Override
    public void close ()
      throws IOException {

      String completeText = partialBuilderRef.get().toString();

      partialBuilderRef.set(null);
      basicEndpoint.sendText(completeText);
    }
  }

  private static abstract class SendExecutable {

    public abstract void execute ()
      throws Throwable;
  }

  public static class Basic extends RemoteEndpointImpl implements RemoteEndpoint.Basic {

    private final AtomicReference<StringBuilder> partialBuilderRef = new AtomicReference<>();
    private final AtomicReference<ByteArrayOutputStream> partialStreamRef = new AtomicReference<>();

    public Basic (SessionImpl session, WebSocket webSocket, Endpoint endpoint, EndpointConfig endpointConfig) {

      super(session, webSocket, endpoint, endpointConfig);
    }

    @Override
    public synchronized void sendText (String text)
      throws IOException {

      if ((partialBuilderRef.get() != null) || (partialStreamRef.get() != null)) {
        throw new IllegalStateException("Incomplete transmission ongoing in another thread of execution");
      }

      try {
        getWebSocket().text(text);
      } catch (WebSocketException webSocketException) {
        getEndpoint().onError(getSession(), webSocketException);
      }
    }

    @Override
    public synchronized void sendBinary (ByteBuffer data)
      throws IOException {

      if ((partialBuilderRef.get() != null) || (partialStreamRef.get() != null)) {
        throw new IllegalStateException("Incomplete transmission ongoing in another thread of execution");
      }

      try {
        getWebSocket().binary(data.array());
      } catch (WebSocketException webSocketException) {
        getEndpoint().onError(getSession(), webSocketException);
      }
    }

    @Override
    public synchronized void sendText (String partialMessage, boolean isLast)
      throws IOException {

      if (partialStreamRef.get() != null) {
        throw new IllegalStateException("Incomplete transmission ongoing in another thread of execution");
      }

      if (isLast) {
        if (partialBuilderRef.get() == null) {
          sendText(partialMessage);
        } else {

          String completeText = partialBuilderRef.get().append(partialMessage).toString();

          partialBuilderRef.set(null);
          sendText(completeText);
        }
      } else {

        StringBuilder partialBuilder;

        if ((partialBuilder = partialBuilderRef.get()) == null) {
          partialBuilderRef.set(partialBuilder = new StringBuilder());
        }
        partialBuilder.append(partialMessage);
      }
    }

    @Override
    public synchronized void sendBinary (ByteBuffer partialByte, boolean isLast)
      throws IOException {

      if (partialBuilderRef.get() != null) {
        throw new IllegalStateException("Incomplete transmission ongoing in another thread of execution");
      }

      if (isLast) {
        if (partialStreamRef.get() == null) {
          sendBinary(partialByte);
        } else {

          byte[] completeBuffer;

          partialStreamRef.get().write(partialByte.array());
          completeBuffer = partialStreamRef.get().toByteArray();

          partialStreamRef.set(null);
          sendBinary(ByteBuffer.wrap(completeBuffer));
        }
      } else {

        ByteArrayOutputStream partialStream;

        if ((partialStream = partialStreamRef.get()) == null) {
          partialStreamRef.set(partialStream = new ByteArrayOutputStream());
        }
        partialStream.write(partialByte.array());
      }
    }

    @Override
    public synchronized OutputStream getSendStream () {

      if ((partialBuilderRef.get() != null) || (partialStreamRef.get() != null)) {
        throw new IllegalStateException("Incomplete transmission ongoing in another thread of execution");
      }

      partialStreamRef.set(new ByteArrayOutputStream());

      return new SendStream(this, partialStreamRef);
    }

    @Override
    public synchronized Writer getSendWriter () {

      if ((partialBuilderRef.get() != null) || (partialStreamRef.get() != null)) {
        throw new IllegalStateException("Incomplete transmission ongoing in another thread of execution");
      }

      partialBuilderRef.set(new StringBuilder());

      return new SendWriter(this, partialBuilderRef);
    }

    @Override
    public void sendObject (Object data)
      throws IOException, EncodeException {

      EncoderHandler<?> encoderHandler;

      if ((encoderHandler = getEncoderHandlerMap().get(data.getClass())) != null) {
        sendBinary(ByteBuffer.wrap(encoderHandler.encode(data)));
      } else {
        sendText(data.toString());
      }
    }
  }

  public static class Async extends RemoteEndpointImpl implements RemoteEndpoint.Async {

    private final AtomicLong sendTimeout;

    public Async (SessionImpl session, WebSocket webSocket, Endpoint endpoint, EndpointConfig endpointConfig) {

      super(session, webSocket, endpoint, endpointConfig);

      sendTimeout = new AtomicLong(session.getContainer().getDefaultAsyncSendTimeout());
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
    public Future<Void> sendText (final String text) {

      return new SendFuture(new SendRunnable(new SendExecutable() {

        @Override
        public void execute ()
          throws IOException {

          try {
            getWebSocket().text(text);
          } catch (WebSocketException webSocketException) {
            getEndpoint().onError(getSession(), webSocketException);
          }
        }
      }));
    }

    @Override
    public void sendText (String text, SendHandler handler) {

      waitForFuture(sendText(text), handler);
    }

    @Override
    public Future<Void> sendBinary (final ByteBuffer data) {

      return new SendFuture(new SendRunnable(new SendExecutable() {

        @Override
        public void execute ()
          throws IOException {

          try {
            getWebSocket().binary(data.array());
          } catch (WebSocketException webSocketException) {
            getEndpoint().onError(getSession(), webSocketException);
          }
        }
      }));
    }

    @Override
    public void sendBinary (ByteBuffer data, SendHandler handler) {

      waitForFuture(sendBinary(data), handler);
    }

    public Future<Void> sendObject (final Object data) {

      return new SendFuture(new SendRunnable(new SendExecutable() {

        @Override
        public void execute ()
          throws IOException, EncodeException {

          EncoderHandler<?> encoderHandler;

          try {
            if ((encoderHandler = getEncoderHandlerMap().get(data.getClass())) != null) {
              getWebSocket().binary(encoderHandler.encode(data));
            } else {
              getWebSocket().text(data.toString());
            }
          } catch (WebSocketException webSocketException) {
            getEndpoint().onError(getSession(), webSocketException);
          }
        }
      }));
    }

    @Override
    public void sendObject (Object data, SendHandler handler) {

      waitForFuture(sendObject(data), handler);
    }

    private void waitForFuture (Future<Void> future, SendHandler handler) {

      try {
        long sendTimeout;

        if ((sendTimeout = getSendTimeout()) > 0) {
          future.get(sendTimeout, TimeUnit.MILLISECONDS);
        } else {
          future.get();
        }

        handler.onResult(new SendResult());
      } catch (InterruptedException | ExecutionException | TimeoutException exception) {
        handler.onResult(new SendResult(exception));
      }
    }
  }
}
