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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.LongPollingTransport;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.ServletProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AsyncOumuamuaServletTest {

  private AsyncOumuamuaServlet<OrthodoxValue> servlet;

  private static Object probeScope () {

    try {

      return PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {

      return missingPerApplicationScopeException;
    }
  }

  @BeforeMethod
  public void beforeMethod () {

    servlet = new AsyncOumuamuaServlet<>();
  }

  public void testDoPostWithNullContentLengthSendsError ()
    throws IOException {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn(null);

    servlet.doPost(request, response);

    Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing content length");
  }

  public void testDoPostWithEmptyContentLengthSendsError ()
    throws IOException {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("");

    servlet.doPost(request, response);

    Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing content length");
  }

  public void testDoPostWithInvalidContentLengthSendsError ()
    throws IOException {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("not-a-number");

    servlet.doPost(request, response);

    Mockito.verify(response).sendError(Mockito.eq(HttpServletResponse.SC_BAD_REQUEST), Mockito.eq("Invalid content length"));
  }

  public void testDoPostWithZeroContentLengthSendsError ()
    throws IOException {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("0");

    servlet.doPost(request, response);

    Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read the full content");
  }

  public void testDoPostWithNegativeContentLengthSendsError ()
    throws IOException {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("-5");

    servlet.doPost(request, response);

    Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read the full content");
  }

  @Test(expectedExceptions = ServletException.class, expectedExceptionsMessageRegExp = ".*Missing OumuamuaServer.*")
  public void testInitWithoutServerAttributeThrows ()
    throws Exception {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(null);

    servlet.init(servletConfig);
  }

  @Test(expectedExceptions = ServletException.class, expectedExceptionsMessageRegExp = ".*http protocol.*")
  public void testInitWithoutHttpProtocolThrows ()
    throws Exception {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(server);
    Mockito.when(server.getProtocol(Protocols.SERVLET.getName())).thenReturn(null);

    servlet.init(servletConfig);
  }

  @Test(expectedExceptions = ServletException.class, expectedExceptionsMessageRegExp = ".*long polling transport.*")
  public void testInitWithoutLongPollingTransportThrows ()
    throws Exception {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);
    Protocol<OrthodoxValue> protocol = Mockito.mock(Protocol.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(server);
    Mockito.when(server.getProtocol(Protocols.SERVLET.getName())).thenReturn(protocol);
    Mockito.when(protocol.getTransport(Transports.LONG_POLLING.getName())).thenReturn(null);

    servlet.init(servletConfig);
  }

  public void testGetServletInfoDelegatesToSuper () {

    Assert.assertEquals(servlet.getServletInfo(), "");
  }

  private OumuamuaServer<OrthodoxValue> initServletWithMocks ()
    throws Exception {

    return initServletWithMocks(null);
  }

  // The scope is read during init(), when the servlet builds its own message executor, so it is stubbed beforehand.
  private OumuamuaServer<OrthodoxValue> initServletWithMocks (PerApplicationScope perApplicationScope)
    throws Exception {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);
    ServletProtocol<OrthodoxValue> protocol = Mockito.mock(ServletProtocol.class);
    LongPollingTransport<OrthodoxValue> transport = Mockito.mock(LongPollingTransport.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(server);
    Mockito.when(server.getProtocol(Protocols.SERVLET.getName())).thenReturn(protocol);
    Mockito.when(protocol.getTransport(Transports.LONG_POLLING.getName())).thenReturn(transport);
    Mockito.when(transport.getProtocol()).thenReturn(protocol);
    Mockito.when(server.getPerApplicationScope()).thenReturn(perApplicationScope);

    servlet.init(servletConfig);

    return server;
  }

  public void testDestroyShutsDownServer ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = initServletWithMocks();

    servlet.destroy();

    Mockito.verify(server).stop();
  }

  public void testDoPostInstallsReadListenerOnValidPost ()
    throws Exception {

    initServletWithMocks();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("8");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(false);

    servlet.doPost(request, response);

    Mockito.verify(asyncContext, Mockito.timeout(2000)).setTimeout(0);
    Mockito.verify(inputStream, Mockito.timeout(2000)).setReadListener(Mockito.any(ReadListener.class));
    Mockito.verify(inputStream, Mockito.never()).read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());
  }

  public void testReadListenerOnErrorCompletesAsyncContext ()
    throws Exception {

    initServletWithMocks();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("4");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(false);

    ArgumentCaptor<ReadListener> listenerCaptor = ArgumentCaptor.forClass(ReadListener.class);

    servlet.doPost(request, response);

    Mockito.verify(inputStream, Mockito.timeout(2000)).setReadListener(listenerCaptor.capture());

    ReadListener listener = listenerCaptor.getValue();

    listener.onError(new RuntimeException("boom"));

    Mockito.verify(asyncContext).complete();
  }

  public void testReadListenerOverflowThrowsIOException ()
    throws Exception {

    initServletWithMocks();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("4");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(true);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(4);
    Mockito.when(inputStream.read()).thenReturn((int)'x');

    ReadListener listener = postAndCaptureListener(request, response, inputStream);

    try {
      listener.onDataAvailable();
      Assert.fail("Expected IOException on overflow");
    } catch (IOException ioException) {
      Assert.assertTrue(ioException.getMessage().contains("exceeds the declared content length"));
    }
  }

  public void testReadListenerDeliversOnceWhenAllDataReadRepeats ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = initServletWithMocks();
    Codec<OrthodoxValue> codec = Mockito.mock(Codec.class);

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(server.getMessageLogLevel()).thenReturn(Level.OFF);
    Mockito.when(codec.from(Mockito.any(byte[].class))).thenReturn(new Message[0]);
    Mockito.when(request.getHeader("Content-Length")).thenReturn("4");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(true);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(4);
    Mockito.when(inputStream.read()).thenReturn(-1);

    ReadListener listener = postAndCaptureListener(request, response, inputStream);

    // The container reports all data read before the buffered body is consumed, then reports it again.
    listener.onAllDataRead();
    listener.onAllDataRead();

    Mockito.verify(asyncContext, Mockito.timeout(2000)).complete();
    Mockito.verify(codec, Mockito.times(1)).from(Mockito.any(byte[].class));
    Mockito.verify(asyncContext, Mockito.times(1)).complete();
  }

  public void testReadListenerWaitsWhenContentIsIncompleteAndStreamIsNotFinished ()
    throws Exception {

    initServletWithMocks();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("4");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(true).thenReturn(false);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(2);
    Mockito.when(inputStream.isFinished()).thenReturn(false);

    ReadListener listener = postAndCaptureListener(request, response, inputStream);

    listener.onAllDataRead();

    Mockito.verify(asyncContext, Mockito.never()).complete();
  }

  public void testReadListenerShortContentOnFinishedStreamThrowsIOException ()
    throws Exception {

    initServletWithMocks();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("4");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(true);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(2).thenReturn(-1);
    Mockito.when(inputStream.isFinished()).thenReturn(true);

    ReadListener listener = postAndCaptureListener(request, response, inputStream);

    try {
      listener.onAllDataRead();
      Assert.fail("Expected IOException on short content");
    } catch (IOException ioException) {
      Assert.assertTrue(ioException.getMessage().contains("fewer bytes(2) than the declared content length(4)"));
    }
  }

  // The servlet builds its own executor bound to the server's scope and never consults the server's executor, so a
  // caller-supplied, unbound executor cannot leave the message task unbound.
  public void testMessageTaskRunsInsideTheServersScopeEvenWhenAnUnboundExecutorWasSupplied ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    OumuamuaServer<OrthodoxValue> server = initServletWithMocks(perApplicationScope);
    ExecutorService suppliedExecutorService = Mockito.mock(ExecutorService.class);
    Codec<OrthodoxValue> codec = Mockito.mock(Codec.class);
    AtomicReference<Object> seenOnComplete = new AtomicReference<>();

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));
    Mockito.when(server.getExecutorService()).thenReturn(suppliedExecutorService);
    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(server.getMessageLogLevel()).thenReturn(Level.OFF);
    Mockito.when(codec.from(Mockito.any(byte[].class))).thenReturn(new Message[0]);
    Mockito.doAnswer(invocation -> {
      seenOnComplete.set(probeScope());

      return null;
    }).when(asyncContext).complete();
    Mockito.when(request.getHeader("Content-Length")).thenReturn("4");
    Mockito.when(request.startAsync()).thenReturn(asyncContext);
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(true);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(4);
    Mockito.when(inputStream.read()).thenReturn(-1);

    ReadListener listener = postAndCaptureListener(request, response, inputStream);

    listener.onAllDataRead();

    Mockito.verify(asyncContext, Mockito.timeout(2000)).complete();
    Assert.assertEquals(seenOnComplete.get(), "scoped", "the message task must run on a thread bound to the server's scope");
    Mockito.verifyNoInteractions(suppliedExecutorService);
  }

  // The read listener hands the protocol listeners to the server to run bound, so a receipt listener sees the
  // configured scope, while the container thread delivering the callback, and the one that ran doPost, stay unbound.
  public void testOnAllDataReadRunsTheReceiptListenersInsideTheScopeAndLeavesTheContainerThreadUnbound ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingProtocolListener protocolListener = new RecordingProtocolListener();
    OumuamuaConfiguration<OrthodoxValue> configuration = new OumuamuaConfiguration<>();
    OumuamuaServer<OrthodoxValue> server;
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);
    AtomicReference<Boolean> boundOnStartAsync = new AtomicReference<>();
    AtomicReference<Boolean> boundOnCallbackRead = new AtomicReference<>();
    byte[] content = "[]".getBytes(StandardCharsets.UTF_8);

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));
    configuration.setCodec(new OrthodoxCodec(new JaxbDeserializer<>()));
    configuration.setProtocols(new Protocol[] {new ServletProtocol<OrthodoxValue>(30000L, new Protocol.ProtocolListener[] {protocolListener})});
    configuration.setPerApplicationScope(perApplicationScope);
    // The spy keeps the real scope wiring and runBound(); only the maintenance threads start() would launch are skipped.
    server = Mockito.spy(new OumuamuaServer<>(configuration));
    Mockito.doNothing().when(server).start(Mockito.any());
    // destroy() calls server.stop(), which would reach the idle executors start() never created.
    Mockito.doNothing().when(server).stop();

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(server);
    Mockito.when(request.getHeader("Content-Length")).thenReturn(String.valueOf(content.length));
    Mockito.when(request.getInputStream()).thenReturn(inputStream);
    Mockito.when(inputStream.isReady()).thenReturn(true);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
      //  Called from onAllDataRead(), so this records the binding of the thread delivering the callback.
      boundOnCallbackRead.set(PerApplicationScope.isBound());
      System.arraycopy(content, 0, invocation.getArgument(0), 0, content.length);

      return content.length;
    });
    Mockito.when(inputStream.read()).thenReturn(-1);
    Mockito.when(request.startAsync()).thenAnswer(invocation -> {
      boundOnStartAsync.set(PerApplicationScope.isBound());

      return asyncContext;
    });

    servlet.init(servletConfig);

    try {

      ReadListener listener = postAndCaptureListener(request, response, inputStream);

      listener.onAllDataRead();

      Assert.assertEquals(protocolListener.seenOnReceipt.get(), "scoped", "the receipt listener must run inside the server's scope");
      Assert.assertEquals(boundOnStartAsync.get(), Boolean.FALSE, "the request thread must not be bound by doPost");
      Assert.assertEquals(boundOnCallbackRead.get(), Boolean.FALSE, "the thread delivering the callback must read the content unbound");
      Mockito.verify(asyncContext, Mockito.timeout(2000)).complete();
    } finally {
      servlet.destroy();
      server.getExecutorService().shutdownNow();
    }
  }

  private ReadListener postAndCaptureListener (HttpServletRequest request, HttpServletResponse response, ServletInputStream inputStream)
    throws IOException {

    ArgumentCaptor<ReadListener> listenerCaptor = ArgumentCaptor.forClass(ReadListener.class);

    servlet.doPost(request, response);

    Mockito.verify(inputStream).setReadListener(listenerCaptor.capture());

    return listenerCaptor.getValue();
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  private static class RecordingProtocolListener implements Protocol.ProtocolListener<OrthodoxValue> {

    private final AtomicReference<Object> seenOnReceipt = new AtomicReference<>();

    @Override
    public void onReceipt (Message<OrthodoxValue>[] incomingMessages) {

      try {
        seenOnReceipt.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
      } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
        seenOnReceipt.set(missingPerApplicationScopeException);
      }
    }

    @Override
    public void onPublish (Message<OrthodoxValue> originatingMessage, Message<OrthodoxValue> outgoingMessage) {

    }

    @Override
    public void onDelivery (Packet<OrthodoxValue> outgoingPacket) {

    }
  }
}
