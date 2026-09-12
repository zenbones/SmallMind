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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Covers the recovery behaviour that the transport contract tests never reach, because every defect
 * this machinery exists to prevent only appears when something has already gone wrong.
 *
 * <p>Each of these corresponds to a failure seen in production or found while building the transport:
 * a recovery chain that ended silently when a reconnect threw, a paused transport that silently
 * resumed on a channel bounce, a queue the broker refused taking down the queues that were fine, and a
 * binding refusal churning the connection underneath consumers that were working.
 */
@Test(groups = "integration")
public class RabbitMQRecoveryIntegrationTest extends AbstractGroundwaterTest {

  private static final String SERVICE_GROUP = "recovery";
  private static final long AWAIT_MILLISECONDS = 60000;

  private ConnectionFactory connectionFactory;
  private RabbitMQConnector connector;

  public RabbitMQRecoveryIntegrationTest () {

    super(DockerApplication.RABBITMQ);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();
    super.beforeClass();

    awaitBroker();

    connectionFactory = new ConnectionFactory();
    connectionFactory.setUsername("guest");
    connectionFactory.setPassword("guest");
    connectionFactory.setVirtualHost("/");
    connectionFactory.setRequestedHeartbeat(5);

    connector = new RabbitMQConnector(connectionFactory, new Address("localhost", 5672));
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    super.afterClass();
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

  /**
   * The client library's own recovery must be off wherever a connector is built, because two recovery
   * mechanisms racing on one broker-side drop leave an autorecovered connection alive behind the
   * manager's back, with its consumers duplicated on queues the manager has already rebuilt.
   */
  @Test
  public void testConnectorDisablesClientRecovery () {

    ConnectionFactory recoveringFactory = new ConnectionFactory();

    recoveringFactory.setAutomaticRecoveryEnabled(true);
    recoveringFactory.setTopologyRecoveryEnabled(true);

    new RabbitMQConnector(recoveringFactory, new Address("localhost", 5672));

    Assert.assertFalse(recoveringFactory.isAutomaticRecoveryEnabled(), "the connector left automatic recovery enabled");
    Assert.assertFalse(recoveringFactory.isTopologyRecoveryEnabled(), "the connector left topology recovery enabled");
  }

  /**
   * Losing the connection, rather than a single channel, must rebuild every registered router and leave
   * the transport carrying traffic again without anything restarting the process.
   */
  @Test
  public void testTransportRecoversFromConnectionLoss ()
    throws Exception {

    RabbitMQResponseTransport responseTransport = createResponseTransport(connector);

    try {
      Assert.assertTrue(awaitHealthy(responseTransport), "the transport never became healthy: " + responseTransport.getDiagnostic());

      killConnection(responseTransport);

      Assert.assertTrue(awaitHealthy(responseTransport), "the transport did not recover from connection loss: " + responseTransport.getDiagnostic());
      Assert.assertTrue(responseTransport.getDiagnostic().contains("consuming(true)"), "consumers were not reinstalled: " + responseTransport.getDiagnostic());
    } finally {
      responseTransport.close();
    }
  }

  /**
   * The most important regression in this class. A reconnect that throws must schedule the next
   * attempt rather than ending the chain - the original implementation re-declared from the channel's
   * own shutdown listener, so a failure to open a connection left no listener registered anywhere and
   * the transport consumed nothing until its pod was restarted, with a single line in the log.
   */
  @Test
  public void testRecoveryChainSurvivesFailedReconnects ()
    throws Exception {

    //  Zero to start with, so that the transport comes up normally - startup is meant to fail fast, and
    //  it is the RECONNECT that has to survive being refused.
    AtomicInteger refusalCount = new AtomicInteger(0);
    RabbitMQConnector refusingConnector = new RabbitMQConnector(connectionFactory, new Address("localhost", 5672)) {

      @Override
      public Connection getConnection ()
        throws IOException, TimeoutException {

        if (refusalCount.get() > 0) {
          refusalCount.decrementAndGet();

          throw new IOException("refused by the test");
        }

        return super.getConnection();
      }
    };

    RabbitMQResponseTransport responseTransport = createResponseTransport(refusingConnector);

    try {
      Assert.assertTrue(awaitHealthy(responseTransport), "the transport never became healthy: " + responseTransport.getDiagnostic());

      //  Refuse the next three reconnects, then relent. Recovery has to outlive all of them.
      refusalCount.set(3);
      killConnection(responseTransport);

      Assert.assertTrue(awaitHealthy(responseTransport), "recovery did not survive repeated reconnect failures: " + responseTransport.getDiagnostic());
      Assert.assertEquals(refusalCount.get(), 0, "the test never exhausted its refusals, so nothing was proven");
    } finally {
      responseTransport.close();
    }
  }

  /**
   * A transport an operator deliberately paused must stay paused across a rebuild. Reinstalling
   * consumers unconditionally meant any channel bounce silently resumed it.
   */
  @Test
  public void testPauseSurvivesRebuild ()
    throws Exception {

    RabbitMQResponseTransport responseTransport = createResponseTransport(connector);

    try {
      Assert.assertTrue(awaitHealthy(responseTransport), "the transport never became healthy: " + responseTransport.getDiagnostic());

      responseTransport.pause();
      Assert.assertEquals(responseTransport.getState(), TransportState.PAUSED, "the transport did not pause");

      killConnection(responseTransport);
      Assert.assertTrue(awaitDiagnostic(responseTransport, "connection(open)"), "the connection was never rebuilt: " + responseTransport.getDiagnostic());

      Assert.assertEquals(responseTransport.getState(), TransportState.PAUSED, "the rebuild resumed a paused transport");
      Assert.assertTrue(responseTransport.getDiagnostic().contains("consuming(false)"), "the rebuild reinstalled consumers on a paused transport: " + responseTransport.getDiagnostic());

      //  Resuming while a rebuild may still be in flight is what once reused a consumer tag, which the
      //  broker answers at connection level, and what a lock held across the call turned into a stall.
      responseTransport.play();
      Assert.assertTrue(awaitHealthy(responseTransport), "the transport did not resume consuming: " + responseTransport.getDiagnostic());
    } finally {
      responseTransport.close();
    }
  }

  /**
   * A queue the broker refuses must degrade the router rather than take down the queues that declared
   * cleanly. The refusal here is the one a development broker upgraded in place produces: a talk queue
   * that already exists as a classic queue cannot be re-declared as a quorum queue.
   */
  @Test
  public void testRefusedQueueDegradesRatherThanKills ()
    throws Exception {

    String serviceGroup = SERVICE_GROUP + "-degraded";
    String talkQueueName = "wire-talkQueue-" + serviceGroup;

    try (Connection connection = connectionFactory.newConnection(new Address[] {new Address("localhost", 5672)});
         Channel channel = connection.createChannel()) {
      channel.queueDelete(talkQueueName);
      channel.queueDeclare(talkQueueName, true, false, false, null);
    }

    RabbitMQResponseTransport responseTransport = createResponseTransport(connector, serviceGroup);

    try {
      Assert.assertTrue(awaitDiagnostic(responseTransport, "consuming(true)"), "the per-instance queues never began consuming: " + responseTransport.getDiagnostic());

      Assert.assertFalse(responseTransport.isHealthy(), "a transport with a refused queue reported itself healthy: " + responseTransport.getDiagnostic());
      Assert.assertTrue(responseTransport.getDiagnostic().contains("bound(false)"), "the refused queue was not reported as unbound: " + responseTransport.getDiagnostic());
    } finally {
      responseTransport.close();
      try (Connection connection = connectionFactory.newConnection(new Address[] {new Address("localhost", 5672)});
           Channel channel = connection.createChannel()) {
        channel.queueDelete(talkQueueName);
      }
    }
  }

  /**
   * A queue the broker refuses on its merits will be refused just as firmly on a new connection, so the
   * binding retry must not escalate into replacing the connection. Escalating deleted and re-created
   * the exclusive queues that were working, flapping their consumers for as long as the operator
   * problem lasted.
   */
  @Test
  public void testRefusedQueueDoesNotChurnTheConnection ()
    throws Exception {

    String serviceGroup = SERVICE_GROUP + "-churn";
    String talkQueueName = "wire-talkQueue-" + serviceGroup;

    try (Connection connection = connectionFactory.newConnection(new Address[] {new Address("localhost", 5672)});
         Channel channel = connection.createChannel()) {
      channel.queueDelete(talkQueueName);
      channel.queueDeclare(talkQueueName, true, false, false, null);
    }

    RabbitMQResponseTransport responseTransport = createResponseTransport(connector, serviceGroup);

    try {
      Assert.assertTrue(awaitDiagnostic(responseTransport, "bound(false)"), "the refused queue was never reported: " + responseTransport.getDiagnostic());

      String settledGeneration = generationOf(responseTransport);

      //  Long enough for several escalation windows to have passed at the configured backoff.
      Thread.sleep(10000);

      Assert.assertEquals(generationOf(responseTransport), settledGeneration, "the connection was replaced while a queue was merely being refused: " + responseTransport.getDiagnostic());
      Assert.assertTrue(responseTransport.getDiagnostic().contains("consuming(true)"), "the queues that did bind stopped consuming: " + responseTransport.getDiagnostic());
    } finally {
      responseTransport.close();
      try (Connection connection = connectionFactory.newConnection(new Address[] {new Address("localhost", 5672)});
           Channel channel = connection.createChannel()) {
        channel.queueDelete(talkQueueName);
      }
    }
  }

  /**
   * An unroutable message is meant to be dropped rather than returned. Publishing is not an assertion
   * that anybody is listening, and a channel must survive having published into nothing.
   */
  @Test
  public void testUnroutablePublishIsDroppedAndLeavesTheChannelUsable ()
    throws Exception {

    RabbitMQConnectionManager connectionManager = new RabbitMQConnectionManager(connector, "unroutable-test");
    BareRouter router = new BareRouter(connectionManager);

    connectionManager.start();
    try {
      router.send("nothing-is-bound-to-this", router.getRequestExchangeName(), new AMQP.BasicProperties.Builder().build(), "dropped".getBytes());

      Assert.assertTrue(connectionManager.isHealthy(), "publishing an unroutable message damaged the transport: " + connectionManager.getDiagnostic());

      router.send("nothing-is-bound-to-this", router.getRequestExchangeName(), new AMQP.BasicProperties.Builder().build(), "also-dropped".getBytes());

      Assert.assertTrue(connectionManager.isHealthy(), "the channel did not survive a second unroutable publish: " + connectionManager.getDiagnostic());
    } finally {
      connectionManager.close();
    }
  }

  /**
   * An exclusive queue belongs to the connection that declared it, so a foreign connection holding the
   * same name is answered with RESOURCE_LOCKED. That is transient by nature - the holder goes away -
   * so the router must keep retrying and recover on its own once the name is free.
   */
  @Test
  public void testRouterRecoversWhenAnExclusiveNameIsReleased ()
    throws Exception {

    String queueName = "wire-test-exclusive-" + System.currentTimeMillis();
    Connection holdingConnection = connectionFactory.newConnection(new Address[] {new Address("localhost", 5672)});
    Channel holdingChannel = holdingConnection.createChannel();

    holdingChannel.queueDeclare(queueName, true, true, false, null);

    RabbitMQConnectionManager connectionManager = new RabbitMQConnectionManager(connector, "locked-test");
    ExclusiveRouter router = new ExclusiveRouter(connectionManager, queueName);

    connectionManager.start();
    try {
      Assert.assertFalse(router.isFullyBound(), "the router claimed a queue another connection holds exclusively");

      holdingConnection.abort();

      Assert.assertTrue(awaitBound(router), "the router never recovered after the exclusive name was released");
    } finally {
      connectionManager.close();
      try {
        holdingConnection.abort();
      } catch (Exception exception) {
        //  already gone
      }
    }
  }

  private RabbitMQResponseTransport createResponseTransport (RabbitMQConnector rabbitMQConnector)
    throws Exception {

    return createResponseTransport(rabbitMQConnector, SERVICE_GROUP);
  }

  private RabbitMQResponseTransport createResponseTransport (RabbitMQConnector rabbitMQConnector, String serviceGroup)
    throws Exception {

    //  A single quorum replica, because the test harness runs one broker node. Production wants three,
    //  since a quorum queue needs a majority of its members.
    return new RabbitMQResponseTransport(rabbitMQConnector, new NameConfiguration(), InvocationWorker.class, new JsonSignalCodec(), serviceGroup, 1, 1, 60, 1, false, null);
  }

  /*
   * Drops the connection the way a broker restart does, without restarting the broker - the transport
   * cannot tell the difference, and the harness's container stays up for the rest of the class.
   */
  /*
   * Kills the connection for real, then reports it through the same entry point the connection's own
   * shutdown listener uses.
   *
   * The second step is not a shortcut around the first. A client-side abort is application initiated,
   * and the manager deliberately ignores those - it must not reconnect a connection the transport
   * itself closed, which is exactly what it does when replacing one. A broker going away is not
   * application initiated, and the harness's broker has no management port to close a connection
   * through, so the loss is announced the way the listener would announce it.
   */
  private void killConnection (RabbitMQResponseTransport responseTransport) {

    RabbitMQConnectionManager connectionManager = responseTransport.getConnectionManager();
    Connection connection = connectionManager.getConnection();
    int generation = connectionManager.getGeneration();

    if (connection != null) {
      connection.abort();
    }

    connectionManager.onConnectionFailure(generation, null);
  }

  private String generationOf (RabbitMQResponseTransport responseTransport) {

    String diagnostic = responseTransport.getDiagnostic();
    int start = diagnostic.indexOf("generation(");

    return (start < 0) ? "" : diagnostic.substring(start, diagnostic.indexOf(')', start) + 1);
  }

  private boolean awaitHealthy (RabbitMQResponseTransport responseTransport)
    throws InterruptedException {

    long stopTime = System.currentTimeMillis() + AWAIT_MILLISECONDS;

    while (System.currentTimeMillis() < stopTime) {
      if (responseTransport.isHealthy()) {

        return true;
      }
      Thread.sleep(250);
    }

    return false;
  }

  private boolean awaitDiagnostic (RabbitMQResponseTransport responseTransport, String fragment)
    throws InterruptedException {

    long stopTime = System.currentTimeMillis() + AWAIT_MILLISECONDS;

    while (System.currentTimeMillis() < stopTime) {
      if (responseTransport.getDiagnostic().contains(fragment)) {

        return true;
      }
      Thread.sleep(250);
    }

    return false;
  }

  private boolean awaitBound (MessageRouter messageRouter)
    throws InterruptedException {

    long stopTime = System.currentTimeMillis() + AWAIT_MILLISECONDS;

    while (System.currentTimeMillis() < stopTime) {
      if (messageRouter.isFullyBound()) {

        return true;
      }
      Thread.sleep(250);
    }

    return false;
  }

  /**
   * A router with no queues of its own, for exercising the publishing side alone.
   */
  private static class BareRouter extends MessageRouter {

    private BareRouter (RabbitMQConnectionManager connectionManager) {

      super(connectionManager, "wire-test", new NameConfiguration(), null);
    }

    @Override
    public void bindQueues () {

      markFullyBound(true);
    }

    @Override
    public void installConsumer () {

    }

    @Override
    public void uninstallConsumer () {

    }

    @Override
    public boolean isConsumerRequired () {

      return false;
    }

    @Override
    public boolean isConsumerInstalled () {

      return false;
    }
  }

  /**
   * A router wanting one exclusive queue by a name the test controls.
   */
  private static class ExclusiveRouter extends MessageRouter {

    private final String queueName;

    private ExclusiveRouter (RabbitMQConnectionManager connectionManager, String queueName) {

      super(connectionManager, "wire-test", new NameConfiguration(), null);

      this.queueName = queueName;
    }

    @Override
    public void bindQueues ()
      throws IOException {

      markFullyBound(declareAndBind(queueName, true, (channel) -> {

        channel.queueDeclare(queueName, true, true, false, Map.of());
        channel.queueBind(queueName, getRequestExchangeName(), queueName);
      }));
    }

    @Override
    public void installConsumer () {

    }

    @Override
    public void uninstallConsumer () {

    }

    @Override
    public boolean isConsumerRequired () {

      return false;
    }

    @Override
    public boolean isConsumerInstalled () {

      return false;
    }
  }
}
