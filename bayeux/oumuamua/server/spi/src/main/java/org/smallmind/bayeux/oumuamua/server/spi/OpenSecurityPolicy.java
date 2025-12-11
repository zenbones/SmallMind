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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.SecurityRejection;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Security policy that permits all operations.
 */
public class OpenSecurityPolicy<V extends Value<V>> implements SecurityPolicy<V> {

  /**
   * Always permits handshake requests.
   *
   * @param session the requesting session
   * @param message handshake message
   * @return {@code null} to indicate no rejection
   */
  @Override
  public SecurityRejection canHandshake (Session<V> session, Message<V> message) {

    return null;
  }

  /**
   * Always permits channel creation.
   *
   * @param session the requesting session
   * @param path    path to create
   * @param message create message
   * @return {@code null} to indicate no rejection
   */
  @Override
  public SecurityRejection canCreate (Session<V> session, String path, Message<V> message) {

    return null;
  }

  /**
   * Always permits channel subscription.
   *
   * @param session the requesting session
   * @param channel target channel
   * @param message subscribe message
   * @return {@code null} to indicate no rejection
   */
  @Override
  public SecurityRejection canSubscribe (Session<V> session, Channel<V> channel, Message<V> message) {

    return null;
  }

  /**
   * Always permits publishing to any channel.
   *
   * @param session the requesting session
   * @param channel target channel
   * @param message publish message
   * @return {@code null} to indicate no rejection
   */
  @Override
  public SecurityRejection canPublish (Session<V> session, Channel<V> channel, Message<V> message) {

    return null;
  }
}
