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

import org.smallmind.bayeux.oumuamua.common.api.json.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;

public class OumuamuaConfiguration<V extends Value<V>> {

  private Backbone<V> backbone;
  private Codec<V> codec;
  private SecurityPolicy<V> securityPolicy;
  private Protocol<V>[] protocols;
  private long channelTimeToLiveMinutes = 30;
  private int idleChannelCycleMinutes = 5;
  private int connectionMaintenanceCycleMinutes = 1;
  private int maxLongPollQueueSize = 1000;

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

  public long getChannelTimeToLiveMinutes () {

    return channelTimeToLiveMinutes;
  }

  public void setChannelTimeToLiveMinutes (long channelTimeToLiveMinutes) {

    this.channelTimeToLiveMinutes = channelTimeToLiveMinutes;
  }

  public int getIdleChannelCycleMinutes () {

    return idleChannelCycleMinutes;
  }

  public void setIdleChannelCycleMinutes (int idleChannelCycleMinutes) {

    this.idleChannelCycleMinutes = idleChannelCycleMinutes;
  }

  public int getConnectionMaintenanceCycleMinutes () {

    return connectionMaintenanceCycleMinutes;
  }

  public void setConnectionMaintenanceCycleMinutes (int connectionMaintenanceCycleMinutes) {

    this.connectionMaintenanceCycleMinutes = connectionMaintenanceCycleMinutes;
  }

  public int getMaxLongPollQueueSize () {

    return maxLongPollQueueSize;
  }

  public void setMaxLongPollQueueSize (int maxLongPollQueueSize) {

    this.maxLongPollQueueSize = maxLongPollQueueSize;
  }
}
