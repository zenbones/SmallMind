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

import com.rabbitmq.client.ConfirmListener;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.testng.annotations.Test;

/**
 * Re-runs the wire transport contract over RabbitMQ with two features the base RabbitMQ test leaves
 * off: a quorum talk queue ({@link QuorumQueueContractor}) and a non-null
 * {@link PublisherConfirmationHandler}, exercising the publisher-confirm path in the message routers.
 * Inherits the broker/connector setup and the four contract methods from
 * {@link RabbitMQTransportIntegrationTest}.
 */
@Test(groups = "integration")
public class RabbitMQConfirmationIntegrationTest extends RabbitMQTransportIntegrationTest {

  private static final PublisherConfirmationHandler CONFIRMATION_HANDLER = new PublisherConfirmationHandler() {

    @Override
    public ConfirmListener generateConfirmListener () {

      return new ConfirmListener() {

        @Override
        public void handleAck (long deliveryTag, boolean multiple) {

        }

        @Override
        public void handleNack (long deliveryTag, boolean multiple) {

        }
      };
    }
  };

  @Override
  protected ResponseTransport createResponseTransport ()
    throws Exception {

    return new RabbitMQResponseTransport(connector, new QuorumQueueContractor(1), new ClassicQueueContractor(), new NameConfiguration(), InvocationWorker.class, signalCodec, "default", 1, 1, 60, false, CONFIRMATION_HANDLER);
  }

  @Override
  protected RequestTransport createRequestTransport ()
    throws Exception {

    return new RabbitMQRequestTransport(connector, new ClassicQueueContractor(), new NameConfiguration(), signalCodec, 1, 1, 30L, 60, false, CONFIRMATION_HANDLER);
  }
}
