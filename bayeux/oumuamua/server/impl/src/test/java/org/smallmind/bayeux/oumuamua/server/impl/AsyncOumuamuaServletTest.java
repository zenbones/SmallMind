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
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.LongPollingTransport;
import org.smallmind.bayeux.oumuamua.server.impl.longpolling.ServletProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AsyncOumuamuaServletTest {

  private AsyncOumuamuaServlet<OrthodoxValue> servlet;

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
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
  private OumuamuaServer<OrthodoxValue> initServletWithMocks ()
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

    servlet.init(servletConfig);

    return server;
  }

  public void testDestroyShutsDownServer ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = initServletWithMocks();

    servlet.destroy();

    Mockito.verify(server).stop();
  }

  @SuppressWarnings("unchecked")
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
  }

  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
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
    Mockito.when(inputStream.isReady()).thenReturn(true).thenReturn(false);
    Mockito.when(inputStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(4);

    ArgumentCaptor<ReadListener> listenerCaptor = ArgumentCaptor.forClass(ReadListener.class);

    servlet.doPost(request, response);

    Mockito.verify(inputStream, Mockito.timeout(2000)).setReadListener(listenerCaptor.capture());

    ReadListener listener = listenerCaptor.getValue();

    Mockito.verify(inputStream, Mockito.timeout(2000).atLeastOnce()).read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());

    try {
      listener.onDataAvailable();
      Assert.fail("Expected IOException on overflow");
    } catch (IOException ioException) {
      Assert.assertTrue(ioException.getMessage().contains("exceeds the declared content length"));
    }
  }
}
