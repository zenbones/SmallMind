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
 * {@link Voice} implementation that delivers a request to a specific service instance
 * within a named group using the {@link VocalMode#WHISPER} delivery mode.
 * Always uses a {@link TwoWayConversation} and supports an optional response timeout.
 */
public class Whispering implements Voice<String, String> {

  private final TwoWayConversation twoWayConversation;
  private final String serviceGroup;
  private final String instanceId;

  /**
   * Constructs a {@code Whispering} voice with no custom timeout (uses the transport default).
   *
   * @param serviceGroup the name of the service group containing the target instance
   * @param instanceId   the identifier of the specific service instance to address
   */
  public Whispering (String serviceGroup, String instanceId) {

    this(serviceGroup, instanceId, 0L);
  }

  /**
   * Constructs a {@code Whispering} voice with an explicit response timeout.
   *
   * @param serviceGroup the name of the service group containing the target instance
   * @param instanceId   the identifier of the specific service instance to address
   * @param timeout      maximum time in seconds to wait for a response; {@code 0} uses the transport default
   */
  public Whispering (String serviceGroup, String instanceId, Long timeout) {

    this.serviceGroup = serviceGroup;
    this.instanceId = instanceId;

    twoWayConversation = new TwoWayConversation(timeout);
  }

  /**
   * Returns {@link VocalMode#WHISPER}, identifying this voice as an instance-targeted request.
   *
   * @return {@link VocalMode#WHISPER}
   */
  @Override
  public VocalMode getMode () {

    return VocalMode.WHISPER;
  }

  /**
   * Returns the two-way conversation that governs timeout and response handling for this whisper.
   *
   * @return the {@link TwoWayConversation} associated with this invocation
   */
  public Conversation<?> getConversation () {

    return twoWayConversation;
  }

  /**
   * Returns the name of the service group that contains the target instance.
   *
   * @return the target service group name
   */
  @Override
  public String getServiceGroup () {

    return serviceGroup;
  }

  /**
   * Returns the identifier of the specific service instance this whisper is addressed to.
   *
   * @return the target instance id
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }
}
