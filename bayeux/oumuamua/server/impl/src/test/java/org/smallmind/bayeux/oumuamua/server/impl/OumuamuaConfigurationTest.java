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

import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OumuamuaConfigurationTest {

  private OumuamuaConfiguration<OrthodoxValue> configuration;

  @BeforeMethod
  public void beforeMethod () {

    configuration = new OumuamuaConfiguration<>();
  }

  private Route route (String path)
    throws Exception {

    return new DefaultRoute(path);
  }

  public void testIsReflectingReturnsFalseWhenPathsUnset ()
    throws Exception {

    Assert.assertFalse(configuration.isReflecting(route("/foo/bar")));
  }

  public void testIsStreamingReturnsFalseWhenPathsUnset ()
    throws Exception {

    Assert.assertFalse(configuration.isStreaming(route("/foo/bar")));
  }

  public void testIsReflectingMatchesExactPath ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo/bar"});

    Assert.assertTrue(configuration.isReflecting(route("/foo/bar")));
  }

  public void testIsReflectingMatchesShortPrefix ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo"});

    Assert.assertTrue(configuration.isReflecting(route("/foo/bar")));
    Assert.assertTrue(configuration.isReflecting(route("/foo/bar/baz")));
  }

  public void testIsReflectingRejectsNonMatchingRoute ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo"});

    Assert.assertFalse(configuration.isReflecting(route("/bar")));
  }

  public void testIsReflectingHonorsSingleWildcardSegment ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo/*"});

    Assert.assertTrue(configuration.isReflecting(route("/foo/bar")));
    Assert.assertTrue(configuration.isReflecting(route("/foo/anything")));
  }

  public void testIsReflectingHonorsDeepWildcard ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo/**"});

    Assert.assertTrue(configuration.isReflecting(route("/foo/bar")));
    Assert.assertTrue(configuration.isReflecting(route("/foo/bar/baz/qux")));
  }

  public void testIsReflectingMatchesAnyOfMultiplePaths ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo", "/baz"});

    Assert.assertTrue(configuration.isReflecting(route("/foo/x")));
    Assert.assertTrue(configuration.isReflecting(route("/baz/x")));
    Assert.assertFalse(configuration.isReflecting(route("/bar/x")));
  }

  public void testIsStreamingUsesIndependentPathSet ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo"});
    configuration.setStreamingPaths(new String[] {"/bar"});

    Assert.assertFalse(configuration.isStreaming(route("/foo/x")));
    Assert.assertTrue(configuration.isStreaming(route("/bar/x")));
  }

  public void testIsReflectingHandlesNullRoute ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo"});

    Assert.assertFalse(configuration.isReflecting(null));
  }

  public void testSetReflectingPathsSkipsNullAndEmptyEntries ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {null, "", "/foo"});

    Assert.assertEquals(configuration.getParsedReflectingPaths().length, 1);
    Assert.assertTrue(configuration.isReflecting(route("/foo/bar")));
  }

  public void testSetReflectingPathsTreatsPathsWithoutLeadingSlashEquivalently ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"foo"});

    Assert.assertTrue(configuration.isReflecting(route("/foo/bar")));
  }

  public void testSetReflectingPathsWithEmptyArrayClearsMatches ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {"/foo"});
    configuration.setReflectingPaths(new String[0]);

    Assert.assertFalse(configuration.isReflecting(route("/foo/bar")));
  }

  public void testDefaultsExposedThroughAccessors () {

    Assert.assertEquals(configuration.getChannelTimeToLiveMinutes(), 30L);
    Assert.assertEquals(configuration.getSessionConnectIntervalSeconds(), 30);
    Assert.assertEquals(configuration.getSessionMaxIdleTimeoutSeconds(), 120);
    Assert.assertEquals(configuration.getIdleChannelCycleMinutes(), 5);
    Assert.assertEquals(configuration.getIdleSessionCycleMinutes(), 1);
    Assert.assertEquals(configuration.getMaxLongPollQueueSize(), 1000);
    Assert.assertFalse(configuration.isAllowsImplicitConnection());
  }

  public void testDefaultLogLevels () {

    Assert.assertEquals(configuration.getMessageLogLevel(), org.smallmind.scribe.pen.Level.TRACE);
    Assert.assertEquals(configuration.getIdleCleanupLogLevel(), org.smallmind.scribe.pen.Level.DEBUG);
    Assert.assertEquals(configuration.getOverflowLogLevel(), org.smallmind.scribe.pen.Level.DEBUG);
  }

  public void testSetAndGetCodec () {

    org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec codec =
      new org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec(
        new org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer<>());

    configuration.setCodec(codec);

    Assert.assertSame(configuration.getCodec(), codec);
  }

  public void testSetAndGetProtocols () {

    org.smallmind.bayeux.oumuamua.server.api.Protocol<OrthodoxValue> protocol =
      org.mockito.Mockito.mock(org.smallmind.bayeux.oumuamua.server.api.Protocol.class);

    configuration.setProtocols(new org.smallmind.bayeux.oumuamua.server.api.Protocol[] {protocol});

    Assert.assertEquals(configuration.getProtocols().length, 1);
    Assert.assertSame(configuration.getProtocols()[0], protocol);
  }

  public void testSetAndGetServices () {

    org.smallmind.bayeux.oumuamua.server.api.BayeuxService<OrthodoxValue> service =
      org.mockito.Mockito.mock(org.smallmind.bayeux.oumuamua.server.api.BayeuxService.class);

    configuration.setServices(new org.smallmind.bayeux.oumuamua.server.api.BayeuxService[] {service});

    Assert.assertEquals(configuration.getServices().length, 1);
    Assert.assertSame(configuration.getServices()[0], service);
  }

  public void testSetAndGetListeners () {

    org.smallmind.bayeux.oumuamua.server.api.Server.Listener<OrthodoxValue> listener =
      org.mockito.Mockito.mock(org.smallmind.bayeux.oumuamua.server.api.Server.Listener.class);

    configuration.setListeners(new org.smallmind.bayeux.oumuamua.server.api.Server.Listener[] {listener});

    Assert.assertEquals(configuration.getListeners().length, 1);
    Assert.assertSame(configuration.getListeners()[0], listener);
  }

  public void testSetAndGetBackbone () {

    org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone<OrthodoxValue> backbone =
      org.mockito.Mockito.mock(org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone.class);

    configuration.setBackbone(backbone);

    Assert.assertSame(configuration.getBackbone(), backbone);
  }

  public void testSetAndGetSecurityPolicy () {

    org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy<OrthodoxValue> policy =
      org.mockito.Mockito.mock(org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy.class);

    configuration.setSecurityPolicy(policy);

    Assert.assertSame(configuration.getSecurityPolicy(), policy);
  }

  public void testSetAndGetExecutorService () {

    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    try {
      configuration.setExecutorService(executor);
      Assert.assertSame(configuration.getExecutorService(), executor);
    } finally {
      executor.shutdown();
    }
  }

  public void testSetAndGetLogLevels () {

    configuration.setMessageLogLevel(org.smallmind.scribe.pen.Level.INFO);
    configuration.setIdleCleanupLogLevel(org.smallmind.scribe.pen.Level.WARN);
    configuration.setOverflowLogLevel(org.smallmind.scribe.pen.Level.ERROR);

    Assert.assertEquals(configuration.getMessageLogLevel(), org.smallmind.scribe.pen.Level.INFO);
    Assert.assertEquals(configuration.getIdleCleanupLogLevel(), org.smallmind.scribe.pen.Level.WARN);
    Assert.assertEquals(configuration.getOverflowLogLevel(), org.smallmind.scribe.pen.Level.ERROR);
  }

  public void testDirectSettersForNumericTunables () {

    configuration.setChannelTimeToLiveMinutes(60L);
    configuration.setSessionConnectIntervalSeconds(15);
    configuration.setSessionMaxIdleTimeoutSeconds(60);
    configuration.setIdleChannelCycleMinutes(10);
    configuration.setIdleSessionCycleMinutes(2);
    configuration.setMaxLongPollQueueSize(500);

    Assert.assertEquals(configuration.getChannelTimeToLiveMinutes(), 60L);
    Assert.assertEquals(configuration.getSessionConnectIntervalSeconds(), 15);
    Assert.assertEquals(configuration.getSessionMaxIdleTimeoutSeconds(), 60);
    Assert.assertEquals(configuration.getIdleChannelCycleMinutes(), 10);
    Assert.assertEquals(configuration.getIdleSessionCycleMinutes(), 2);
    Assert.assertEquals(configuration.getMaxLongPollQueueSize(), 500);
  }

  public void testParsedReflectingPathsAccessor () {

    Assert.assertNull(configuration.getParsedReflectingPaths());

    configuration.setReflectingPaths(new String[] {"/foo"});

    Assert.assertNotNull(configuration.getParsedReflectingPaths());
  }

  public void testParsedStreamingPathsAccessor () {

    Assert.assertNull(configuration.getParsedStreamingPaths());

    configuration.setStreamingPaths(new String[] {"/bar"});

    Assert.assertNotNull(configuration.getParsedStreamingPaths());
  }

  public void testSetReflectingPathsToleratesNull () {

    configuration.setReflectingPaths(null);

    Assert.assertNotNull(configuration.getParsedReflectingPaths(), "Null input must produce an empty parsed-path array, not null");
    Assert.assertEquals(configuration.getParsedReflectingPaths().length, 0);
  }

  public void testSetStreamingPathsToleratesNull () {

    configuration.setStreamingPaths(null);

    Assert.assertNotNull(configuration.getParsedStreamingPaths());
    Assert.assertEquals(configuration.getParsedStreamingPaths().length, 0);
  }

  public void testDecomposeIgnoresNullAndEmptyEntries ()
    throws Exception {

    configuration.setReflectingPaths(new String[] {null, "", "/legitimate"});

    Assert.assertTrue(configuration.isReflecting(route("/legitimate")));
    Assert.assertEquals(configuration.getParsedReflectingPaths().length, 1, "Null and empty entries must be filtered out of the parsed-path array");
  }
}
