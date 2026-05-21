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
package org.smallmind.bayeux.oumuamua.server.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.json.TestValueFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies {@link Protocol#init(Server, ServletConfig)}'s default implementation initializes
 * every transport registered under the protocol, in registration order, and propagates the
 * first {@link ServletException} thrown by any transport.
 */
@Test(groups = "unit")
public class ProtocolDefaultsTest {

  public void testDefaultInitInvokesEveryTransportInOrder ()
    throws ServletException {

    RecordingTransport first = new RecordingTransport("a", false);
    RecordingTransport second = new RecordingTransport("b", false);
    RecordingProtocol protocol = new RecordingProtocol(new String[] {"a", "b"}, Map.of("a", first, "b", second));

    protocol.init(null, null);

    Assert.assertEquals(first.initCount, 1);
    Assert.assertEquals(second.initCount, 1);
    Assert.assertEquals(protocol.lookups, List.of("a", "b"));
  }

  public void testDefaultInitPropagatesTransportException () {

    RecordingTransport failing = new RecordingTransport("x", true);
    RecordingProtocol protocol = new RecordingProtocol(new String[] {"x"}, Map.of("x", failing));

    try {
      protocol.init(null, null);
      Assert.fail("Expected ServletException to propagate");
    } catch (ServletException expected) {
      Assert.assertEquals(expected.getMessage(), "boom");
    }
  }

  public void testDefaultInitWithEmptyTransportListIsNoOp ()
    throws ServletException {

    RecordingProtocol protocol = new RecordingProtocol(new String[0], Map.of());

    protocol.init(null, null);

    Assert.assertTrue(protocol.lookups.isEmpty());
  }

  private static class RecordingProtocol implements Protocol<TestValueFactory.TestValue> {

    private final List<String> lookups = new ArrayList<>();
    private final String[] transportNames;
    private final Map<String, Transport<TestValueFactory.TestValue>> transports;

    private RecordingProtocol (String[] transportNames, Map<String, Transport<TestValueFactory.TestValue>> transports) {

      this.transportNames = transportNames;
      this.transports = transports;
    }

    @Override
    public String getName () {

      return "test";
    }

    @Override
    public boolean isLongPolling () {

      return false;
    }

    @Override
    public long getLongPollTimeoutMilliseconds () {

      return 0;
    }

    @Override
    public String[] getTransportNames () {

      return transportNames;
    }

    @Override
    public Transport<TestValueFactory.TestValue> getTransport (String name) {

      lookups.add(name);

      return transports.get(name);
    }

    @Override
    public void addListener (Listener<TestValueFactory.TestValue> listener) {

    }

    @Override
    public void removeListener (Listener<TestValueFactory.TestValue> listener) {

    }
  }

  private static class RecordingTransport implements Transport<TestValueFactory.TestValue> {

    private final Map<String, Object> attrs = new HashMap<>();
    private final String name;
    private final boolean failsOnInit;

    private int initCount;

    private RecordingTransport (String name, boolean failsOnInit) {

      this.name = name;
      this.failsOnInit = failsOnInit;
    }

    @Override
    public Protocol<TestValueFactory.TestValue> getProtocol () {

      return null;
    }

    @Override
    public String getName () {

      return name;
    }

    @Override
    public boolean isLocal () {

      return false;
    }

    @Override
    public void init (Server<?> server, ServletConfig servletConfig)
      throws ServletException {

      initCount++;

      if (failsOnInit) {
        throw new ServletException("boom");
      }
    }

    @Override
    public Object getAttribute (String name) {

      return attrs.get(name);
    }

    @Override
    public void setAttribute (String name, Object value) {

      attrs.put(name, value);
    }

    @Override
    public Object removeAttribute (String name) {

      return attrs.remove(name);
    }

    @Override
    public java.util.Set<String> getAttributeNames () {

      return attrs.keySet();
    }
  }
}
