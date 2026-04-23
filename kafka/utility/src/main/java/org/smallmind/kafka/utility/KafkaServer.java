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
 * Immutable-by-convention value object representing a single Kafka broker address as a host/port pair.
 * Instances are naturally ordered first by host name, then by port number.
 */
public class KafkaServer implements Comparable<KafkaServer> {

  private String host;
  private int port = 9094;

  /**
   * Creates an empty broker descriptor whose port defaults to {@code 9094}.
   * Intended for use by bean-wiring frameworks that set properties after construction.
   */
  public KafkaServer () {

  }

  /**
   * Creates a fully specified broker descriptor.
   *
   * @param host broker hostname or IP address
   * @param port broker listener port number
   */
  public KafkaServer (String host, int port) {

    this.host = host;
    this.port = port;
  }

  /**
   * Returns the broker hostname or IP address.
   *
   * @return host string, or {@code null} if not yet set
   */
  public String getHost () {

    return host;
  }

  /**
   * Sets the broker hostname or IP address.
   *
   * @param host broker hostname or IP address
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Returns the broker listener port number.
   *
   * @return port number; defaults to {@code 9094} when not explicitly assigned
   */
  public int getPort () {

    return port;
  }

  /**
   * Sets the broker listener port number.
   *
   * @param port broker listener port number
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Compares this broker to another, ordering first by host name lexicographically and then
   * by port number numerically when hosts are equal.
   *
   * @param server the broker to compare against; must not be {@code null}
   * @return a negative integer, zero, or a positive integer as this broker is less than,
   * equal to, or greater than {@code server}
   */
  @Override
  public int compareTo (KafkaServer server) {

    int comparison;

    return ((comparison = host.compareTo(server.host)) == 0) ? port - server.getPort() : comparison;
  }
}
