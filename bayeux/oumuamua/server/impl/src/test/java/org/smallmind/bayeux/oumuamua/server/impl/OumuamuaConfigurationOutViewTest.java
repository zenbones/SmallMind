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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the generated {@link OumuamuaConfigurationOutView}: verifies that
 * {@link OumuamuaConfigurationOutView#instance} correctly projects all configuration fields,
 * that {@link OumuamuaConfigurationOutView#factory} round-trips them back, and that all
 * individual getter/setter pairs on the view behave as simple accessors.
 */
@Test(groups = "unit")
public class OumuamuaConfigurationOutViewTest {

  private OumuamuaConfiguration<OrthodoxValue> configuration;
  private OrthodoxCodec codec;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());

    Protocol<OrthodoxValue> protocol = Mockito.mock(Protocol.class);

    Mockito.when(protocol.getName()).thenReturn("websocket");

    configuration = new OumuamuaConfiguration<>();
    configuration.setCodec(codec);
    configuration.setProtocols(new Protocol[] {protocol});
  }

  public void testDefaultConstructorProducesBlankView () {

    OumuamuaConfigurationOutView view = new OumuamuaConfigurationOutView();

    Assert.assertNull(view.getCodec());
    Assert.assertNull(view.getBackbone());
    Assert.assertNull(view.getProtocols());
    Assert.assertNull(view.getServices());
    Assert.assertNull(view.getListeners());
    Assert.assertNull(view.getExecutorService());
    Assert.assertNull(view.getSecurityPolicy());
    Assert.assertNull(view.getParsedReflectingPaths());
    Assert.assertNull(view.getParsedStreamingPaths());
  }

  public void testInstanceCopiesCodecFromConfiguration () {

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertSame(view.getCodec(), codec);
  }

  public void testInstanceCopiesDefaultNumericTunables () {

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertEquals(view.getChannelTimeToLiveMinutes(), 30L);
    Assert.assertEquals(view.getSessionConnectIntervalSeconds(), 30);
    Assert.assertEquals(view.getSessionMaxIdleTimeoutSeconds(), 120);
    Assert.assertEquals(view.getIdleChannelCycleMinutes(), 5);
    Assert.assertEquals(view.getIdleSessionCycleMinutes(), 1);
    Assert.assertEquals(view.getMaxLongPollQueueSize(), 1000);
  }

  public void testInstanceCopiesDefaultLogLevels () {

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertEquals(view.getMessageLogLevel(), Level.TRACE);
    Assert.assertEquals(view.getIdleCleanupLogLevel(), Level.DEBUG);
    Assert.assertEquals(view.getOverflowLogLevel(), Level.DEBUG);
  }

  public void testInstanceCopiesAllowsImplicitConnection () {

    configuration.setAllowsImplicitConnection(true);

    Assert.assertTrue(OumuamuaConfigurationOutView.instance(configuration).isAllowsImplicitConnection());
  }

  public void testInstanceCopiesExecutorService () {

    ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      configuration.setExecutorService(executor);
      Assert.assertSame(OumuamuaConfigurationOutView.instance(configuration).getExecutorService(), executor);
    } finally {
      executor.shutdown();
    }
  }

  @SuppressWarnings("unchecked")
  public void testInstanceCopiesListeners () {

    Server.Listener<OrthodoxValue> listener = Mockito.mock(Server.Listener.class);

    configuration.setListeners(new Server.Listener[] {listener});

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertEquals(view.getListeners().length, 1);
    Assert.assertSame(view.getListeners()[0], listener);
  }

  public void testFactoryReconstructsConfiguration () {

    configuration.setChannelTimeToLiveMinutes(60L);
    configuration.setMaxLongPollQueueSize(500);
    configuration.setAllowsImplicitConnection(true);

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);
    OumuamuaConfiguration<OrthodoxValue> rebuilt = view.factory();

    Assert.assertEquals(rebuilt.getChannelTimeToLiveMinutes(), 60L);
    Assert.assertEquals(rebuilt.getMaxLongPollQueueSize(), 500);
    Assert.assertTrue(rebuilt.isAllowsImplicitConnection());
    Assert.assertSame(rebuilt.getCodec(), codec);
  }

  public void testFactoryWithExistingConfigurationPopulatesIt () {

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);
    OumuamuaConfiguration<OrthodoxValue> target = new OumuamuaConfiguration<>();

    view.factory(target);

    Assert.assertSame(target.getCodec(), codec);
  }

  public void testSetterGetterRoundTripsForAllNumericTunables () {

    OumuamuaConfigurationOutView view = new OumuamuaConfigurationOutView();

    view.setChannelTimeToLiveMinutes(45L);
    view.setSessionConnectIntervalSeconds(15);
    view.setSessionMaxIdleTimeoutSeconds(90);
    view.setIdleChannelCycleMinutes(10);
    view.setIdleSessionCycleMinutes(2);
    view.setMaxLongPollQueueSize(2000);

    Assert.assertEquals(view.getChannelTimeToLiveMinutes(), 45L);
    Assert.assertEquals(view.getSessionConnectIntervalSeconds(), 15);
    Assert.assertEquals(view.getSessionMaxIdleTimeoutSeconds(), 90);
    Assert.assertEquals(view.getIdleChannelCycleMinutes(), 10);
    Assert.assertEquals(view.getIdleSessionCycleMinutes(), 2);
    Assert.assertEquals(view.getMaxLongPollQueueSize(), 2000);
  }

  public void testSetterGetterRoundTripsForLogLevels () {

    OumuamuaConfigurationOutView view = new OumuamuaConfigurationOutView();

    view.setMessageLogLevel(Level.INFO);
    view.setIdleCleanupLogLevel(Level.WARN);
    view.setOverflowLogLevel(Level.ERROR);

    Assert.assertEquals(view.getMessageLogLevel(), Level.INFO);
    Assert.assertEquals(view.getIdleCleanupLogLevel(), Level.WARN);
    Assert.assertEquals(view.getOverflowLogLevel(), Level.ERROR);
  }

  public void testSetterGetterRoundTripsForPaths () {

    OumuamuaConfigurationOutView view = new OumuamuaConfigurationOutView();
    String[][] reflectingPaths = {{"foo", "bar"}};
    String[][] streamingPaths = {{"baz"}};

    view.setParsedReflectingPaths(reflectingPaths);
    view.setParsedStreamingPaths(streamingPaths);

    Assert.assertSame(view.getParsedReflectingPaths(), reflectingPaths);
    Assert.assertSame(view.getParsedStreamingPaths(), streamingPaths);
  }

  public void testAllowsImplicitConnectionSetterGetter () {

    OumuamuaConfigurationOutView view = new OumuamuaConfigurationOutView();

    view.setAllowsImplicitConnection(true);
    Assert.assertTrue(view.isAllowsImplicitConnection());

    view.setAllowsImplicitConnection(false);
    Assert.assertFalse(view.isAllowsImplicitConnection());
  }

  public void testEqualsAndHashCodeForTwoViewsOfSameConfiguration () {

    OumuamuaConfigurationOutView a = OumuamuaConfigurationOutView.instance(configuration);
    OumuamuaConfigurationOutView b = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertEquals(a, b);
    Assert.assertEquals(a.hashCode(), b.hashCode());
  }

  public void testEqualsReturnsTrueForSelf () {

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertEquals(view, view);
  }

  public void testEqualsReturnsFalseForNonView () {

    OumuamuaConfigurationOutView view = OumuamuaConfigurationOutView.instance(configuration);

    Assert.assertNotEquals(view, "not-a-view");
  }
}
