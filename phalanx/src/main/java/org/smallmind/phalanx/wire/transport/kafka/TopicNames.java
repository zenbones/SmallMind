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
package org.smallmind.phalanx.wire.transport.kafka;

/**
 * Derives Kafka topic names for the four wire-protocol conversation patterns.
 *
 * <ul>
 *   <li><b>shout</b> — broadcasts a request to every instance of a service group</li>
 *   <li><b>talk</b>  — load-balances delivery within a service group (one consumer per message)</li>
 *   <li><b>whisper</b> — targets a specific service instance directly</li>
 *   <li><b>response</b> — routes results back to the originating caller</li>
 * </ul>
 * <p>
 * All generated names share a caller-supplied prefix to scope them within the broker namespace.
 */
public class TopicNames {

  private final String prefix;

  /**
   * Constructs a {@code TopicNames} instance that prefixes every generated name with
   * {@code prefix}.
   *
   * @param prefix shared namespace prefix applied to all generated topic names (e.g. {@code "wire"})
   */
  public TopicNames (String prefix) {

    this.prefix = prefix;
  }

  /**
   * Returns the shout topic name for the given service group.  Messages published here are
   * consumed by every running instance of the group.
   *
   * @param serviceGroup logical service group identifier
   * @return topic name in the form {@code <prefix>-shout-<serviceGroup>}
   */
  public String getShoutTopicName (String serviceGroup) {

    return prefix + "-shout-" + serviceGroup;
  }

  /**
   * Returns the talk topic name for the given service group.  Messages published here are
   * load-balanced across group instances so that exactly one instance processes each message.
   *
   * @param serviceGroup logical service group identifier
   * @return topic name in the form {@code <prefix>-talk-<serviceGroup>}
   */
  public String getTalkTopicName (String serviceGroup) {

    return prefix + "-talk-" + serviceGroup;
  }

  /**
   * Returns the whisper topic name that uniquely identifies a single service instance.
   * Only that instance subscribes to this topic.
   *
   * @param serviceGroup logical service group identifier
   * @param instanceId   unique identifier of the target service instance
   * @return topic name in the form {@code <prefix>-whisper-<serviceGroup>-<instanceId>}
   */
  public String getWhisperTopicName (String serviceGroup, String instanceId) {

    return prefix + "-whisper-" + serviceGroup + "-" + instanceId;
  }

  /**
   * Returns the response topic name for the given caller.  The response transport publishes
   * {@link org.smallmind.phalanx.wire.signal.ResultSignal}s here, and the corresponding request
   * transport consumes from it.
   *
   * @param instanceId unique identifier of the calling client (its caller ID)
   * @return topic name in the form {@code <prefix>-response-<instanceId>}
   */
  public String getResponseTopicName (String instanceId) {

    return prefix + "-response-" + instanceId;
  }
}
