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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.HashMap;
import java.util.Map;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.websocket.jakarta.WebSocketTransport;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for Oumuamua Bayeux server integration tests. Starts a Kafka
 * container, then loads the Grizzly HTTP server and the Oumuamua Bayeux
 * server through Spring; shuts the Spring context down before stopping
 * Kafka so the {@code KafkaBackbone} can drain against a live broker.
 *
 * <p>Subclasses obtain {@link BayeuxClient} instances through
 * {@link #constructBayeuxClient()} and drive them with {@link #handshakeAndAwait}
 * and {@link #disconnectAndAwait}. The container lifecycle, the Spring
 * context, and the server endpoint URL are owned here; per-test client
 * setup and teardown stay with the subclass.</p>
 */
public abstract class AbstractBayeuxIntegrationTest extends AbstractGroundwaterTest {

  private static final String[] DEFAULT_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/logging.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua.xml"
  };
  private static final String SERVER_URL = "http://localhost:9017/smallmind/cometd";
  private static final long DEFAULT_HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DEFAULT_DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;

  private ClassPathXmlApplicationContext applicationContext;

  public AbstractBayeuxIntegrationTest () {

    super(DockerApplication.KAFKA);
  }

  /**
   * Returns the Spring resource locations used to load the primary Bayeux stack. The default
   * locations wire {@code logging.xml}, {@code oumuamua-grizzly.xml}, and {@code oumuamua.xml};
   * subclasses override this hook to swap in alternative configuration (shorter session
   * timeouts, a custom {@code SecurityPolicy}, alternative protocol wiring, and so on).
   *
   * @return Spring classpath resource locations consumed by {@link #beforeClass()}
   */
  protected String[] springResourceLocations () {

    return DEFAULT_SPRING_RESOURCE_LOCATIONS;
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    applicationContext = new ClassPathXmlApplicationContext(springResourceLocations());
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (applicationContext != null) {
      applicationContext.close();
    }

    super.afterClass();
  }

  protected ApplicationContext applicationContext () {

    return applicationContext;
  }

  protected String serverUrl () {

    return SERVER_URL;
  }

  /**
   * Builds an unconnected {@link BayeuxClient} configured with a Jakarta
   * WebSocket transport pointing at {@link #serverUrl()}. The caller owns
   * the returned client and is responsible for handshake, extensions, and
   * disconnect.
   */
  protected BayeuxClient constructBayeuxClient () {

    WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
    ClientTransport transport = new WebSocketTransport(null, null, webSocketContainer);

    return new BayeuxClient(SERVER_URL, transport);
  }

  protected void handshakeAndAwait (BayeuxClient client) {

    handshakeAndAwait(client, DEFAULT_HANDSHAKE_TIMEOUT_MILLISECONDS);
  }

  /**
   * Drives a handshake on {@code client} with an empty template and blocks
   * until the client reaches {@link BayeuxClient.State#CONNECTED}.
   *
   * @throws IllegalStateException when the client does not reach
   *                               {@code CONNECTED} within {@code timeoutMilliseconds}.
   */
  protected void handshakeAndAwait (BayeuxClient client, long timeoutMilliseconds) {

    Map<String, Object> handshakeTemplate = new HashMap<>();

    client.handshake(handshakeTemplate, System.out::println);
    if (!client.waitFor(timeoutMilliseconds, BayeuxClient.State.CONNECTED)) {
      throw new IllegalStateException("BayeuxClient did not reach CONNECTED within " + timeoutMilliseconds + "ms");
    }
  }

  protected void disconnectAndAwait (BayeuxClient client) {

    disconnectAndAwait(client, DEFAULT_DISCONNECT_TIMEOUT_MILLISECONDS);
  }

  /**
   * Disconnects {@code client} and blocks until it reaches
   * {@link BayeuxClient.State#DISCONNECTED}. A {@code null} client is
   * treated as a no-op. {@link BayeuxClient#disconnect()} is idempotent,
   * and {@link BayeuxClient#waitFor} returns immediately when the client
   * is already in the requested state, so this is safe to call against an
   * unconnected or already-disconnected client.
   */
  protected void disconnectAndAwait (BayeuxClient client, long timeoutMilliseconds) {

    if (client != null) {
      client.disconnect();
      client.waitFor(timeoutMilliseconds, BayeuxClient.State.DISCONNECTED);
    }
  }
}
