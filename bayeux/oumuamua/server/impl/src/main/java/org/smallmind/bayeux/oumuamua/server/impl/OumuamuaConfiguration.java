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

/**
 * Aggregates configurable components and tunables for the Oumuamua server.
 *
 * @param <V> value representation
 */
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

  /**
   * @return configured backbone implementation
   */
  public Backbone<V> getBackbone () {

    return backbone;
  }

  /**
   * Sets the backbone used to distribute messages across nodes.
   *
   * @param backbone backbone implementation
   */
  public void setBackbone (Backbone<V> backbone) {

    this.backbone = backbone;
  }

  /**
   * @return codec used to create and parse JSON messages
   */
  public Codec<V> getCodec () {

    return codec;
  }

  /**
   * Sets the codec implementation used for message handling.
   *
   * @param codec codec implementation
   */
  public void setCodec (Codec<V> codec) {

    this.codec = codec;
  }

  /**
   * @return executor service used for asynchronous work
   */
  public ExecutorService getExecutorService () {

    return executorService;
  }

  /**
   * Assigns the executor service used for background processing.
   *
   * @param executorService executor service
   */
  public void setExecutorService (ExecutorService executorService) {

    this.executorService = executorService;
  }

  /**
   * @return security policy applied to Bayeux operations
   */
  public SecurityPolicy<V> getSecurityPolicy () {

    return securityPolicy;
  }

  /**
   * Sets the security policy that authorizes operations.
   *
   * @param securityPolicy security policy implementation
   */
  public void setSecurityPolicy (SecurityPolicy<V> securityPolicy) {

    this.securityPolicy = securityPolicy;
  }

  /**
   * @return supported protocols
   */
  public Protocol<V>[] getProtocols () {

    return protocols;
  }

  /**
   * Defines the set of supported Bayeux protocols.
   *
   * @param protocols protocol implementations
   */
  public void setProtocols (Protocol<V>[] protocols) {

    this.protocols = protocols;
  }

  /**
   * @return configured Bayeux services
   */
  public BayeuxService<V>[] getServices () {

    return services;
  }

  /**
   * Sets the collection of server-side services.
   *
   * @param services Bayeux services
   */
  public void setServices (BayeuxService<V>[] services) {

    this.services = services;
  }

  /**
   * @return server listeners
   */
  public Server.Listener<V>[] getListeners () {

    return listeners;
  }

  /**
   * Assigns listeners that observe server lifecycle events.
   *
   * @param listeners server listeners
   */
  public void setListeners (Server.Listener<V>[] listeners) {

    this.listeners = listeners;
  }

  /**
   * @return log level used for message handling diagnostics
   */
  public Level getMessageLogLevel () {

    return messageLogLevel;
  }

  /**
   * Sets the log level used when processing messages.
   *
   * @param messageLogLevel log level
   */
  public void setMessageLogLevel (Level messageLogLevel) {

    this.messageLogLevel = messageLogLevel;
  }

  /**
   * @return log level for idle cleanup operations
   */
  public Level getIdleCleanupLogLevel () {

    return idleCleanupLogLevel;
  }

  /**
   * Sets the log level used when pruning idle resources.
   *
   * @param idleCleanupLogLevel log level
   */
  public void setIdleCleanupLogLevel (Level idleCleanupLogLevel) {

    this.idleCleanupLogLevel = idleCleanupLogLevel;
  }

  /**
   * @return log level used when queues overflow
   */
  public Level getOverflowLogLevel () {

    return overflowLogLevel;
  }

  /**
   * Configures the log level emitted when long-poll queues overflow.
   *
   * @param overflowLogLevel log level
   */
  public void setOverflowLogLevel (Level overflowLogLevel) {

    this.overflowLogLevel = overflowLogLevel;
  }

  /**
   * @return whether implicit connection is allowed before explicit connect calls
   */
  public boolean isAllowsImplicitConnection () {

    return allowsImplicitConnection;
  }

  /**
   * Enables or disables implicit connection handling.
   *
   * @param allowsImplicitConnection {@code true} to allow implicit connects
   */
  public void setAllowsImplicitConnection (boolean allowsImplicitConnection) {

    this.allowsImplicitConnection = allowsImplicitConnection;
  }

  /**
   * @return minutes a channel may remain idle before removal
   */
  public long getChannelTimeToLiveMinutes () {

    return channelTimeToLiveMinutes;
  }

  /**
   * Sets the idle time-to-live for ephemeral channels.
   *
   * @param channelTimeToLiveMinutes ttl in minutes
   */
  public void setChannelTimeToLiveMinutes (long channelTimeToLiveMinutes) {

    this.channelTimeToLiveMinutes = channelTimeToLiveMinutes;
  }

  /**
   * @return interval advised to clients between connect calls
   */
  public int getSessionConnectIntervalSeconds () {

    return sessionConnectIntervalSeconds;
  }

  /**
   * Configures the session connect interval advice.
   *
   * @param sessionConnectIntervalSeconds interval in seconds
   */
  public void setSessionConnectIntervalSeconds (int sessionConnectIntervalSeconds) {

    this.sessionConnectIntervalSeconds = sessionConnectIntervalSeconds;
  }

  /**
   * @return maximum idle time before a session is terminated
   */
  public int getSessionMaxIdleTimeoutSeconds () {

    return sessionMaxIdleTimeoutSeconds;
  }

  /**
   * Sets the maximum allowed session idle time.
   *
   * @param sessionMaxIdleTimeoutSeconds idle timeout in seconds
   */
  public void setSessionMaxIdleTimeoutSeconds (int sessionMaxIdleTimeoutSeconds) {

    this.sessionMaxIdleTimeoutSeconds = sessionMaxIdleTimeoutSeconds;
  }

  /**
   * @return minutes between idle channel scans
   */
  public int getIdleChannelCycleMinutes () {

    return idleChannelCycleMinutes;
  }

  /**
   * Sets the cadence for cleaning idle channels.
   *
   * @param idleChannelCycleMinutes minutes between scans
   */
  public void setIdleChannelCycleMinutes (int idleChannelCycleMinutes) {

    this.idleChannelCycleMinutes = idleChannelCycleMinutes;
  }

  /**
   * @return minutes between idle session inspections
   */
  public int getIdleSessionCycleMinutes () {

    return idleSessionCycleMinutes;
  }

  /**
   * Sets the cadence for inspecting idle sessions.
   *
   * @param idleSessionCycleMinutes minutes between inspections
   */
  public void setIdleSessionCycleMinutes (int idleSessionCycleMinutes) {

    this.idleSessionCycleMinutes = idleSessionCycleMinutes;
  }

  /**
   * @return maximum number of queued responses for long-polling sessions
   */
  public int getMaxLongPollQueueSize () {

    return maxLongPollQueueSize;
  }

  /**
   * Sets the maximum queue size for long-poll connections before overflow handling.
   *
   * @param maxLongPollQueueSize maximum queue length
   */
  public void setMaxLongPollQueueSize (int maxLongPollQueueSize) {

    this.maxLongPollQueueSize = maxLongPollQueueSize;
  }

  /**
   * @return parsed reflecting path patterns
   */
  public String[][] getParsedReflectingPaths () {

    return parsedReflectingPaths;
  }

  /**
   * Allows subclasses or frameworks to set parsed reflecting paths.
   *
   * @param parsedReflectingPaths parsed path segments
   */
  protected void setParsedReflectingPaths (String[][] parsedReflectingPaths) {

    this.parsedReflectingPaths = parsedReflectingPaths;
  }

  /**
   * Configures channel paths that should reflect messages back to the publisher.
   *
   * @param paths raw channel path patterns
   */
  public void setReflectingPaths (String[] paths) {

    LinkedList<String[]> reflectingPathList = decomposePaths(paths);

    parsedReflectingPaths = new String[reflectingPathList.size()][];
    reflectingPathList.toArray(parsedReflectingPaths);
  }

  /**
   * Indicates whether the route should reflect messages.
   *
   * @param route route to check
   * @return {@code true} if reflection is enabled for the route
   */
  public boolean isReflecting (Route route) {

    return matchesPaths(parsedReflectingPaths, route);
  }

  /**
   * @return parsed streaming path patterns
   */
  public String[][] getParsedStreamingPaths () {

    return parsedStreamingPaths;
  }

  /**
   * Allows subclasses or frameworks to set parsed streaming paths.
   *
   * @param parsedStreamingPaths parsed path segments
   */
  protected void setParsedStreamingPaths (String[][] parsedStreamingPaths) {

    this.parsedStreamingPaths = parsedStreamingPaths;
  }

  /**
   * Configures paths that should always stream data.
   *
   * @param paths raw channel path patterns
   */
  public void setStreamingPaths (String[] paths) {

    LinkedList<String[]> streamingPathList = decomposePaths(paths);

    parsedStreamingPaths = new String[streamingPathList.size()][];
    streamingPathList.toArray(parsedStreamingPaths);
  }

  /**
   * Indicates whether the route should stream data.
   *
   * @param route route to check
   * @return {@code true} if streaming is enabled
   */
  public boolean isStreaming (Route route) {

    return matchesPaths(parsedStreamingPaths, route);
  }

  /**
   * Breaks the provided path strings into segment arrays.
   *
   * @param paths raw path strings
   * @return list of decomposed paths
   */
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

  /**
   * Tests whether the route matches any of the provided patterns.
   *
   * @param paths parsed path patterns
   * @param route route to evaluate
   * @return {@code true} if the route matches a pattern
   */
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
