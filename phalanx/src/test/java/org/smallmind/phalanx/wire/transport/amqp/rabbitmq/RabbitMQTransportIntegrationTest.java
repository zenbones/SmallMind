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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.transport.AbstractWireTransportContractTest;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.testbench.docker.DockerApplication;
import org.testng.annotations.Test;

/**
 * Runs the wire transport contract against a RabbitMQ broker started by the Docker test harness
 * ({@link DockerApplication#RABBITMQ}, default {@code guest}/{@code guest} on {@code localhost:5672}).
 */
@Test(groups = "integration")
public class RabbitMQTransportIntegrationTest extends AbstractWireTransportContractTest {

  protected final JsonSignalCodec signalCodec = new JsonSignalCodec();
  protected RabbitMQConnector connector;

  public RabbitMQTransportIntegrationTest () {

    super(DockerApplication.RABBITMQ);
  }

  @Override
  protected void prepareInfrastructure ()
    throws Exception {

    ConnectionFactory connectionFactory = new ConnectionFactory();

    connectionFactory.setUsername("guest");
    connectionFactory.setPassword("guest");
    connectionFactory.setVirtualHost("/");

    awaitBroker();

    connector = new RabbitMQConnector(connectionFactory, new Address("localhost", 5672));
  }

  private void awaitBroker ()
    throws InterruptedException {

    ConnectionFactory probeFactory = new ConnectionFactory();

    probeFactory.setUsername("guest");
    probeFactory.setPassword("guest");
    probeFactory.setVirtualHost("/");

    for (int attempt = 0; attempt < 30; attempt++) {
      try (Connection connection = probeFactory.newConnection(new Address[] {new Address("localhost", 5672)})) {

        return;
      } catch (Exception exception) {
        Thread.sleep(1000);
      }
    }
  }

  @Override
  protected ResponseTransport createResponseTransport ()
    throws Exception {

    return new RabbitMQResponseTransport(connector, new ClassicQueueContractor(), new ClassicQueueContractor(), new NameConfiguration(), InvocationWorker.class, signalCodec, "default", 1, 1, 60, false, null);
  }

  @Override
  protected RequestTransport createRequestTransport ()
    throws Exception {

    return new RabbitMQRequestTransport(connector, new ClassicQueueContractor(), new NameConfiguration(), signalCodec, 1, 1, 30L, 60, false, null);
  }
}
