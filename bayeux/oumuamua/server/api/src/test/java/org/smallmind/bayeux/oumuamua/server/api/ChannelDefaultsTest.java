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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.TestValueFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pins {@link Channel}'s default predicates ({@code isWild}, {@code isDeepWild},
 * {@code isMeta}, {@code isService}, {@code isDeliverable}) to delegate to the
 * channel's {@link Route}.
 */
@Test(groups = "unit")
public class ChannelDefaultsTest {

  public void testIsWildDelegatesToRoute () {

    Assert.assertTrue(channelWith(true, false, false, false).isWild());
    Assert.assertFalse(channelWith(false, false, false, false).isWild());
  }

  public void testIsDeepWildDelegatesToRoute () {

    Assert.assertTrue(channelWith(false, true, false, false).isDeepWild());
    Assert.assertFalse(channelWith(false, false, false, false).isDeepWild());
  }

  public void testIsMetaDelegatesToRoute () {

    Assert.assertTrue(channelWith(false, false, true, false).isMeta());
    Assert.assertFalse(channelWith(false, false, false, false).isMeta());
  }

  public void testIsServiceDelegatesToRoute () {

    Assert.assertTrue(channelWith(false, false, false, true).isService());
    Assert.assertFalse(channelWith(false, false, false, false).isService());
  }

  public void testIsDeliverableDelegatesToRoute () {

    Assert.assertTrue(channelWith(false, false, false, false).isDeliverable());
    Assert.assertFalse(channelWith(true, false, false, false).isDeliverable());
    Assert.assertFalse(channelWith(false, true, false, false).isDeliverable());
    Assert.assertFalse(channelWith(false, false, true, false).isDeliverable());
    Assert.assertFalse(channelWith(false, false, false, true).isDeliverable());
  }

  private static StubChannel channelWith (boolean wild, boolean deepWild, boolean meta, boolean service) {

    return new StubChannel(new Route() {

      @Override
      public String getPath () {

        return "/stub";
      }

      @Override
      public int size () {

        return 1;
      }

      @Override
      public int lastIndex () {

        return 0;
      }

      @Override
      public Segment getSegment (int index) {

        return null;
      }

      @Override
      public boolean isWild () {

        return wild;
      }

      @Override
      public boolean isDeepWild () {

        return deepWild;
      }

      @Override
      public boolean isMeta () {

        return meta;
      }

      @Override
      public boolean isService () {

        return service;
      }

      @Override
      public boolean matchesPrefix (String... segments) {

        return false;
      }
    });
  }

  private static class StubChannel implements Channel<TestValueFactory.TestValue> {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Route route;

    private StubChannel (Route route) {

      this.route = route;
    }

    @Override
    public Route getRoute () {

      return route;
    }

    @Override
    public void addListener (Listener<TestValueFactory.TestValue> listener) {

    }

    @Override
    public void removeListener (Listener<TestValueFactory.TestValue> listener) {

    }

    @Override
    public boolean isPersistent () {

      return false;
    }

    @Override
    public void setPersistent (boolean persistent) {

    }

    @Override
    public boolean isReflecting () {

      return false;
    }

    @Override
    public void setReflecting (boolean reflecting) {

    }

    @Override
    public boolean isStreaming () {

      return false;
    }

    @Override
    public void setStreaming (boolean streaming) {

    }

    @Override
    public boolean subscribe (Session<TestValueFactory.TestValue> session) {

      return false;
    }

    @Override
    public void unsubscribe (Session<TestValueFactory.TestValue> session) {

    }

    @Override
    public boolean isRemovable (long now) {

      return false;
    }

    @Override
    public void deliver (Session<TestValueFactory.TestValue> sender, Packet<TestValueFactory.TestValue> packet, Set<String> sessionIdSet) {

    }

    @Override
    public void publish (ObjectValue<TestValueFactory.TestValue> data) {

    }

    @Override
    public Object getAttribute (String name) {

      return attributes.get(name);
    }

    @Override
    public void setAttribute (String name, Object value) {

      attributes.put(name, value);
    }

    @Override
    public Object removeAttribute (String name) {

      return attributes.remove(name);
    }

    @Override
    public Set<String> getAttributeNames () {

      return attributes.keySet();
    }
  }
}
