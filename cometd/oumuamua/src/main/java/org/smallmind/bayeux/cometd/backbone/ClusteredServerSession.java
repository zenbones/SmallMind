/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.bayeux.cometd.backbone;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerTransport;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.bayeux.cometd.session.OumuamuaServerSession;
import org.smallmind.bayeux.cometd.transport.OumuamuaCarrier;
import org.smallmind.bayeux.cometd.transport.OumuamuaTransport;

public class ClusteredServerSession implements OumuamuaServerSession {

  private final OumuamuaTransport serverTransport;
  private final String id;

  public ClusteredServerSession (OumuamuaServer oumuamuaServer, String transport, String id) {

    this.id = id;

    serverTransport = (OumuamuaTransport)oumuamuaServer.getTransport(transport);
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public OumuamuaCarrier getCarrier () {

    return null;
  }

  @Override
  public String[] getNegotiatedTransports () {

    return new String[0];
  }

  @Override
  public void setNegotiatedTransports (String[] negotiatedTransports) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isHandshook () {

    return true;
  }

  @Override
  public void setHandshook (boolean handshook) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isConnected () {

    return true;
  }

  @Override
  public void setConnected (boolean connected) {

    throw new UnsupportedOperationException();
  }

  @Override
  public String getUserAgent () {

    return null;
  }

  @Override
  public long getTimeout () {

    return serverTransport.getTimeout();
  }

  @Override
  public void setTimeout (long timeout) {

    throw new UnsupportedOperationException();
  }

  @Override
  public long getInterval () {

    return serverTransport.getInterval();
  }

  @Override
  public void setInterval (long interval) {

    throw new UnsupportedOperationException();
  }

  @Override
  public long getMaxInterval () {

    return serverTransport.getMaxInterval();
  }

  @Override
  public void setMaxInterval (long maxInterval) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isMetaConnectDeliveryOnly () {

    return false;
  }

  @Override
  public void setMetaConnectDeliveryOnly (boolean metaConnectDeliveryOnly) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isBroadcastToPublisher () {

    return false;
  }

  @Override
  public void setBroadcastToPublisher (boolean broadcastToPublisher) {

    throw new UnsupportedOperationException();
  }

  @Override
  public ServerTransport getServerTransport () {

    return serverTransport;
  }

  @Override
  public boolean isLocalSession () {

    return false;
  }

  @Override
  public LocalSession getLocalSession () {

    return null;
  }

  @Override
  public void setAttribute (String name, Object value) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Object getAttribute (String name) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getAttributeNames () {

    throw new UnsupportedOperationException();
  }

  @Override
  public Object removeAttribute (String name) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Extension> iterateExtensions () {

    throw new UnsupportedOperationException();
  }

  @Override
  public List<Extension> getExtensions () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void addExtension (Extension extension) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void removeExtension (Extension extension) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener (ServerSessionListener serverSessionListener) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void removeListener (ServerSessionListener serverSessionListener) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Set<ServerChannel> getSubscriptions () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void deliver (Session sender, ServerMessage.Mutable message, Promise<Boolean> promise) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void deliver (Session sender, String channel, Object data, Promise<Boolean> promise) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void disconnect () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void batch (Runnable batch) {

    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized void startBatch () {

    throw new UnsupportedOperationException();
  }

  @Override
  public synchronized boolean endBatch () {

    throw new UnsupportedOperationException();
  }

  @Override
  public OumuamuaPacket[] poll () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void send (OumuamuaPacket packet) {

    throw new UnsupportedOperationException();
  }
}
