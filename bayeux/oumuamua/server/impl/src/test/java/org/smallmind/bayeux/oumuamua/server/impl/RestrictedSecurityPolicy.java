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

import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.SecurityRejection;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.OpenSecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;

/**
 * Test-only {@link OpenSecurityPolicy} used by {@code MetaChannelErrorIntegrationTest}
 * to drive {@code SecurityPolicy} denial paths over the wire. The policy rejects each
 * of the four operations under a separate, observable condition:
 *
 * <ul>
 *   <li>{@link #canHandshake} — request carries {@code ext.deny=true}</li>
 *   <li>{@link #canCreate} — channel path begins with {@code /create-forbidden/}</li>
 *   <li>{@link #canSubscribe} — channel path begins with {@code /forbidden/}</li>
 *   <li>{@link #canPublish} — channel path begins with {@code /forbidden/}</li>
 * </ul>
 *
 * <p>Everything else inherits the permissive {@link OpenSecurityPolicy} defaults.</p>
 */
public class RestrictedSecurityPolicy extends OpenSecurityPolicy<OrthodoxValue> {

  private static final String FORBIDDEN_PREFIX = "/forbidden/";
  private static final String CREATE_FORBIDDEN_PREFIX = "/create-forbidden/";
  private static final String HANDSHAKE_REASON = "Handshake denied by RestrictedSecurityPolicy";
  private static final String CREATE_REASON = "Channel creation denied by RestrictedSecurityPolicy";
  private static final String SUBSCRIBE_REASON = "Subscription denied by RestrictedSecurityPolicy";
  private static final String PUBLISH_REASON = "Publish denied by RestrictedSecurityPolicy";

  /**
   * Denies handshake when the request carries {@code ext.deny=true}; this gives an
   * integration test an explicit knob to trigger the denial path without affecting
   * unrelated tests against the same server.
   *
   * @param session the session attempting to handshake
   * @param message the {@code /meta/handshake} message
   * @return a {@link SecurityRejection} with a reason when the deny flag is present, {@code null} otherwise
   */
  @Override
  public SecurityRejection canHandshake (Session<OrthodoxValue> session, Message<OrthodoxValue> message) {

    ObjectValue<OrthodoxValue> ext;
    Value<OrthodoxValue> denyValue;

    if ((message != null) && ((ext = message.getExt()) != null) && ((denyValue = ext.get("deny")) != null) && ValueType.BOOLEAN.equals(denyValue.getType()) && ((BooleanValue<OrthodoxValue>)denyValue).asBoolean()) {

      return SecurityRejection.reason(HANDSHAKE_REASON);
    }

    return null;
  }

  /**
   * Denies channel creation for any path starting with {@code /create-forbidden/}. This
   * check fires on first publish or subscribe to a previously-unseen channel; existing
   * channels never re-run it.
   *
   * @param session the session that triggered the creation
   * @param path    the path of the channel the server is about to create
   * @param message the originating request message
   * @return a {@link SecurityRejection} with a reason when the path is reserved, {@code null} otherwise
   */
  @Override
  public SecurityRejection canCreate (Session<OrthodoxValue> session, String path, Message<OrthodoxValue> message) {

    if ((path != null) && path.startsWith(CREATE_FORBIDDEN_PREFIX)) {

      return SecurityRejection.reason(CREATE_REASON);
    }

    return null;
  }

  /**
   * Denies subscriptions to any channel whose path starts with {@code /forbidden/}.
   *
   * @param session the session requesting the subscription
   * @param channel the channel being subscribed to
   * @param message the {@code /meta/subscribe} message
   * @return a {@link SecurityRejection} with a reason when the channel is forbidden, {@code null} otherwise
   */
  @Override
  public SecurityRejection canSubscribe (Session<OrthodoxValue> session, Channel<OrthodoxValue> channel, Message<OrthodoxValue> message) {

    if ((channel != null) && (channel.getRoute() != null) && channel.getRoute().getPath().startsWith(FORBIDDEN_PREFIX)) {

      return SecurityRejection.reason(SUBSCRIBE_REASON);
    }

    return null;
  }

  /**
   * Denies publishes to any channel whose path starts with {@code /forbidden/}.
   *
   * @param session the session attempting the publish
   * @param channel the destination channel
   * @param message the publish message
   * @return a {@link SecurityRejection} with a reason when the channel is forbidden, {@code null} otherwise
   */
  @Override
  public SecurityRejection canPublish (Session<OrthodoxValue> session, Channel<OrthodoxValue> channel, Message<OrthodoxValue> message) {

    if ((channel != null) && (channel.getRoute() != null) && channel.getRoute().getPath().startsWith(FORBIDDEN_PREFIX)) {

      return SecurityRejection.reason(PUBLISH_REASON);
    }

    return null;
  }
}
