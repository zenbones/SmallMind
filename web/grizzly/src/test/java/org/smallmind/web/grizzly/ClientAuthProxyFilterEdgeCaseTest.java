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
 * Covers the malformed, missing, and error edge branches of {@link ClientAuthProxyFilter} that the existing happy-path
 * certificate test does not reach: an unresolved peer address, a peer-certificate retrieval that throws, a non-X.509
 * certificate whose conversion fails, and a peer certificate that carries no subject DN. Each path must still pass the
 * request through unchanged via the invoke action.
 */
@Test(groups = "integration")
public class ClientAuthProxyFilterEdgeCaseTest {

  private FilterChainContext mockContext (HttpContent httpContent, NextAction invokeAction, InetSocketAddress peerAddress) {

    FilterChainContext context = Mockito.mock(FilterChainContext.class);
    Connection<?> connection = Mockito.mock(Connection.class);

    Mockito.when(context.getMessage()).thenReturn(httpContent);
    Mockito.when(context.getInvokeAction()).thenReturn(invokeAction);
    Mockito.when(context.getConnection()).thenReturn(connection);
    Mockito.when(connection.getPeerAddress()).thenReturn(peerAddress);

    return context;
  }

  public void testUnresolvedPeerAddressSkipsForwardedForHeader ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    // An unresolved socket address carries no InetAddress, so the X-Forwarded-For header must not be written.
    InetSocketAddress peerAddress = InetSocketAddress.createUnresolved("unknown.host", 8443);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(null);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      Mockito.verify(httpHeader, Mockito.never()).setHeader(Mockito.eq(ClientAuthProxyFilter.X_FORWARDED_FOR), Mockito.anyString());
    }
  }

  public void testPeerCertificateRetrievalErrorSkipsIdentityInjection ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("10.1.2.3", 7000);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    SSLEngine sslEngine = Mockito.mock(SSLEngine.class);
    SSLSession sslSession = Mockito.mock(SSLSession.class);

    Mockito.when(sslEngine.getSession()).thenReturn(sslSession);
    // Tyrus/JSSE throws when no peer certificates are available; the filter swallows it and treats the chain as null.
    Mockito.when(sslSession.getPeerCertificates()).thenThrow(new javax.net.ssl.SSLPeerUnverifiedException("no peer certificate"));

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(sslEngine);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      // The IP is still forwarded, but the unverified peer means no certificate identity is added.
      Mockito.verify(httpHeader).setHeader(ClientAuthProxyFilter.X_FORWARDED_FOR, "10.1.2.3");
      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_VERIFY), Mockito.anyString());
    }
  }

  public void testNonX509CertificateThatFailsConversionSkipsIdentityInjection ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("10.4.5.6", 7100);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    SSLEngine sslEngine = Mockito.mock(SSLEngine.class);
    SSLSession sslSession = Mockito.mock(SSLSession.class);
    // A non-X.509 certificate whose encoded bytes are not parseable forces the CertificateFactory conversion to fail,
    // which the filter treats as a null chain and skips identity injection.
    Certificate bogusCertificate = Mockito.mock(Certificate.class);

    Mockito.when(sslEngine.getSession()).thenReturn(sslSession);
    Mockito.when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] {bogusCertificate});
    Mockito.when(bogusCertificate.getEncoded()).thenReturn(new byte[] {1, 2, 3, 4});

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(sslEngine);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      Mockito.verify(httpHeader).setHeader(ClientAuthProxyFilter.X_FORWARDED_FOR, "10.4.5.6");
      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_VERIFY), Mockito.anyString());
    }
  }

  public void testCertificateWithNullSubjectDnSkipsIdentityInjection ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("10.7.8.9", 7200);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    SSLEngine sslEngine = Mockito.mock(SSLEngine.class);
    SSLSession sslSession = Mockito.mock(SSLSession.class);
    X509Certificate peerCertificate = Mockito.mock(X509Certificate.class);

    Mockito.when(sslEngine.getSession()).thenReturn(sslSession);
    Mockito.when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] {peerCertificate});
    // A peer certificate with no subject DN must short-circuit before any identity header is written.
    Mockito.when(peerCertificate.getSubjectDN()).thenReturn(null);

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(sslEngine);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_S_DN), Mockito.anyString());
      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_VERIFY), Mockito.anyString());
    }
  }

  public void testCertificateWithNullIssuerDnStillSetsSubjectAndVerify ()
    throws Exception {

    HttpContent httpContent = Mockito.mock(HttpContent.class);
    HttpHeader httpHeader = Mockito.mock(HttpHeader.class);
    NextAction invokeAction = Mockito.mock(NextAction.class);
    InetSocketAddress peerAddress = new InetSocketAddress("10.10.11.12", 7300);

    Mockito.when(httpContent.getHttpHeader()).thenReturn(httpHeader);

    SSLEngine sslEngine = Mockito.mock(SSLEngine.class);
    SSLSession sslSession = Mockito.mock(SSLSession.class);
    X509Certificate peerCertificate = Mockito.mock(X509Certificate.class);
    Principal subject = Mockito.mock(Principal.class);

    Mockito.when(sslEngine.getSession()).thenReturn(sslSession);
    Mockito.when(sslSession.getPeerCertificates()).thenReturn(new Certificate[] {peerCertificate});
    Mockito.when(peerCertificate.getSubjectDN()).thenReturn(subject);
    Mockito.when(subject.getName()).thenReturn("CN=subject-only");
    // A null issuer DN exercises the branch that skips the issuer header while still writing subject and verify status.
    Mockito.when(peerCertificate.getIssuerDN()).thenReturn(null);

    FilterChainContext context = mockContext(httpContent, invokeAction, peerAddress);

    try (MockedStatic<SSLUtils> sslUtilsStatic = Mockito.mockStatic(SSLUtils.class)) {

      sslUtilsStatic.when(() -> SSLUtils.getSSLEngine(Mockito.any())).thenReturn(sslEngine);

      ClientAuthProxyFilter filter = new ClientAuthProxyFilter(false);

      Assert.assertSame(filter.handleRead(context), invokeAction);

      Mockito.verify(httpHeader).addHeader(ClientAuthProxyFilter.SSL_CLIENT_S_DN, "CN=subject-only");
      Mockito.verify(httpHeader, Mockito.never()).addHeader(Mockito.eq(ClientAuthProxyFilter.SSL_CLIENT_I_DN), Mockito.anyString());
      Mockito.verify(httpHeader).addHeader(ClientAuthProxyFilter.SSL_CLIENT_VERIFY, "SUCCESS");
    }
  }
}
