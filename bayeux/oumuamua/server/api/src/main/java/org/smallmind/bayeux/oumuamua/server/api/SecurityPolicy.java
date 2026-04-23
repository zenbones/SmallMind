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
package org.smallmind.bayeux.oumuamua.server.api;

import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Access-control extension point consulted at each security-sensitive step of the Bayeux lifecycle;
 * all default implementations allow every operation.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface SecurityPolicy<V extends Value<V>> {

  /**
   * Decides whether the given session may complete a handshake.
   *
   * @param session session attempting to handshake
   * @param message the {@code /meta/handshake} message
   * @return {@link SecurityRejection#noReason()} to allow, or a rejection describing the denial
   */
  default SecurityRejection canHandshake (Session<V> session, Message<V> message) {

    return SecurityRejection.noReason();
  }

  /**
   * Decides whether the given session may cause a new channel to be created at the specified path.
   *
   * @param session session requesting channel creation
   * @param path    channel path that would be created
   * @param message the Bayeux message that triggered the creation attempt
   * @return {@link SecurityRejection#noReason()} to allow, or a rejection describing the denial
   */
  default SecurityRejection canCreate (Session<V> session, String path, Message<V> message) {

    return SecurityRejection.noReason();
  }

  /**
   * Decides whether the given session may subscribe to the specified channel.
   *
   * @param session session requesting the subscription
   * @param channel channel being subscribed to
   * @param message the {@code /meta/subscribe} message
   * @return {@link SecurityRejection#noReason()} to allow, or a rejection describing the denial
   */
  default SecurityRejection canSubscribe (Session<V> session, Channel<V> channel, Message<V> message) {

    return SecurityRejection.noReason();
  }

  /**
   * Decides whether the given session may publish to the specified channel.
   *
   * @param session session attempting to publish
   * @param channel channel being published to
   * @param message the publish message
   * @return {@link SecurityRejection#noReason()} to allow, or a rejection describing the denial
   */
  default SecurityRejection canPublish (Session<V> session, Channel<V> channel, Message<V> message) {

    return SecurityRejection.noReason();
  }
}
