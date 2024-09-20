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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.impl.json.ClassNameXmlAdapter;
import org.smallmind.nutsnbolts.util.MutationUtility;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.json.LevelEnumXmlAdapter;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.OUT;

@Doppelganger
public class OumuamuaConfiguration<V extends Value<V>> {

  @View(adapter = ClassNameXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Backbone<V> backbone;
  @View(adapter = ClassNameXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Codec<V> codec;
  @View(adapter = ClassNameXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private ExecutorService executorService;
  @View(adapter = ClassNameXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private SecurityPolicy<V> securityPolicy;
  private Protocol<V>[] protocols;
  private BayeuxService[] services;
  private Server.Listener<V>[] listeners;
  private String[][] reflectivePaths;
  private String[][] streamingPaths;
  @View(adapter = LevelEnumXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Level overflowLogLevel = Level.DEBUG;
  @View(idioms = @Idiom(visibility = OUT))
  private long channelTimeToLiveMinutes = 30;
  @View(idioms = @Idiom(visibility = OUT))
  private int sessionConnectIntervalSeconds = 30;
  @View(idioms = @Idiom(visibility = OUT))
  private int sessionMaxIdleTimeoutSeconds = 90;
  @View(idioms = @Idiom(visibility = OUT))
  private int idleChannelCycleMinutes = 5;
  @View(idioms = @Idiom(visibility = OUT))
  private int idleSessionCycleMinutes = 1;
  @View(idioms = @Idiom(visibility = OUT))
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

  public ExecutorService getExecutorService () {

    return executorService;
  }

  public void setExecutorService (ExecutorService executorService) {

    this.executorService = executorService;
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

  public BayeuxService[] getServices () {

    return services;
  }

  public void setServices (BayeuxService[] services) {

    this.services = services;
  }

  public Server.Listener<V>[] getListeners () {

    return listeners;
  }

  public void setListeners (Server.Listener<V>[] listeners) {

    this.listeners = listeners;
  }

  public Level getOverflowLogLevel () {

    return overflowLogLevel;
  }

  public void setOverflowLogLevel (Level overflowLogLevel) {

    this.overflowLogLevel = overflowLogLevel;
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

  public void setReflectivePaths (String[] paths) {

    LinkedList<String[]> reflectivePathList = decomposePaths(paths);

    reflectivePaths = new String[reflectivePathList.size()][];
    reflectivePathList.toArray(reflectivePaths);
  }

  public boolean isReflective (Route route) {

    return matchesPaths(reflectivePaths, route);
  }

  public void setStreamingPaths (String[] paths) {

    LinkedList<String[]> streamingPathList = decomposePaths(paths);

    streamingPaths = new String[streamingPathList.size()][];
    streamingPathList.toArray(streamingPaths);
  }

  public boolean isStreaming (Route route) {

    return matchesPaths(streamingPaths, route);
  }

  private LinkedList<String[]> decomposePaths (String[] paths) {

    LinkedList<String[]> pathList = new LinkedList<>();

    if (paths != null) {
      for (String path : paths) {
        if ((path != null) && (!path.isEmpty())) {
          pathList.add((path.charAt(0) == '/') ? path.substring(1).split("/", -1) : path.split("/", -1));
        }
      }
    }

    return pathList;
  }

  public boolean matchesPaths (String[][] paths, Route route) {

    if ((route != null) && (paths != null)) {
      for (String[] reflectivePath : paths) {
        if (route.matches(reflectivePath)) {

          return true;
        }
      }
    }

    return false;
  }

  @Override
  public String toString () {

    StringBuilder builder = new StringBuilder("\n{\n");

    if (backbone != null) {
      builder.append("  backbone: ").append(backbone.getClass().getName()).append(System.lineSeparator());
    }
    if (codec != null) {
      builder.append("  codec: ").append(codec.getClass().getName()).append(System.lineSeparator());
    }
    if (executorService != null) {
      builder.append("  executorService: ").append(executorService.getClass().getName()).append(System.lineSeparator());
    }
    if (securityPolicy != null) {
      builder.append("  securityPolicy: ").append(securityPolicy.getClass().getName()).append(System.lineSeparator());
    }
    if (protocols != null) {
      builder.append("  protocols: ").append(Arrays.toString(MutationUtility.toArray(protocols, String.class, protocol -> protocol.getClass().getName()))).append(System.lineSeparator());
    }
    if (services != null) {
      builder.append("  services: ").append(Arrays.toString(MutationUtility.toArray(services, String.class, service -> service.getClass().getName()))).append(System.lineSeparator());
    }
    if (listeners != null) {
      builder.append("  listeners: ").append(Arrays.toString(MutationUtility.toArray(listeners, String.class, listener -> listener.getClass().getName()))).append(System.lineSeparator());
    }
    if (reflectivePaths != null) {
      builder.append("  reflectivePaths: ").append(fromRoutes(reflectivePaths)).append(System.lineSeparator());
    }
    if (streamingPaths != null) {
      builder.append("  streamingPaths: ").append(fromRoutes(streamingPaths)).append(System.lineSeparator());
    }
    if (overflowLogLevel != null) {
      builder.append("  overflowLogLevel: ").append(overflowLogLevel.name()).append(System.lineSeparator());
    }
    builder.append("  channelTimeToLiveMinutes: ").append(channelTimeToLiveMinutes).append(System.lineSeparator());
    builder.append("  sessionConnectIntervalSeconds: ").append(sessionConnectIntervalSeconds).append(System.lineSeparator());
    builder.append("  sessionMaxIdleTimeoutSeconds: ").append(sessionMaxIdleTimeoutSeconds).append(System.lineSeparator());
    builder.append("  idleChannelCycleMinutes: ").append(idleChannelCycleMinutes).append(System.lineSeparator());
    builder.append("  idleSessionCycleMinutes: ").append(idleSessionCycleMinutes).append(System.lineSeparator());
    builder.append("  maxLongPollQueueSize: ").append(maxLongPollQueueSize).append(System.lineSeparator());

    return builder.append("}").toString();
  }

  private String fromRoutes (String[][] arrayOfArray) {

    StringBuilder builder = new StringBuilder("[");
    boolean first = true;

    for (String[] array : arrayOfArray) {
      if (!first) {
        builder.append(",");
      }

      builder.append("/").append(String.join("/", array));
      first = false;
    }

    return builder.append("]").toString();
  }
}
