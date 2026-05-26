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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
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
}
