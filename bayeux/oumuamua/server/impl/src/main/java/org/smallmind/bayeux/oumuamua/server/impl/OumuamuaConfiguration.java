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
package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

public class OumuamuaConfiguration<V extends Value<V>> {

  private Backbone<V> backbone;
  private Codec<V> codec;
  private SecurityPolicy<V> securityPolicy;
  private Protocol<V>[] protocols;
  private Server.Listener<V>[] listeners;
  private long channelTimeToLiveMinutes = 30;
  private long threadPoolKeepAliveSeconds = 60;
  private int sessionConnectIntervalSeconds = 30;
  private int sessionMaxIdleTimeoutSeconds = 90;
  private int idleChannelCycleMinutes = 5;
  private int idleSessionCycleMinutes = 1;
  private int maxLongPollQueueSize = 1000;
  private int threadPoolCoreSize = 0;
  private int threadPoolMaximumSize = Integer.MAX_VALUE;

  public Backbone<V> getBackbone () {

    return backbone;
  }

  public void setBackbone (Backbone<V> backbone) {

    this.backbone = backbone;
  }

  public Codec<V> getCodec () {

    return codec;
  }

  public void setCodec (Codec<V> codec) {

    this.codec = codec;
  }

  public SecurityPolicy<V> getSecurityPolicy () {

    return securityPolicy;
  }

  public void setSecurityPolicy (SecurityPolicy<V> securityPolicy) {

    this.securityPolicy = securityPolicy;
  }

  public Protocol<V>[] getProtocols () {

    return protocols;
  }

  public void setProtocols (Protocol<V>[] protocols) {

    this.protocols = protocols;
  }

  public Server.Listener<V>[] getListeners () {

    return listeners;
  }

  public void setListeners (Server.Listener<V>[] listeners) {

    this.listeners = listeners;
  }

  public long getChannelTimeToLiveMinutes () {

    return channelTimeToLiveMinutes;
  }

  public void setChannelTimeToLiveMinutes (long channelTimeToLiveMinutes) {

    this.channelTimeToLiveMinutes = channelTimeToLiveMinutes;
  }

  public int getSessionConnectIntervalSeconds () {

    return sessionConnectIntervalSeconds;
  }

  public void setSessionConnectIntervalSeconds (int sessionConnectIntervalSeconds) {

    this.sessionConnectIntervalSeconds = sessionConnectIntervalSeconds;
  }

  public int getSessionMaxIdleTimeoutSeconds () {

    return sessionMaxIdleTimeoutSeconds;
  }

  public void setSessionMaxIdleTimeoutSeconds (int sessionMaxIdleTimeoutSeconds) {

    this.sessionMaxIdleTimeoutSeconds = sessionMaxIdleTimeoutSeconds;
  }

  public int getIdleChannelCycleMinutes () {

    return idleChannelCycleMinutes;
  }

  public void setIdleChannelCycleMinutes (int idleChannelCycleMinutes) {

    this.idleChannelCycleMinutes = idleChannelCycleMinutes;
  }

  public int getIdleSessionCycleMinutes () {

    return idleSessionCycleMinutes;
  }

  public void setIdleSessionCycleMinutes (int idleSessionCycleMinutes) {

    this.idleSessionCycleMinutes = idleSessionCycleMinutes;
  }

  public int getMaxLongPollQueueSize () {

    return maxLongPollQueueSize;
  }

  public void setMaxLongPollQueueSize (int maxLongPollQueueSize) {

    this.maxLongPollQueueSize = maxLongPollQueueSize;
  }

  public long getThreadPoolKeepAliveSeconds () {

    return threadPoolKeepAliveSeconds;
  }

  public void setThreadPoolKeepAliveSeconds (long threadPoolKeepAliveSeconds) {

    this.threadPoolKeepAliveSeconds = threadPoolKeepAliveSeconds;
  }

  public int getThreadPoolCoreSize () {

    return threadPoolCoreSize;
  }

  public void setThreadPoolCoreSize (int threadPoolCoreSize) {

    this.threadPoolCoreSize = threadPoolCoreSize;
  }

  public int getThreadPoolMaximumSize () {

    return threadPoolMaximumSize;
  }

  public void setThreadPoolMaximumSize (int threadPoolMaximumSize) {

    this.threadPoolMaximumSize = threadPoolMaximumSize;
  }
}
