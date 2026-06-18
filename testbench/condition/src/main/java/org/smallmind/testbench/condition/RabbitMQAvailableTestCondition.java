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
package org.smallmind.testbench.condition;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * A {@link TestCondition} satisfied once a RabbitMQ broker accepts an authenticated AMQP connection
 * at a given address, used to wait for a freshly started broker to finish coming up before a test
 * publishes or consumes. Progress is echoed to standard output as a {@code "Waiting for RabbitMQ..."}
 * line followed by an incrementing attempt counter.
 */
public class RabbitMQAvailableTestCondition implements TestCondition {

  private final ConnectionFactory connectionFactory = new ConnectionFactory();
  private final Address address;
  private int count = 0;

  /**
   * Creates a condition that waits for the broker at {@code address} to accept the given credentials.
   *
   * @param userName the AMQP username to authenticate with
   * @param password the AMQP password to authenticate with
   * @param address the host and port of the broker to probe
   */
  public RabbitMQAvailableTestCondition (String userName, String password, Address address) {

    this.address = address;

    connectionFactory.setUsername(userName);
    connectionFactory.setPassword(password);
  }

  /**
   * Attempts to open and immediately close an AMQP connection to the configured broker.
   *
   * @return {@code null} when the connection succeeds (the broker is available), or a
   * {@link MessageTestConditionFailure} when the connection attempt fails, having printed an
   * incrementing attempt counter to standard output
   * @throws Exception is declared by the contract but not thrown directly; connection failures are
   * caught and reported as a {@link TestConditionFailure} instead
   */
  @Override
  public TestConditionFailure test ()
    throws Exception {

    if (count == 0) {
      System.out.print("Waiting for RabbitMQ...");
    }

    try (Connection connection = connectionFactory.newConnection(new Address[] {address})) {
      System.out.println();

      return null;
    } catch (Exception exception) {

      System.out.print((++count) + "...");
      return new MessageTestConditionFailure("Could not start RabbitMQ");
    }
  }
}
