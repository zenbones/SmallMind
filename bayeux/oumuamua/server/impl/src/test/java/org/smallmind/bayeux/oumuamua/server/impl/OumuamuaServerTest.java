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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.OumuamuaException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Direct unit tests for the public surface of {@link OumuamuaServer} that the existing
 * channel- and session-focused tests only exercise indirectly. The server is constructed
 * with a minimal configuration (codec + a single mock protocol) and the maintenance
 * threads are never started, so each test stays in-process and fast. Methods covered:
 * {@code findChannel}, {@code requireChannel}, {@code addService}/{@code getService}/
 * {@code removeService}, {@code addInitializer}, {@code addSession}/{@code getSession}/
 * {@code iterateSessions}, and the constructor's input validation.
 */
@Test(groups = "unit")
public class OumuamuaServerTest {

  private static final String PROTOCOL_NAME = "websocket";

  private OumuamuaConfiguration<OrthodoxValue> configuration;
  private Protocol<OrthodoxValue> protocol;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    protocol = Mockito.mock(Protocol.class);
    Mockito.when(protocol.getName()).thenReturn(PROTOCOL_NAME);

    configuration = new OumuamuaConfiguration<>();
    configuration.setCodec(new OrthodoxCodec(new JaxbDeserializer<>()));
    configuration.setProtocols(new Protocol[] {protocol});
  }

  private OumuamuaServer<OrthodoxValue> newServer ()
    throws OumuamuaException {

    return new OumuamuaServer<>(configuration);
  }

  @Test(expectedExceptions = OumuamuaException.class)
  public void testConstructorRejectsNullConfiguration ()
    throws OumuamuaException {

    new OumuamuaServer<>(null);
  }

  @Test(expectedExceptions = OumuamuaException.class)
  public void testConstructorRejectsMissingCodec ()
    throws OumuamuaException {

    OumuamuaConfiguration<OrthodoxValue> missingCodec = new OumuamuaConfiguration<>();

    missingCodec.setProtocols(new Protocol[] {protocol});

    new OumuamuaServer<>(missingCodec);
  }

  @Test(expectedExceptions = OumuamuaException.class)
  public void testConstructorRejectsMissingProtocols ()
    throws OumuamuaException {

    OumuamuaConfiguration<OrthodoxValue> missingProtocols = new OumuamuaConfiguration<>();

    missingProtocols.setCodec(new OrthodoxCodec(new JaxbDeserializer<>()));

    new OumuamuaServer<>(missingProtocols);
  }

  public void testProtocolNamesArePopulatedFromConfiguration ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();

    Assert.assertEquals(server.getProtocolNames(), new String[] {PROTOCOL_NAME});
    Assert.assertSame(server.getProtocol(PROTOCOL_NAME), protocol);
  }

  public void testFindChannelReturnsNullForUnknownPath ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();

    Assert.assertNull(server.findChannel("/unknown/path"));
  }

  public void testRequireChannelCreatesThenFindLocatesIt ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Channel<OrthodoxValue> created = server.requireChannel("/created");

    Assert.assertNotNull(created);
    Assert.assertSame(server.findChannel("/created"), created, "findChannel must locate the channel created by requireChannel");
  }

  public void testRequireChannelIsIdempotent ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Channel<OrthodoxValue> first = server.requireChannel("/idempotent");
    Channel<OrthodoxValue> second = server.requireChannel("/idempotent");

    Assert.assertSame(second, first, "requireChannel must return the existing channel on subsequent calls");
  }

  public void testRegisteredInitializerRunsOnRequireChannel ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicInteger invocations = new AtomicInteger();
    ChannelInitializer<OrthodoxValue> initializer = channel -> invocations.incrementAndGet();

    server.addInitializer(initializer);
    server.requireChannel("/with/initializer");

    Assert.assertEquals(invocations.get(), 1, "Server-level initializer must run when the channel is created");

    server.requireChannel("/with/initializer");

    Assert.assertEquals(invocations.get(), 1, "Server-level initializer must not run again for an existing channel");
  }

  public void testRemoveInitializerStopsItFromRunning ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicInteger invocations = new AtomicInteger();
    ChannelInitializer<OrthodoxValue> initializer = channel -> invocations.incrementAndGet();

    server.addInitializer(initializer);
    server.removeInitializer(initializer);
    server.requireChannel("/removed/initializer");

    Assert.assertEquals(invocations.get(), 0, "Removed initializer must not run");
  }

  public void testAddServiceBindsBoundRoutes ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Route echoRoute = new DefaultRoute("/service/echo");
    Route dataRoute = new DefaultRoute("/service/data");
    BayeuxService<OrthodoxValue> service = new TwoRouteService(echoRoute, dataRoute);

    server.addService(service);

    Assert.assertSame(server.getService(echoRoute), service);
    Assert.assertSame(server.getService(dataRoute), service);
  }

  public void testRemoveServiceClearsTheRoute ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Route route = new DefaultRoute("/service/temporary");
    BayeuxService<OrthodoxValue> service = new TwoRouteService(route, route);

    server.addService(service);
    server.removeService(route);

    Assert.assertNull(server.getService(route));
  }

  public void testAddServiceWithNullBoundRoutesIsNoOp ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    BayeuxService<OrthodoxValue> service = new TwoRouteService(null, null) {

      @Override
      public Route[] boundRoutes () {

        return null;
      }
    };

    server.addService(service);

    Assert.assertNull(server.getService(new DefaultRoute("/service/echo")));
  }

  public void testGetSessionReturnsNullForUnknownIdAndIterateSessionsIsEmptyByDefault ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();

    Assert.assertNull(server.getSession("not-a-real-id"));
    Assert.assertFalse(server.iterateSessions().hasNext());
  }

  public void testAllowsImplicitConnectionMirrorsConfiguration ()
    throws Exception {

    OumuamuaConfiguration<OrthodoxValue> implicitConfig = new OumuamuaConfiguration<>();

    implicitConfig.setCodec(new OrthodoxCodec(new JaxbDeserializer<>()));
    implicitConfig.setProtocols(new Protocol[] {protocol});
    implicitConfig.setAllowsImplicitConnection(true);

    Assert.assertTrue(new OumuamuaServer<>(implicitConfig).allowsImplicitConnection());
    Assert.assertFalse(newServer().allowsImplicitConnection());
  }

  public void testBayeuxVersionAccessors ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();

    Assert.assertEquals(server.getBayeuxVersion(), "1.0");
    Assert.assertEquals(server.getMinimumBayeuxVersion(), "1.0");
  }

  @SuppressWarnings("unchecked")
  private OumuamuaSession<OrthodoxValue> newSession (OumuamuaServer<OrthodoxValue> server) {

    Connection<OrthodoxValue> connection = Mockito.mock(Connection.class);
    Transport<OrthodoxValue> transport = Mockito.mock(Transport.class);
    Protocol<OrthodoxValue> connProtocol = Mockito.mock(Protocol.class);

    Mockito.when(connection.getTransport()).thenReturn(transport);
    Mockito.when(transport.getProtocol()).thenReturn(connProtocol);
    Mockito.when(connProtocol.isLongPolling()).thenReturn(false);

    return server.createSession(connection);
  }

  private Packet<OrthodoxValue> requestPacket (String channel)
    throws Exception {

    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.REQUEST, null, new DefaultRoute(channel), message);
  }

  private Packet<OrthodoxValue> deliveryPacket (String channel)
    throws Exception {

    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(channel), message);
  }

  public void testAddAndRemoveSessionExposedViaIterator ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    OumuamuaSession<OrthodoxValue> session = newSession(server);

    server.addSession(session);

    Assert.assertSame(server.getSession(session.getId()), session);
    Assert.assertTrue(server.iterateSessions().hasNext());

    server.removeSession(session);

    Assert.assertNull(server.getSession(session.getId()));
    Assert.assertFalse(server.iterateSessions().hasNext());
  }

  public void testChannelListenerFiredOnRequireChannel ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicReference<Channel<OrthodoxValue>> created = new AtomicReference<>();
    Server.ChannelListener<OrthodoxValue> listener = new Server.ChannelListener<OrthodoxValue>() {

      @Override
      public void onCreated (Channel<OrthodoxValue> channel) {

        created.set(channel);
      }

      @Override
      public void onRemoved (Channel<OrthodoxValue> channel) {

      }
    };

    server.addListener(listener);

    Channel<OrthodoxValue> channel = server.requireChannel("/listener/test");

    Assert.assertSame(created.get(), channel, "ChannelListener.onCreated must fire when the channel is first created");

    created.set(null);
    server.removeListener(listener);
    server.requireChannel("/listener/removed");

    Assert.assertNull(created.get(), "Removed ChannelListener must not fire");
  }

  public void testPacketListenerVetoesOnRequest ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Server.PacketListener<OrthodoxValue> vetoer = new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    };

    server.addListener(vetoer);

    Packet<OrthodoxValue> result = server.onRequest(null, requestPacket("/vetoed"));

    Assert.assertNull(result, "PacketListener returning null from onRequest must veto the packet");
  }

  public void testDeliverWithNullRouteIsNoOp ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> nullRoutePacket = new Packet<>(PacketType.DELIVERY, null, null, message);

    server.deliver(null, nullRoutePacket, false);
  }

  @SuppressWarnings("unchecked")
  public void testForwardWithNullRouteIsNoOp ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);
    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> nullRoutePacket = new Packet<>(PacketType.DELIVERY, null, null, message);

    server.forward(channel, nullRoutePacket);

    Mockito.verify(channel, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @SuppressWarnings("unchecked")
  public void testForwardWithNonNullRouteDeliversToChannel ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);
    Packet<OrthodoxValue> packet = deliveryPacket("/forward/test");

    server.forward(channel, packet);

    Mockito.verify(channel).deliver(Mockito.isNull(), Mockito.eq(packet), Mockito.any());
  }

  public void testGetMessageLogLevel ()
    throws Exception {

    Assert.assertEquals(newServer().getMessageLogLevel(), Level.TRACE);
  }

  @SuppressWarnings("unchecked")
  public void testStartCallsBackboneStartUpThenStopShutsItDown ()
    throws Exception {

    Backbone<OrthodoxValue> backbone = Mockito.mock(Backbone.class);

    configuration.setBackbone(backbone);

    OumuamuaServer<OrthodoxValue> server = newServer();
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);

    server.start(servletConfig);
    server.stop();

    Mockito.verify(backbone).startUp(server);
    Mockito.verify(backbone).shutDown();
    Mockito.verify(protocol).init(server, servletConfig);
  }

  @Test(expectedExceptions = ServletException.class)
  @SuppressWarnings("unchecked")
  public void testStartWrapsBackboneStartUpFailureInServletException ()
    throws Exception {

    Backbone<OrthodoxValue> backbone = Mockito.mock(Backbone.class);

    Mockito.doThrow(new RuntimeException("boom")).when(backbone).startUp(Mockito.any());
    configuration.setBackbone(backbone);

    OumuamuaServer<OrthodoxValue> server = newServer();
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);

    server.start(servletConfig);
  }

  @SuppressWarnings("unchecked")
  public void testStopLogsAndContinuesWhenBackboneShutDownFails ()
    throws Exception {

    Backbone<OrthodoxValue> backbone = Mockito.mock(Backbone.class);

    Mockito.doThrow(new RuntimeException("kaboom")).when(backbone).shutDown();
    configuration.setBackbone(backbone);

    OumuamuaServer<OrthodoxValue> server = newServer();
    ServletConfig servletConfig = Mockito.mock(ServletConfig.class);

    server.start(servletConfig);
    server.stop();

    Mockito.verify(backbone).shutDown();
  }

  private static class TwoRouteService implements BayeuxService<OrthodoxValue> {

    private final Route[] routes;

    private TwoRouteService (Route first, Route second) {

      this.routes = (first == second) ? new Route[] {first} : new Route[] {first, second};
    }

    @Override
    public Route[] boundRoutes () {

      return routes;
    }

    @Override
    public Packet<OrthodoxValue> process (Protocol<OrthodoxValue> protocol, Route route, Server<OrthodoxValue> server, org.smallmind.bayeux.oumuamua.server.api.Session<OrthodoxValue> session, Message<OrthodoxValue> request) {

      return null;
    }
  }
}
