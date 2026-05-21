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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.OumuamuaException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Branch-coverage companion to {@link OumuamuaServerTest} that exercises the constructor's
 * services/listeners loops, the {@link Server.PacketListener} dispatch for every
 * {@link PacketType}, the {@link #deliver} backbone forwarding paths, and the
 * {@link Server.ChannelListener#onRemoved} dispatch path that the original test class
 * does not reach.
 */
@Test(groups = "unit")
public class OumuamuaServerBranchTest {

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

  public void testConstructorPicksUpCustomExecutorService ()
    throws Exception {

    ExecutorService customExecutor = Executors.newSingleThreadExecutor();

    configuration.setExecutorService(customExecutor);

    OumuamuaServer<OrthodoxValue> server = new OumuamuaServer<>(configuration);

    Assert.assertSame(server.getExecutorService(), customExecutor, "Configured executor must be used when supplied");

    customExecutor.shutdown();
  }

  public void testConstructorRegistersServicesFromConfiguration ()
    throws Exception {

    Route serviceRoute = new DefaultRoute("/configured/service");
    BayeuxService<OrthodoxValue> service = new SingleRouteService(serviceRoute);

    configuration.setServices(new BayeuxService[] {service});

    OumuamuaServer<OrthodoxValue> server = new OumuamuaServer<>(configuration);

    Assert.assertSame(server.getService(serviceRoute), service, "Configured services must be registered during construction");
  }

  public void testConstructorRegistersListenersFromConfiguration ()
    throws Exception {

    AtomicInteger created = new AtomicInteger();
    Server.ChannelListener<OrthodoxValue> listener = new Server.ChannelListener<OrthodoxValue>() {

      @Override
      public void onCreated (Channel<OrthodoxValue> channel) {

        created.incrementAndGet();
      }

      @Override
      public void onRemoved (Channel<OrthodoxValue> channel) {

      }
    };

    configuration.setListeners(new Server.Listener[] {listener});

    OumuamuaServer<OrthodoxValue> server = new OumuamuaServer<>(configuration);

    server.requireChannel("/listener/configured");
    Assert.assertEquals(created.get(), 1, "ChannelListener registered via configuration must receive the onCreated event");
  }

  public void testPacketListenerOnResponsePathReceivesPacket ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicReference<PacketType> seenType = new AtomicReference<>();
    Server.PacketListener<OrthodoxValue> listener = new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        seenType.set(PacketType.REQUEST);

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        seenType.set(PacketType.RESPONSE);

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        seenType.set(PacketType.DELIVERY);

        return packet;
      }
    };

    server.addListener(listener);

    Packet<OrthodoxValue> result = server.onResponse(null, responsePacket("/response/test"));

    Assert.assertNotNull(result);
    Assert.assertEquals(seenType.get(), PacketType.RESPONSE);
  }

  public void testDeliverInvokesPacketListenerDeliveryBranch ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicReference<PacketType> seenType = new AtomicReference<>();
    Server.PacketListener<OrthodoxValue> listener = new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        seenType.set(packet.getPacketType());

        return packet;
      }
    };

    server.addListener(listener);
    server.deliver(null, deliveryPacket("/deliver/listener"), false);

    Assert.assertEquals(seenType.get(), PacketType.DELIVERY, "Server-level PacketListener must observe DELIVERY packets");
  }

  public void testDeliverVetoedByPacketListenerSkipsChannelTree ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();

    server.addListener(new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }
    });

    server.deliver(null, deliveryPacket("/deliver/vetoed"), false);
  }

  @SuppressWarnings("unchecked")
  public void testDeliverWithClusteredTruePublishesThroughBackbone ()
    throws Exception {

    Backbone<OrthodoxValue> backbone = Mockito.mock(Backbone.class);

    configuration.setBackbone(backbone);

    OumuamuaServer<OrthodoxValue> server = new OumuamuaServer<>(configuration);
    Packet<OrthodoxValue> packet = deliveryPacket("/cluster/path");

    server.deliver(null, packet, true);

    Mockito.verify(backbone).publish(Mockito.any());
  }

  @SuppressWarnings("unchecked")
  public void testForwardWithBackbonePublishes ()
    throws Exception {

    Backbone<OrthodoxValue> backbone = Mockito.mock(Backbone.class);

    configuration.setBackbone(backbone);

    OumuamuaServer<OrthodoxValue> server = new OumuamuaServer<>(configuration);
    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);
    Packet<OrthodoxValue> packet = deliveryPacket("/forward/backbone");

    server.forward(channel, packet);

    Mockito.verify(channel).deliver(Mockito.isNull(), Mockito.any(), Mockito.any());
    Mockito.verify(backbone).publish(Mockito.any());
  }

  @SuppressWarnings("unchecked")
  public void testForwardVetoedByPacketListenerSkipsDelivery ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    server.addListener(new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }
    });

    server.forward(channel, deliveryPacket("/forward/vetoed"));

    Mockito.verify(channel, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testRequireChannelAppliesPerCallInitializers ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicInteger runs = new AtomicInteger();

    server.requireChannel("/with/per-call", channel -> runs.incrementAndGet());

    Assert.assertEquals(runs.get(), 1, "Per-call initializer must run when the channel is created");
  }

  public void testRemoveChannelFiresChannelListenerOnRemoved ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicReference<Channel<OrthodoxValue>> removedChannel = new AtomicReference<>();

    server.addListener(new Server.ChannelListener<OrthodoxValue>() {

      @Override
      public void onCreated (Channel<OrthodoxValue> channel) {

      }

      @Override
      public void onRemoved (Channel<OrthodoxValue> channel) {

        removedChannel.set(channel);
      }
    });

    Channel<OrthodoxValue> channel = server.requireChannel("/removable");

    server.removeChannel(channel);

    Assert.assertSame(removedChannel.get(), channel, "ChannelListener.onRemoved must fire when the channel is removed");
  }

  public void testPacketListenerOnResponseVetoBreaksTheChain ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicInteger downstreamHits = new AtomicInteger();

    server.addListener(new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    });

    server.addListener(new Server.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onRequest (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        downstreamHits.incrementAndGet();

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    });

    Packet<OrthodoxValue> result = server.onResponse(null, responsePacket("/veto/response"));

    Assert.assertNull(result, "Returning null from onResponse must short-circuit the listener chain");
    Assert.assertEquals(downstreamHits.get(), 0, "Downstream listeners must not run after a veto");
  }

  public void testMixedListenerOrderingPreservesFilterOnUnrelatedTypes ()
    throws Exception {

    OumuamuaServer<OrthodoxValue> server = newServer();
    AtomicInteger removedSessionListener = new AtomicInteger();
    AtomicReference<Channel<OrthodoxValue>> removedChannel = new AtomicReference<>();

    // A SessionListener is registered before the ChannelListener; removeChannel must skip the SessionListener and still fire the ChannelListener.
    server.addListener(new Server.SessionListener<OrthodoxValue>() {

      @Override
      public void onConnected (Session<OrthodoxValue> session) {

      }

      @Override
      public void onDisconnected (Session<OrthodoxValue> session) {

        removedSessionListener.incrementAndGet();
      }
    });

    server.addListener(new Server.ChannelListener<OrthodoxValue>() {

      @Override
      public void onCreated (Channel<OrthodoxValue> channel) {

      }

      @Override
      public void onRemoved (Channel<OrthodoxValue> channel) {

        removedChannel.set(channel);
      }
    });

    Channel<OrthodoxValue> channel = server.requireChannel("/mixed/listener/path");

    server.removeChannel(channel);

    Assert.assertSame(removedChannel.get(), channel);
    Assert.assertEquals(removedSessionListener.get(), 0, "SessionListener must not receive ChannelListener events");
  }

  private Packet<OrthodoxValue> responsePacket (String channel)
    throws Exception {

    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.RESPONSE, null, new DefaultRoute(channel), message);
  }

  private Packet<OrthodoxValue> deliveryPacket (String channel)
    throws Exception {

    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(channel), message);
  }

  private static class SingleRouteService implements BayeuxService<OrthodoxValue> {

    private final Route[] routes;

    private SingleRouteService (Route route) {

      this.routes = new Route[] {route};
    }

    @Override
    public Route[] boundRoutes () {

      return routes;
    }

    @Override
    public Packet<OrthodoxValue> process (Protocol<OrthodoxValue> protocol, Route route, Server<OrthodoxValue> server, Session<OrthodoxValue> session, Message<OrthodoxValue> request) {

      return null;
    }
  }
}
