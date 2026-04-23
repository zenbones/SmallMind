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
 * {@link Voice} implementation that sends a standard request to any available member of a
 * named service group using the {@link VocalMode#TALK} delivery mode.
 * Supports both one-way ({@link OneWayConversation}) and two-way ({@link TwoWayConversation})
 * conversation semantics depending on the supplied {@link Conversation}.
 */
public class Talking implements Voice<String, Void> {

  private final Conversation<?> conversation;
  private final String serviceGroup;

  /**
   * Constructs a {@code Talking} voice with the specified conversation and target service group.
   *
   * @param conversation the conversation definition governing timeout and response expectations
   * @param serviceGroup the name of the service group to address
   */
  public Talking (Conversation<?> conversation, String serviceGroup) {

    this.conversation = conversation;
    this.serviceGroup = serviceGroup;
  }

  /**
   * Returns {@link VocalMode#TALK}, identifying this voice as a standard group-addressed request.
   *
   * @return {@link VocalMode#TALK}
   */
  @Override
  public VocalMode getMode () {

    return VocalMode.TALK;
  }

  /**
   * Returns the conversation definition that controls timeout and response handling for this request.
   *
   * @return the associated {@link Conversation}
   */
  @Override
  public Conversation getConversation () {

    return conversation;
  }

  /**
   * Returns the name of the service group that receives this request.
   *
   * @return the target service group name
   */
  @Override
  public String getServiceGroup () {

    return serviceGroup;
  }

  /**
   * Returns {@code null} because talk messages are routed to any group member, not a specific instance.
   *
   * @return {@code null} always
   */
  @Override
  public Void getInstanceId () {

    return null;
  }
}
