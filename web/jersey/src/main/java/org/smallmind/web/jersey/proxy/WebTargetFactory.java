/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.web.jersey.fault.FaultTranslatingClientResponseFilter;
import org.smallmind.web.jersey.jackson.JsonProvider;

public class WebTargetFactory {

  public static WebTarget manufacture (HttpProtocol protocol, String host, int port, String context)
    throws NoSuchAlgorithmException, MalformedURLException, URISyntaxException {

    Client client;
    ClientConfig clientConfig = new ClientConfig();
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(StandardCharsets.UTF_8).build());
    connectionManager.setMaxTotal(3);
    connectionManager.setDefaultMaxPerRoute(3);

    clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
    clientConfig.connectorProvider(new ApacheConnectorProvider());

    switch (protocol) {
      case HTTP:
        client = ClientBuilder.newClient(clientConfig);
        break;
      case HTTPS:
        System.setProperty("https.protocols", "TLSv1");
        client = ClientBuilder.newBuilder().withConfig(clientConfig).hostnameVerifier(new TrustAllHostNameVerifier()).sslContext(SSLContext.getInstance("TLSv1")).build();
        break;
      default:
        throw new UnknownSwitchCaseException(protocol.name());
    }

    client.register(JsonProvider.class).register(new FaultTranslatingClientResponseFilter());
    client.property(ClientProperties.CONNECT_TIMEOUT, 20000);
    client.property(ClientProperties.READ_TIMEOUT, 20000);

    return client.target(new URL(protocol.getScheme(), host, (port > 0) ? port : protocol.getPort(), context).toURI());
  }
}
