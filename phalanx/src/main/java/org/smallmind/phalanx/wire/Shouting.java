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
package org.smallmind.phalanx.wire;

/**
 * {@link Voice} implementation that broadcasts a fire-and-forget message to all listeners
 * in a named service group using the {@link VocalMode#SHOUT} delivery mode.
 */
public class Shouting implements Voice<String, Void> {

  private static final OneWayConversation ONE_WAY_CONVERSATION = new OneWayConversation();

  private final String serviceGroup;

  /**
   * Constructs a {@code Shouting} voice targeting the specified service group.
   *
   * @param serviceGroup the name of the service group to broadcast to
   */
  public Shouting (String serviceGroup) {

    this.serviceGroup = serviceGroup;
  }

  /**
   * Returns {@link VocalMode#SHOUT}, identifying this voice as a broadcast delivery.
   *
   * @return {@link VocalMode#SHOUT}
   */
  @Override
  public VocalMode getMode () {

    return VocalMode.SHOUT;
  }

  /**
   * Returns the shared one-way conversation used for all shout invocations.
   *
   * @return the {@link OneWayConversation} singleton for fire-and-forget messaging
   */
  public Conversation getConversation () {

    return ONE_WAY_CONVERSATION;
  }

  /**
   * Returns the name of the service group to which this shout is broadcast.
   *
   * @return the target service group name
   */
  @Override
  public String getServiceGroup () {

    return serviceGroup;
  }

  /**
   * Returns {@code null} because shouts are not directed at a specific instance.
   *
   * @return {@code null} always
   */
  @Override
  public Void getInstanceId () {

    return null;
  }
}
