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
package org.smallmind.kafka.utility;

/**
 * Selects the Kafka consumer group protocol forwarded to
 * {@link org.apache.kafka.clients.consumer.ConsumerConfig#GROUP_PROTOCOL_CONFIG}.
 * The choice also determines which additional consumer properties
 * {@link KafkaConnector#createConsumer} sets: {@link #CLASSIC} enables
 * client-side heartbeat and session-timeout settings; {@link #GROUP} omits
 * them because the broker manages those values under the new protocol.
 */
public enum KafkaConsumerType {

  /**
   * Classic client-side rebalance protocol ({@code group.protocol=classic}).
   * {@link KafkaConnector#createConsumer} additionally sets
   * {@code heartbeat.interval.ms=3000} and {@code session.timeout.ms=45000}.
   */
  CLASSIC("classic"),

  /**
   * KIP-848 new consumer group protocol ({@code group.protocol=group}).
   * {@code heartbeat.interval.ms} and {@code session.timeout.ms} are not set
   * client-side; the broker manages them under this protocol.
   */
  GROUP("group");

  private final String code;

  KafkaConsumerType (String code) {

    this.code = code;
  }

  /**
   * Returns the string value written to {@code group.protocol} in consumer configuration.
   *
   * @return the Kafka protocol identifier for this type
   */
  public String getCode () {

    return code;
  }
}
