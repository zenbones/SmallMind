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

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.OumuamuaException;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the contract between the persistent flag on a {@link Channel} and the
 * server-side removal path. {@code IdleChannelOperationTest} already covers idle
 * pruning, and {@code OumuamuaChannelTest} covers flag accessors; what is missing is
 * the {@link OumuamuaServer#removeChannel} entry point that throws
 * {@link ChannelStateException} for persistent channels. This unit test verifies that
 * boundary directly against a real {@link OumuamuaServer} (constructed with a mocked
 * protocol, no maintenance threads started).
 */
@Test(groups = "unit")
public class PersistentChannelLifecycleTest {

  private OumuamuaServer<OrthodoxValue> server;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod ()
    throws OumuamuaException {

    Protocol<OrthodoxValue> protocol = Mockito.mock(Protocol.class);

    Mockito.when(protocol.getName()).thenReturn("websocket");

    OumuamuaConfiguration<OrthodoxValue> configuration = new OumuamuaConfiguration<>();

    configuration.setCodec(new OrthodoxCodec(new JaxbDeserializer<>()));
    configuration.setProtocols(new Protocol[] {protocol});

    server = new OumuamuaServer<>(configuration);
  }

  public void testRemoveChannelDeletesNonPersistentChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = server.requireChannel("/transient");

    server.removeChannel(channel);

    Assert.assertNull(server.findChannel("/transient"), "Non-persistent channel should be gone after removeChannel");
  }

  @Test(expectedExceptions = ChannelStateException.class)
  public void testRemoveChannelThrowsForPersistentChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = server.requireChannel("/locked");

    channel.setPersistent(true);
    server.removeChannel(channel);
  }

  public void testRemoveChannelLeavesPersistentChannelInPlaceAfterFailedAttempt ()
    throws Exception {

    Channel<OrthodoxValue> channel = server.requireChannel("/locked");

    channel.setPersistent(true);

    try {
      server.removeChannel(channel);
      Assert.fail("Expected ChannelStateException when removing a persistent channel");
    } catch (ChannelStateException expected) {
      // expected
    }

    Assert.assertSame(server.findChannel("/locked"), channel, "Persistent channel must still be in the tree after a failed remove");
  }

  public void testFlippingPersistentToFalseAllowsSubsequentRemoval ()
    throws Exception {

    Channel<OrthodoxValue> channel = server.requireChannel("/togglable");

    channel.setPersistent(true);

    try {
      server.removeChannel(channel);
      Assert.fail("Persistent channel should not be removable while the flag is set");
    } catch (ChannelStateException expected) {
      // expected
    }

    channel.setPersistent(false);
    server.removeChannel(channel);

    Assert.assertNull(server.findChannel("/togglable"), "Channel should be removable after clearing the persistent flag");
  }

  public void testInitializerSettingPersistentSurvivesRemoveAttempt ()
    throws Exception {

    ChannelInitializer<OrthodoxValue> persistentInitializer = c -> c.setPersistent(true);
    Channel<OrthodoxValue> channel = server.requireChannel("/init-persistent", persistentInitializer);

    Assert.assertTrue(channel.isPersistent(), "Initializer should have set the persistent flag");

    try {
      server.removeChannel(channel);
      Assert.fail("Expected ChannelStateException when removing a channel made persistent by an initializer");
    } catch (ChannelStateException expected) {
      // expected
    }

    Assert.assertSame(server.findChannel("/init-persistent"), channel);
  }

  public void testFindChannelOnEmptyServerReturnsNull ()
    throws Exception {

    Assert.assertNull(server.findChannel("/no/such/channel"));
  }

  public void testIsReflectingReturnsFalseForUnconfiguredRoute ()
    throws Exception {

    Assert.assertFalse(server.isReflecting(new DefaultRoute("/anything")));
  }
}
