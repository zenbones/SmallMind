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
package org.smallmind.phalanx.wire.transport.kafka;

/**
 * Utility for constructing standardized Kafka topic names used by the wire transport.
 */
public class TopicNames {

  // prefix = "wire"
  private final String prefix;

  /**
   * @param prefix prefix applied to all topic names to namespace them for a deployment
   */
  public TopicNames (String prefix) {

    this.prefix = prefix;
  }

  /**
   * @param serviceGroup the service group receiving the shout message
   * @return topic name for broadcasting shout requests
   */
  public String getShoutTopicName (String serviceGroup) {

    return prefix + "-shout-" + serviceGroup;
  }

  /**
   * @param serviceGroup the service group targeted by the talk message
   * @return topic name for distributing talk requests
   */
  public String getTalkTopicName (String serviceGroup) {

    return prefix + "-talk-" + serviceGroup;
  }

  /**
   * @param serviceGroup the service group addressed by the whisper message
   * @param instanceId   identifier of the specific instance to receive the whisper
   * @return topic name for direct whisper requests to the instance
   */
  public String getWhisperTopicName (String serviceGroup, String instanceId) {

    return prefix + "-whisper-" + serviceGroup + "-" + instanceId;
  }

  /**
   * @param instanceId caller or responder instance identifier
   * @return topic name for responses directed to the instance
   */
  public String getResponseTopicName (String instanceId) {

    return prefix + "-response-" + instanceId;
  }
}
