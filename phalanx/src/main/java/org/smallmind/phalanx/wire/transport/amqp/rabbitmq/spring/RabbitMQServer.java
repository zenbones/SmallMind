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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq.spring;

/**
 * Holds the host and port for a single RabbitMQ broker endpoint.
 */
public class RabbitMQServer {

  private String host;
  private int port = 5672;

  /**
   * Creates a server entry using the default AMQP port 5672.
   *
   * @param host broker hostname or IP address.
   */
  public RabbitMQServer (String host) {

    this.host = host;
  }

  /**
   * Creates a server entry with an explicit port.
   *
   * @param host broker hostname or IP address.
   * @param port broker AMQP port.
   */
  public RabbitMQServer (String host, int port) {

    this(host);

    this.port = port;
  }

  /**
   * Returns the broker hostname or IP address.
   *
   * @return broker host.
   */
  public String getHost () {

    return host;
  }

  /**
   * Sets the broker hostname or IP address.
   *
   * @param host broker host.
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Returns the broker AMQP port.
   *
   * @return broker port.
   */
  public int getPort () {

    return port;
  }

  /**
   * Sets the broker AMQP port.
   *
   * @param port broker port.
   */
  public void setPort (int port) {

    this.port = port;
  }
}
