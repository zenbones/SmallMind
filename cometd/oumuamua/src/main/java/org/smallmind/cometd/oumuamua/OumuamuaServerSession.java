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

import java.util.List;
import java.util.Set;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerTransport;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class OumuamuaServerSession implements ServerSession {

  private final String id;
  private final String userAgent;
  private boolean handshook;
  private boolean connected;
  private long interval = -1;
  private long timeout = -1;
  private long maxInterval = -1;

  public OumuamuaServerSession () {

    this(null);
  }

  public OumuamuaServerSession (String userAgent) {

    this.userAgent = userAgent;

    id = SnowflakeId.newInstance().generateHexEncoding();
  }

  @Override
  public String getId () {

    return id;
  }

  @Override
  public boolean isConnected () {

    return connected;
  }

  @Override
  public boolean isHandshook () {

    return handshook;
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
  public Set<ServerChannel> getSubscriptions () {

    return null;
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
  public boolean isMetaConnectDeliveryOnly () {

    return false;
  }

  @Override
  public void setMetaConnectDeliveryOnly (boolean b) {

  }

  @Override
  public boolean isBroadcastToPublisher () {

    return false;
  }

  @Override
  public void setBroadcastToPublisher (boolean b) {

  }

  @Override
  public void setAttribute (String name, Object value) {

  }

  @Override
  public Object getAttribute (String name) {

    return null;
  }

  @Override
  public Set<String> getAttributeNames () {

    return null;
  }

  @Override
  public Object removeAttribute (String name) {

    return null;
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
  public void deliver (Session session, ServerMessage.Mutable mutable, Promise<Boolean> promise) {

  }

  @Override
  public void deliver (Session session, String s, Object o, Promise<Boolean> promise) {

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
