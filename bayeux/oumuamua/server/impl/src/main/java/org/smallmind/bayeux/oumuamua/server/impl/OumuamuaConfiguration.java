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
import org.smallmind.bayeux.oumuamua.server.impl.json.ClassNameArrayXmlAdapter;
import org.smallmind.bayeux.oumuamua.server.impl.json.ClassNameXmlAdapter;
import org.smallmind.bayeux.oumuamua.server.impl.json.DoubleStringArrayXmlAdapter;
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
  @View(adapter = ClassNameArrayXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Protocol<V>[] protocols;
  @View(adapter = ClassNameArrayXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private BayeuxService<V>[] services;
  @View(adapter = ClassNameArrayXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Server.Listener<V>[] listeners;
  @View(adapter = DoubleStringArrayXmlAdapter.class, name = "reflectingPaths", idioms = @Idiom(visibility = OUT))
  // send the request back to the sender as part of the success/failure response
  private String[][] parsedReflectingPaths;
  @View(adapter = DoubleStringArrayXmlAdapter.class, name = "streamingPaths", idioms = @Idiom(visibility = OUT))
  // ignore the ack extension (or other forced long polling), *if* the protocol does not require long polling
  private String[][] parsedStreamingPaths;
  @View(adapter = LevelEnumXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Level messageLogLevel = Level.TRACE;
  @View(adapter = LevelEnumXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Level idleCleanupLogLevel = Level.DEBUG;
  @View(adapter = LevelEnumXmlAdapter.class, idioms = @Idiom(visibility = OUT))
  private Level overflowLogLevel = Level.DEBUG;
  @View(idioms = @Idiom(visibility = OUT))
  private boolean allowsImplicitConnection;
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

  public BayeuxService<V>[] getServices () {

    return services;
  }

  public void setServices (BayeuxService<V>[] services) {

    this.services = services;
  }

  public Server.Listener<V>[] getListeners () {

    return listeners;
  }

  public void setListeners (Server.Listener<V>[] listeners) {

    this.listeners = listeners;
  }

  public Level getMessageLogLevel () {

    return messageLogLevel;
  }

  public void setMessageLogLevel (Level messageLogLevel) {

    this.messageLogLevel = messageLogLevel;
  }

  public Level getIdleCleanupLogLevel () {

    return idleCleanupLogLevel;
  }

  public void setIdleCleanupLogLevel (Level idleCleanupLogLevel) {

    this.idleCleanupLogLevel = idleCleanupLogLevel;
  }

  public Level getOverflowLogLevel () {

    return overflowLogLevel;
  }

  public void setOverflowLogLevel (Level overflowLogLevel) {

    this.overflowLogLevel = overflowLogLevel;
  }

  public boolean isAllowsImplicitConnection () {

    return allowsImplicitConnection;
  }

  public void setAllowsImplicitConnection (boolean allowsImplicitConnection) {

    this.allowsImplicitConnection = allowsImplicitConnection;
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

  public String[][] getParsedReflectingPaths () {

    return parsedReflectingPaths;
  }

  protected void setParsedReflectingPaths (String[][] parsedReflectingPaths) {

    this.parsedReflectingPaths = parsedReflectingPaths;
  }

  public void setReflectingPaths (String[] paths) {

    LinkedList<String[]> reflectingPathList = decomposePaths(paths);

    parsedReflectingPaths = new String[reflectingPathList.size()][];
    reflectingPathList.toArray(parsedReflectingPaths);
  }

  public boolean isReflecting (Route route) {

    return matchesPaths(parsedReflectingPaths, route);
  }

  public String[][] getParsedStreamingPaths () {

    return parsedStreamingPaths;
  }

  protected void setParsedStreamingPaths (String[][] parsedStreamingPaths) {

    this.parsedStreamingPaths = parsedStreamingPaths;
  }

  public void setStreamingPaths (String[] paths) {

    LinkedList<String[]> streamingPathList = decomposePaths(paths);

    parsedStreamingPaths = new String[streamingPathList.size()][];
    streamingPathList.toArray(parsedStreamingPaths);
  }

  public boolean isStreaming (Route route) {

    return matchesPaths(parsedStreamingPaths, route);
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
      for (String[] reflectingPath : paths) {
        if (route.matches(reflectingPath)) {

          return true;
        }
      }
    }

    return false;
  }
}
