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
package org.smallmind.web.grizzly;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.ssl.SSLUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the non-proxy certificate-injection path of {@link ClientAuthProxyFilter}, faking the peer TLS session and
 * certificate chain on the filter chain context so that the filter strips spoofable headers and repopulates them from
 * the peer certificate.
 */
@Test(groups = "integration")
public class ClientAuthProxyFilterCertificateTest {

  private FilterChainContext mockContext (HttpContent httpContent, NextAction invokeAction, InetSocketAddress peerAddress) {

    FilterChainContext context = Mockito.mock(FilterChainContext.class);
    Connection<?> connection = Mockito.mock(Connection.class);

    Mockito.when(context.getMessage()).thenReturn(httpContent);
    Mockito.when(context.getInvokeAction()).thenReturn(invokeAction);
    Mockito.when(context.getConnection()).thenReturn(connection);
    Mockito.when(connection.getPeerAddress()).thenReturn(peerAddress);

    return context;
  }

  public void testCertificateIdentityInjectedWhenSslSessionPresent ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("192.168.1.5", 4321);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);
    // Pre-existing spoofed headers that must be cleared.
    Mockito.when(httpHeader.containsHeader(ClientAuthProxyFilter.SSL_CLIENT_S_DN)).thenReturn(true);
    Mockito.when(httpHeader.containsHeader(ClientAuthProxyFilter.X_FORWARDED_FOR)).thenReturn(true);

    SSLEngine sslEngine = Mockito.mock(SSLEngine.class);
    SSLSession sslSession = Mockito.mock(SSLSession.class);
    X509Certificate peerCertificate = Mockito.mock(X509Certificate.class);
    Principal subject = Mockito.mock(Principal.class);
    Principal issuer = Mockito.mock(Principal.class);

    Mockito.when(sslEngine.getSession()).thenReturn(sslSession);
    Mockito.when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] {peerCertificate});
    Mockito.when(peerCertificate.getSubjectDN()).thenReturn(subject);
    Mockito.when(subject.getName()).thenReturn("CN=client");
    Mockito.when(peerCertificate.getIssuerDN()).thenReturn(issuer);
    Mockito.when(issuer.getName()).thenReturn("CN=issuer");

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    // The HttpContent mock is itself an HttpPacket, so the filter's HttpPacket.isHttp check passes naturally.
    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(sslEngine);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      // Spoofable headers were cleared.
      Mockito.verify(httpHeader).setHeader(ClientAuthProxyFilter.SSL_CLIENT_S_DN, (org.glassfish.grizzly.http.util.HeaderValue)null);
      // Remote IP and certificate identity were injected.
      Mockito.verify(httpHeader).setHeader(ClientAuthProxyFilter.X_FORWARDED_FOR, "192.168.1.5");
      Mockito.verify(httpHeader).addHeader(ClientAuthProxyFilter.SSL_CLIENT_S_DN, "CN=client");
      Mockito.verify(httpHeader).addHeader(ClientAuthProxyFilter.SSL_CLIENT_I_DN, "CN=issuer");
      Mockito.verify(httpHeader).addHeader(ClientAuthProxyFilter.SSL_CLIENT_VERIFY, "SUCCESS");
    }
  }

  public void testNoSslEngineSkipsCertificateInjection ()
    throws IOException {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("10.0.0.1", 9999);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(null);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      // Without an SSL engine the IP is still forwarded, but no certificate identity is added.
      Mockito.verify(httpHeader).setHeader(ClientAuthProxyFilter.X_FORWARDED_FOR, "10.0.0.1");
      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_VERIFY), Mockito.anyString());
    }
  }

  public void testEmptyCertificateChainSkipsIdentityInjection ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("172.16.0.9", 1234);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    SSLEngine sslEngine = Mockito.mock(SSLEngine.class);
    SSLSession sslSession = Mockito.mock(SSLSession.class);

    Mockito.when(sslEngine.getSession()).thenReturn(sslSession);
    Mockito.when(sslSession.getPeerCertificates()).thenReturn(new Certificate[0]);

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(sslEngine);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_VERIFY), Mockito.anyString());
    }
  }
}
