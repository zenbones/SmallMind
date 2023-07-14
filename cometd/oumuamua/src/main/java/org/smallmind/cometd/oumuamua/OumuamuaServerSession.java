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
package org.smallmind.cometd.oumuamua;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerTransport;
import org.smallmind.cometd.oumuamua.transport.OumuamuaCarrier;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaServerSession implements ServerSession {

  private final HashMap<String, Object> attributeMap = new HashMap<>();
  private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
  private final OumuamuaTransport serverTransport;
  private final OumuamuaCarrier carrier;
  private final LocalSession localSession;
  private final String id;
  private Boolean metaConnectDeliveryOnly;
  private boolean handshook;
  private boolean connected;
  private boolean broadcastToPublisher;
  private long interval = -1;
  private long timeout = -1;
  private long maxInterval = -1;

  public OumuamuaServerSession (OumuamuaTransport serverTransport, OumuamuaCarrier carrier) {

    this.serverTransport = serverTransport;
    this.carrier = carrier;

    id = SnowflakeId.newInstance().generateHexEncoding();
    localSession = null;
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public boolean isHandshook () {

    return handshook;
  }

  public void setHandshook (boolean handshook) {

    this.handshook = handshook;
  }

  @Override
  public boolean isConnected () {

    return connected;
  }

  public void setConnected (boolean connected) {

    this.connected = connected;
  }

  @Override
  public String getUserAgent () {

    return carrier.getUserAgent();
  }

  @Override
  public long getInterval () {

    return interval;
  }

  @Override
  public void setInterval (long interval) {

    this.interval = interval;
  }

  @Override
  public long getTimeout () {

    return timeout;
  }

  @Override
  public void setTimeout (long timeout) {

    this.timeout = timeout;
  }

  @Override
  public long getMaxInterval () {

    return maxInterval;
  }

  @Override
  public void setMaxInterval (long maxInterval) {

    this.maxInterval = maxInterval;
  }

  @Override
  public boolean isMetaConnectDeliveryOnly () {

    return metaConnectDeliveryOnly;
  }

  @Override
  public void setMetaConnectDeliveryOnly (boolean metaConnectDeliveryOnly) {

    this.metaConnectDeliveryOnly = metaConnectDeliveryOnly;
  }

  @Override
  public boolean isBroadcastToPublisher () {

    return broadcastToPublisher;
  }

  @Override
  public void setBroadcastToPublisher (boolean broadcastToPublisher) {

    this.broadcastToPublisher = broadcastToPublisher;
  }

  @Override
  public ServerTransport getServerTransport () {

    return serverTransport;
  }

  @Override
  public boolean isLocalSession () {

    return localSession != null;
  }

  @Override
  public LocalSession getLocalSession () {

    return localSession;
  }

  @Override
  public void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  @Override
  public Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  @Override
  public Set<String> getAttributeNames () {

    return attributeMap.keySet();
  }

  @Override
  public Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }

  @Override
  public void addExtension (Extension extension) {

  }

  @Override
  public void removeExtension (Extension extension) {

  }

  @Override
  public List<Extension> getExtensions () {

    return null;
  }

  @Override
  public void addListener (ServerSessionListener serverSessionListener) {

  }

  @Override
  public void removeListener (ServerSessionListener serverSessionListener) {

  }

  @Override
  public Set<ServerChannel> getSubscriptions () {

    return null;
  }

  @Override
  public void deliver (Session session, ServerMessage.Mutable mutable, Promise<Boolean> promise) {

  }

  @Override
  public void deliver (Session session, String s, Object o, Promise<Boolean> promise) {

  }

  public String poll () {

    return messageQueue.poll();
  }

  public void send (String text) {

    if ((metaConnectDeliveryOnly == null) ? serverTransport.isMetaConnectDeliveryOnly() : metaConnectDeliveryOnly) {
      messageQueue.add(text);
    } else {
      try {
        carrier.send("[" + text + "]");
      } catch (Exception exception) {
        LoggerManager.getLogger(OumuamuaServerSession.class).error(exception);
      }
    }
  }

  @Override
  public void disconnect () {

  }

  @Override
  public void batch (Runnable batch) {

  }

  @Override
  public void startBatch () {

  }

  @Override
  public boolean endBatch () {

    return false;
  }
}
