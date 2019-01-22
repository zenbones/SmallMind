/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.web.jersey.proxy;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.smallmind.nutsnbolts.ssl.NaiveHostNameVerifier;

public class JsonTargetFactory {

  public static JsonTarget manufacture (String host, int concurrencyLevel, int timeout)
    throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, KeyStoreException, KeyManagementException {

    return manufacture(HttpProtocol.HTTP, host, 0, null, concurrencyLevel, timeout);
  }

  public static JsonTarget manufacture (HttpProtocol protocol, String host, int concurrencyLevel, int timeout)
    throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, KeyStoreException, KeyManagementException {

    return manufacture(protocol, host, 0, null, concurrencyLevel, timeout);
  }

  public static JsonTarget manufacture (HttpProtocol protocol, String host, int port, int concurrencyLevel, int timeout)
    throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, KeyStoreException, KeyManagementException {

    return manufacture(protocol, host, port, null, concurrencyLevel, timeout);
  }

  public static JsonTarget manufacture (HttpProtocol protocol, String host, String context, int concurrencyLevel, int timeout)
    throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, KeyStoreException, KeyManagementException {

    return manufacture(protocol, host, 0, context, concurrencyLevel, timeout);
  }

  public static JsonTarget manufacture (HttpProtocol protocol, String host, int port, String context, int concurrencyLevel, int timeout)
    throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException, KeyStoreException, KeyManagementException {

    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    PoolingHttpClientConnectionManager connectionManager;
    Registry<ConnectionSocketFactory> socketFactoryRegistry;
    SSLConnectionSocketFactory sslSocketFactory;
    SSLContext sslContext;
    CloseableHttpClient httpClient;

    sslContext = SSLContextBuilder.create().loadTrustMaterial(null, (X509Certificate[] arg0, String arg1) -> true).build();
    clientBuilder.setSSLContext(sslContext);

    // use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
    sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NaiveHostNameVerifier());

    socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                              .register("http", PlainConnectionSocketFactory.getSocketFactory())
                              .register("https", sslSocketFactory)
                              .build();

    connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(StandardCharsets.UTF_8).build());
    connectionManager.setDefaultSocketConfig(SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(timeout).setTcpNoDelay(true).build());
    connectionManager.setDefaultMaxPerRoute(concurrencyLevel);
    connectionManager.setMaxTotal(concurrencyLevel);

    httpClient = clientBuilder.setConnectionManager(connectionManager).setRedirectStrategy(new ExtraLaxRedirectStrategy()).build();

    return new JsonTarget(httpClient, URI.create(protocol.getScheme() + "://" + host + ((port > 0) ? ":" + port : "") + ((context != null) ? context : "")));
  }
}
