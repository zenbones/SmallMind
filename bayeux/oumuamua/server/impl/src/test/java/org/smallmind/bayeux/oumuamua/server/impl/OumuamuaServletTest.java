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
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.ServletProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit-level coverage for the synchronous {@link OumuamuaServlet}. Mirrors the equivalent
 * coverage on {@link AsyncOumuamuaServletTest}: validates each early-failure branch of
 * {@link OumuamuaServlet#doPost} (missing, malformed, zero, and negative Content-Length) and the
 * {@link OumuamuaServlet#init} failure paths the container relies on for fail-fast wiring errors.
 */
@Test(groups = "unit")
public class OumuamuaServletTest {

  private OumuamuaServlet<OrthodoxValue> servlet;

  @BeforeMethod
  public void beforeMethod () {

    servlet = new OumuamuaServlet<>();
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

    Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid content length");
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

  @Test(expectedExceptions = ServletException.class, expectedExceptionsMessageRegExp = ".*OumuamuaServer.*OumuamuaServletContextListener.*")
  public void testInitThrowsServletExceptionWhenServerMissingFromContext ()
    throws ServletException {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(null);

    servlet.init(servletConfig);
  }

  public void testGetServletInfoDelegatesToSuper () {

    // Default servlet info on an uninitialised servlet is the empty string supplied by HttpServlet.
    // The accessor is included in the public API so we verify it returns non-null and does not throw.
    String info = servlet.getServletInfo();

    Assert.assertNotNull(info);
  }

  @Test(expectedExceptions = ServletException.class, expectedExceptionsMessageRegExp = ".*http protocol.*")
  public void testInitThrowsWhenServletProtocolMissing ()
    throws ServletException {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(server);
    Mockito.when(server.getProtocol(Mockito.anyString())).thenReturn(null);

    servlet.init(servletConfig);
  }

  @Test(expectedExceptions = ServletException.class, expectedExceptionsMessageRegExp = ".*long polling.*")
  public void testInitThrowsWhenLongPollingTransportMissing ()
    throws ServletException {

    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);
    Protocol<OrthodoxValue> protocol = Mockito.mock(Protocol.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(Server.ATTRIBUTE)).thenReturn(server);
    Mockito.when(server.getProtocol(Mockito.anyString())).thenReturn(protocol);
    Mockito.when(protocol.getTransport(Mockito.anyString())).thenReturn(null);

    servlet.init(servletConfig);
  }

  public void testDoPostSendsErrorWhenReadStreamUnderflows ()
    throws IOException {

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    ServletInputStream stream = Mockito.mock(ServletInputStream.class);

    Mockito.when(request.getHeader("Content-Length")).thenReturn("5");
    Mockito.when(request.getInputStream()).thenReturn(stream);
    Mockito.when(stream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(-1);

    servlet.doPost(request, response);

    Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to read the full content");
  }

  // The protocol listeners are handed to the server to run bound, so a receipt listener sees the configured scope,
  // while the container's request thread, which runs the rest of doPost, is never bound.
  public void testDoPostRunsTheReceiptListenersInsideTheScopeAndLeavesTheRequestThreadUnbound ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    RecordingProtocolListener protocolListener = new RecordingProtocolListener();
    OumuamuaConfiguration<OrthodoxValue> configuration = new OumuamuaConfiguration<>();
    OumuamuaServer<OrthodoxValue> server;
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    ServletInputStream inputStream = Mockito.mock(ServletInputStream.class);
    AsyncContext asyncContext = Mockito.mock(AsyncContext.class);
    AtomicReference<Boolean> boundOnStartAsync = new AtomicReference<>();
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
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation -> {
      System.arraycopy(content, 0, invocation.getArgument(0), 0, content.length);

      return content.length;
    });
    Mockito.when(request.startAsync()).thenAnswer(invocation -> {
      boundOnStartAsync.set(PerApplicationScope.isBound());

      return asyncContext;
    });

    servlet.init(servletConfig);

    try {
      servlet.doPost(request, response);

      Assert.assertEquals(protocolListener.seenOnReceipt.get(), "scoped", "the receipt listener must run inside the server's scope");
      Assert.assertEquals(boundOnStartAsync.get(), Boolean.FALSE, "the request thread must not remain bound once the listeners return");
      Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(), Mockito.anyString());
      Mockito.verify(asyncContext, Mockito.timeout(2000)).complete();
    } finally {
      servlet.destroy();
      server.getExecutorService().shutdownNow();
    }
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
