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
package org.smallmind.cometd.oumuamua.message;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerTransport;

public class RemoteServerSession implements ServerSession {

  private final HashMap<String, Object> attributeMap;
  private final String id;
  private final String userAgent;
  private final boolean metaConnectDeliveryOnly;
  private final boolean broadcastToPublisher;
  private final long interval;
  private final long timeout;
  private final long maxInterval;

  public RemoteServerSession (String id, String userAgent, HashMap<String, Object> attributeMap, boolean metaConnectDeliveryOnly, boolean broadcastToPublisher, long interval, long timeout, long maxInterval) {

    this.id = id;
    this.userAgent = userAgent;
    this.attributeMap = attributeMap;
    this.metaConnectDeliveryOnly = metaConnectDeliveryOnly;
    this.broadcastToPublisher = broadcastToPublisher;
    this.interval = interval;
    this.timeout = timeout;
    this.maxInterval = maxInterval;
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public String getUserAgent () {

    return userAgent;
  }

  @Override
  public long getInterval () {

    return interval;
  }

  @Override
  public void setInterval (long interval) {

    throw new UnsupportedOperationException();
  }

  @Override
  public long getTimeout () {

    return timeout;
  }

  @Override
  public void setTimeout (long timeout) {

    throw new UnsupportedOperationException();
  }

  @Override
  public long getMaxInterval () {

    return maxInterval;
  }

  @Override
  public void setMaxInterval (long maxInterval) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isMetaConnectDeliveryOnly () {

    return metaConnectDeliveryOnly;
  }

  @Override
  public void setMetaConnectDeliveryOnly (boolean metaConnectDeliveryOnly) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isBroadcastToPublisher () {

    return broadcastToPublisher;
  }

  @Override
  public void setBroadcastToPublisher (boolean broadcastToPublisher) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isConnected () {

    return true;
  }

  @Override
  public boolean isHandshook () {

    return true;
  }

  @Override
  public void disconnect () {

    throw new UnsupportedOperationException();
  }

  @Override
  public ServerTransport getServerTransport () {

    return null;
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
  public Set<ServerChannel> getSubscriptions () {

    return null;
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
  public List<Extension> getExtensions () {

    return null;
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
  public void addListener (ServerSessionListener listener) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void removeListener (ServerSessionListener listener) {

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
  public void batch (Runnable batch) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void startBatch () {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean endBatch () {

    return false;
  }
}