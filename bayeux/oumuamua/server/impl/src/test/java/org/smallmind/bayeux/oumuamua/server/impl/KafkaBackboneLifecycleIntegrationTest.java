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

import org.smallmind.bayeux.oumuamua.server.spi.backbone.kafka.KafkaBackbone;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.kafka.utility.KafkaConsumerType;
import org.smallmind.kafka.utility.KafkaServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the idempotent branches of {@link KafkaBackbone#startUp} and
 * {@link KafkaBackbone#shutDown}: calling each method when the backbone is
 * already in the target state must be a silent no-op, not an exception.
 *
 * <p>A separate backbone instance is created for this test so it does not
 * interfere with the Spring-managed backbone used by the rest of the integration
 * suite. The server bean from the shared Spring context is reused as the
 * delivery target, which avoids duplicating its full configuration.
 */
@Test(groups = "integration")
public class KafkaBackboneLifecycleIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String LIFECYCLE_TOPIC = "lifecycle-test";
  private static final String LIFECYCLE_NODE = "lifecycle-test-node";
  private static final int GRACE_PERIOD_SECONDS = 10;

  private KafkaBackbone<OrthodoxValue> backbone;
  private OumuamuaServer<OrthodoxValue> server;

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    backbone = new KafkaBackbone<>(
      LIFECYCLE_NODE,
      1,
      GRACE_PERIOD_SECONDS,
      KafkaConsumerType.CLASSIC,
      LIFECYCLE_TOPIC,
      new KafkaServer("localhost", 9094));

    server = applicationContext().getBean("oumuamuaServer", OumuamuaServer.class);
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    if (backbone != null) {
      try {
        backbone.shutDown();
      } catch (Exception ignored) {
      }
    }

    super.afterClass();
  }

  @Test
  public void startUpIsIdempotentWhenAlreadyStarted ()
    throws Exception {

    backbone.startUp(server);
    backbone.startUp(server);
  }

  @Test(dependsOnMethods = "startUpIsIdempotentWhenAlreadyStarted")
  public void shutDownIsIdempotentWhenAlreadyStopped ()
    throws Exception {

    backbone.shutDown();
    backbone.shutDown();
  }
}
