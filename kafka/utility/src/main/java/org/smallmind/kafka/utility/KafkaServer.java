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
 * Simple value object describing a Kafka broker host/port pair.
 */
public class KafkaServer implements Comparable<KafkaServer> {

  private String host;
  private int port = 9094;

  /**
   * Creates an empty broker descriptor with the default port {@code 9094}.
   */
  public KafkaServer () {

  }

  /**
   * Creates a broker descriptor.
   *
   * @param host broker host name or address
   * @param port broker listener port
   */
  public KafkaServer (String host, int port) {

    this.host = host;
    this.port = port;
  }

  /**
   * Returns the broker host name or address.
   *
   * @return host value
   */
  public String getHost () {

    return host;
  }

  /**
   * Updates the broker host name or address.
   *
   * @param host host value
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Returns the configured broker port.
   *
   * @return port value
   */
  public int getPort () {

    return port;
  }

  /**
   * Updates the broker port.
   *
   * @param port port value
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Orders brokers lexicographically by host, then numerically by port.
   *
   * @param server the broker to compare against
   * @return negative if this broker sorts before {@code server}, zero if equal, positive otherwise
   */
  @Override
  public int compareTo (KafkaServer server) {

    int comparison;

    return ((comparison = host.compareTo(server.host)) == 0) ? port - server.getPort() : comparison;
  }
}
