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
 * Open {@link SecurityPolicy} that unconditionally allows every Bayeux operation;
 * intended for development or deployments where access control is handled externally.
 *
 * @param <V> concrete {@link Value} type carried in Bayeux messages
 */
public class OpenSecurityPolicy<V extends Value<V>> implements SecurityPolicy<V> {

  /**
   * Approves all handshake requests without restriction.
   *
   * @param session the session attempting to handshake
   * @param message the {@code /meta/handshake} message
   * @return {@code null}, indicating no rejection
   */
  @Override
  public SecurityRejection canHandshake (Session<V> session, Message<V> message) {

    return null;
  }

  /**
   * Approves all channel creation attempts without restriction.
   *
   * @param session the session requesting channel creation
   * @param path    the path of the channel to be created
   * @param message the message that triggered the creation
   * @return {@code null}, indicating no rejection
   */
  @Override
  public SecurityRejection canCreate (Session<V> session, String path, Message<V> message) {

    return null;
  }

  /**
   * Approves all subscription requests without restriction.
   *
   * @param session the session requesting the subscription
   * @param channel the channel being subscribed to
   * @param message the {@code /meta/subscribe} message
   * @return {@code null}, indicating no rejection
   */
  @Override
  public SecurityRejection canSubscribe (Session<V> session, Channel<V> channel, Message<V> message) {

    return null;
  }

  /**
   * Approves all publish attempts without restriction.
   *
   * @param session the session publishing the message
   * @param channel the target channel
   * @param message the message being published
   * @return {@code null}, indicating no rejection
   */
  @Override
  public SecurityRejection canPublish (Session<V> session, Channel<V> channel, Message<V> message) {

    return null;
  }
}
