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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class WebSocketTransportTest {

  private WebsocketConfiguration configuration;
  private WebsocketProtocol<OrthodoxValue> protocol;
  private WebSocketTransport<OrthodoxValue> transport;

  @BeforeMethod
  public void beforeMethod () {

    configuration = new WebsocketConfiguration(StubEndpoint.class, "/cometd");
    configuration.setMaxIdleTimeoutMilliseconds(60000L);
    configuration.setAsyncSendTimeoutMilliseconds(10000L);
    configuration.setMaximumTextMessageBufferSize(131072);

    protocol = new WebsocketProtocol<>(20000L, configuration);
    transport = (WebSocketTransport<OrthodoxValue>)protocol.getTransport(Transports.WEBSOCKET.getName());
  }

  public void testGetNameReturnsWebsocket () {

    Assert.assertEquals(transport.getName(), Transports.WEBSOCKET.getName());
  }

  public void testIsLocalReturnsFalse () {

    Assert.assertFalse(transport.isLocal());
  }

  public void testGetProtocolReturnsOwningProtocol () {

    Assert.assertSame(transport.getProtocol(), protocol);
  }

  public void testGetMaxIdleTimeoutDelegatesToConfiguration () {

    Assert.assertEquals(transport.getMaxIdleTimeoutMilliseconds(), 60000L);
  }

  public void testGetAsyncSendTimeoutDelegatesToConfiguration () {

    Assert.assertEquals(transport.getAsyncSendTimeoutMilliseconds(), 10000L);
  }

  public void testGetMaximumTextMessageBufferSizeDelegatesToConfiguration () {

    Assert.assertEquals(transport.getMaximumTextMessageBufferSize(), 131072);
  }

  public void testAttributeStorageOnTransport () {

    transport.setAttribute("key", "value");

    Assert.assertEquals(transport.getAttribute("key"), "value");

    transport.removeAttribute("key");

    Assert.assertNull(transport.getAttribute("key"));
  }

  public void testInitRegistersEndpointWithContainer ()
    throws Exception {

    Server<OrthodoxValue> server = Mockito.mock(Server.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    ServerContainer container = Mockito.mock(ServerContainer.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(container);

    transport.init(server, servletConfig);

    Mockito.verify(container).addEndpoint(Mockito.any(ServerEndpointConfig.class));
  }

  public void testInitRegistersServerAsUserProperty ()
    throws Exception {

    Server<OrthodoxValue> server = Mockito.mock(Server.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    ServerContainer container = Mockito.mock(ServerContainer.class);
    ArgumentCaptor<ServerEndpointConfig> captor = ArgumentCaptor.forClass(ServerEndpointConfig.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(container);

    transport.init(server, servletConfig);

    Mockito.verify(container).addEndpoint(captor.capture());
    Assert.assertSame(captor.getValue().getUserProperties().get(Server.ATTRIBUTE), server);
    Assert.assertSame(captor.getValue().getUserProperties().get(WebSocketTransport.ATTRIBUTE), transport);
  }

  public void testInitNormalizesUrlWithLeadingSlash ()
    throws Exception {

    WebsocketConfiguration cfg = new WebsocketConfiguration(StubEndpoint.class, "cometd");
    WebsocketProtocol<OrthodoxValue> proto = new WebsocketProtocol<>(20000L, cfg);
    WebSocketTransport<OrthodoxValue> t = (WebSocketTransport<OrthodoxValue>)proto.getTransport(Transports.WEBSOCKET.getName());

    Server<OrthodoxValue> server = Mockito.mock(Server.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    ServerContainer container = Mockito.mock(ServerContainer.class);
    ArgumentCaptor<ServerEndpointConfig> captor = ArgumentCaptor.forClass(ServerEndpointConfig.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(container);

    t.init(server, servletConfig);

    Mockito.verify(container).addEndpoint(captor.capture());
    Assert.assertEquals(captor.getValue().getPath(), "/cometd");
  }

  public void testInitStripsWildcardSuffix ()
    throws Exception {

    WebsocketConfiguration cfg = new WebsocketConfiguration(StubEndpoint.class, "/cometd/*");
    WebsocketProtocol<OrthodoxValue> proto = new WebsocketProtocol<>(20000L, cfg);
    WebSocketTransport<OrthodoxValue> t = (WebSocketTransport<OrthodoxValue>)proto.getTransport(Transports.WEBSOCKET.getName());

    Server<OrthodoxValue> server = Mockito.mock(Server.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    ServerContainer container = Mockito.mock(ServerContainer.class);
    ArgumentCaptor<ServerEndpointConfig> captor = ArgumentCaptor.forClass(ServerEndpointConfig.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(container);

    t.init(server, servletConfig);

    Mockito.verify(container).addEndpoint(captor.capture());
    Assert.assertEquals(captor.getValue().getPath(), "/cometd");
  }

  public void testInitPropagatesConfiguredSubProtocol ()
    throws Exception {

    WebsocketConfiguration subProtocolConfig = new WebsocketConfiguration(StubEndpoint.class, "/cometd");

    subProtocolConfig.setSubProtocol("bayeux");

    WebsocketProtocol<OrthodoxValue> subProtocolProtocol = new WebsocketProtocol<>(20000L, subProtocolConfig);
    WebSocketTransport<OrthodoxValue> subProtocolTransport = (WebSocketTransport<OrthodoxValue>)subProtocolProtocol.getTransport(Transports.WEBSOCKET.getName());

    Server<OrthodoxValue> server = Mockito.mock(Server.class);
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
    ServletContext servletContext = Mockito.mock(ServletContext.class);
    ServerContainer container = Mockito.mock(ServerContainer.class);
    ArgumentCaptor<ServerEndpointConfig> captor = ArgumentCaptor.forClass(ServerEndpointConfig.class);

    Mockito.when(servletConfig.getServletContext()).thenReturn(servletContext);
    Mockito.when(servletContext.getAttribute(ServerContainer.class.getName())).thenReturn(container);

    subProtocolTransport.init(server, servletConfig);

    Mockito.verify(container).addEndpoint(captor.capture());
    Assert.assertEquals(captor.getValue().getSubprotocols(), java.util.List.of("bayeux"), "Configured sub-protocol must be propagated to the registered endpoint");
  }

  private static final class StubEndpoint extends Endpoint {

    @Override
    public void onOpen (jakarta.websocket.Session session, jakarta.websocket.EndpointConfig config) {

    }
  }
}
