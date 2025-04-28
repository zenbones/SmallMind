/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.api;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

public interface Server<V extends Value<V>> extends Attributed {

  String ATTRIBUTE = "org.smallmind.bayeux.oumuamua.server";

  interface Listener<V extends Value<V>> {

  }

  interface SessionListener<V extends Value<V>> extends Listener<V> {

    void onConnected (Session<V> session);

    void onDisconnected (Session<V> session);
  }

  interface ChannelListener<V extends Value<V>> extends Listener<V> {

    void onCreated (Channel<V> channel);

    void onRemoved (Channel<V> channel);
  }

  interface SubscriptionListener<V extends Value<V>> extends Listener<V> {

    void onSubscribed (Channel<V> channel, Session<V> session);

    void onUnsubscribed (Channel<V> channel, Session<V> session);
  }

  interface PacketListener<V extends Value<V>> extends Listener<V> {

    Packet<V> onRequest (Session<V> sender, Packet<V> packet);

    Packet<V> onResponse (Session<V> sender, Packet<V> packet);

    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  void addService (BayeuxService<V> service);

  void removeService (Route route);

  BayeuxService<V> getService (Route route);

  void addListener (Listener<V> listener);

  void removeListener (Listener<V> listener);

  void start (ServletConfig servletConfig)
    throws ServletException;

  void stop ();

  String getBayeuxVersion ();

  String getMinimumBayeuxVersion ();

  long getSessionConnectionIntervalMilliseconds ();

  boolean allowsImplicitConnection ();

  String[] getProtocolNames ();

  Protocol<V> getProtocol (String name);

  Backbone<V> getBackbone ();

  SecurityPolicy<V> getSecurityPolicy ();

  // Serves as an injection point for implementations that wish to add client configurable additions to the json codec pipeline
  Codec<V> getCodec ();

  boolean isReflecting (Route route);

  boolean isStreaming (Route route);

  Session<V> getSession (String sessionId);

  void addInitializer (ChannelInitializer<V> initializer);

  void removeInitializer (ChannelInitializer<V> initializer);

  Channel<V> findChannel (String path)
    throws InvalidPathException;

  // Initializers will be applied before the channel is returned if this call creates the channel
  Channel<V> requireChannel (String path, ChannelInitializer... initializers)
    throws InvalidPathException;

  // It's an error to attempt to remove a persistent channel
  // All sessions must be unsubscribed (and listeners notified) upon channel removal
  void removeChannel (Channel<V> channel)
    throws ChannelStateException;

  Packet<V> onRequest (Session<V> sender, Packet<V> packet);

  Packet<V> onResponse (Session<V> sender, Packet<V> packet);

  void deliver (Session<V> sender, Packet<V> packet, boolean clustered);

  void forward (Channel<V> channel, Packet<V> packet);
}
