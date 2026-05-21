/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;

/**
 * Test-only listener that implements all three lifecycle subtypes
 * ({@link Server.SessionListener}, {@link Server.ChannelListener},
 * {@link Server.SubscriptionListener}) and records every callback into thread-safe lists,
 * so an integration test can assert which events fired and in what order against a live
 * server.
 */
public class RecordingServerListener implements Server.SessionListener<OrthodoxValue>, Server.ChannelListener<OrthodoxValue>, Server.SubscriptionListener<OrthodoxValue> {

  private final List<String> connectedSessions = new CopyOnWriteArrayList<>();
  private final List<String> disconnectedSessions = new CopyOnWriteArrayList<>();
  private final List<String> createdChannels = new CopyOnWriteArrayList<>();
  private final List<String> removedChannels = new CopyOnWriteArrayList<>();
  private final List<String> subscribedPairs = new CopyOnWriteArrayList<>();
  private final List<String> unsubscribedPairs = new CopyOnWriteArrayList<>();

  /**
   * Returns the ids of sessions seen in {@link #onConnected}, in observation order.
   */
  public List<String> connectedSessions () {

    return connectedSessions;
  }

  /**
   * Returns the ids of sessions seen in {@link #onDisconnected}, in observation order.
   */
  public List<String> disconnectedSessions () {

    return disconnectedSessions;
  }

  /**
   * Returns the paths of channels seen in {@link #onCreated}, in observation order.
   */
  public List<String> createdChannels () {

    return createdChannels;
  }

  /**
   * Returns the paths of channels seen in {@link #onRemoved}, in observation order.
   */
  public List<String> removedChannels () {

    return removedChannels;
  }

  /**
   * Returns {@code channelPath|sessionId} pairs seen in {@link #onSubscribed}, in
   * observation order.
   */
  public List<String> subscribedPairs () {

    return subscribedPairs;
  }

  /**
   * Returns {@code channelPath|sessionId} pairs seen in {@link #onUnsubscribed}, in
   * observation order.
   */
  public List<String> unsubscribedPairs () {

    return unsubscribedPairs;
  }

  @Override
  public void onConnected (Session<OrthodoxValue> session) {

    connectedSessions.add(session.getId());
  }

  @Override
  public void onDisconnected (Session<OrthodoxValue> session) {

    disconnectedSessions.add(session.getId());
  }

  @Override
  public void onCreated (Channel<OrthodoxValue> channel) {

    createdChannels.add(channel.getRoute().getPath());
  }

  @Override
  public void onRemoved (Channel<OrthodoxValue> channel) {

    removedChannels.add(channel.getRoute().getPath());
  }

  @Override
  public void onSubscribed (Channel<OrthodoxValue> channel, Session<OrthodoxValue> session) {

    subscribedPairs.add(channel.getRoute().getPath() + "|" + session.getId());
  }

  @Override
  public void onUnsubscribed (Channel<OrthodoxValue> channel, Session<OrthodoxValue> session) {

    unsubscribedPairs.add(channel.getRoute().getPath() + "|" + session.getId());
  }
}
